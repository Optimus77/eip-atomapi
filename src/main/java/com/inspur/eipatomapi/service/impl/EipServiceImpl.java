package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.ReturnMsg;
import com.inspur.eipatomapi.entity.bss.*;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.service.BssApiService;
import com.inspur.eipatomapi.service.EipDaoService;
import com.inspur.eipatomapi.service.IEipService;
import com.inspur.eipatomapi.service.NeutronService;
import com.inspur.eipatomapi.util.*;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HTTP;
import org.openstack4j.model.common.ActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.Server;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class EipServiceImpl implements IEipService {

    @Autowired
    private EipRepository eipRepository;

    @Autowired
    private NeutronService neutronService;

    @Autowired
    private EipDaoService eipDaoService;

    @Autowired
    private BssApiService bssApiService;

    private String pushMq;

    public final static Logger log = LoggerFactory.getLogger(EipServiceImpl.class);

    /**
     * create a eip
     * @param eipConfig          config
     * @return                   json info of eip
     */
    @ICPServiceLog
    public ResponseEntity atomCreateEip(EipAllocateParam eipConfig) {

        String code;
        String msg;
        try {
            Eip eipMo = eipDaoService.allocateEip(eipConfig, null);
            if (null != eipMo) {
                EipReturnBase eipInfo = new EipReturnBase();
                BeanUtils.copyProperties(eipMo, eipInfo);
                log.info("Atom create a eip success:{}", eipMo);
                return new ResponseEntity<>(ReturnMsgUtil.success(eipInfo), HttpStatus.OK);
            } else {
                code = ReturnStatus.SC_OPENSTACK_FIPCREATE_ERROR;
                msg = "Failed to create floating ip in external network:" + eipConfig.getRegion();
                log.error(msg);
            }

        }catch (Exception e){
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getCause()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * delete eip
     * @param eipId eipid
     * @return return
     */
    @ICPServiceLog
    public ResponseEntity atomDeleteEip(String eipId) {
        String msg;
        String code;

        try {
            ActionResponse actionResponse =  eipDaoService.deleteEip(eipId);
            if (actionResponse.isSuccess()){
                log.info("Atom delete eip successfully, eipId:{}", eipId);
                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
            }else {
                msg = actionResponse.getFault();
                code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            }
        }catch (Exception e){
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getCause()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /**
     * create a eip
     * @param eipOrder          config
     * @return                   json info of eip
     */
    @Override
    @ICPServiceLog
    public ResponseEntity createEip(EipReciveOrder eipOrder) {

        String code;
        String msg;
        try {
            log.info("Recive order:{}", JSONObject.toJSONString(eipOrder));
            EipOrder retrunMsg =  eipOrder.getReturnConsoleMessage();
            if(eipOrder.getOrderStatus().equals(HsConstants.PAYSUCCESS) ||
                    retrunMsg.getBillType().equals(HsConstants.HOURLYSETTLEMENT)) {
                EipAllocateParam eipConfig = getEipConfigByOrder(eipOrder);
                ReturnMsg returnMsg = preCheckParam(eipConfig);
                if(!returnMsg.getCode().equals(ReturnStatus.SC_OK)){
                    bssApiService.resultReturnMq(getEipOrderResult(eipOrder, "",HsConstants.FAIL));
                    return new ResponseEntity<>(returnMsg, HttpStatus.BAD_REQUEST);
                }
                Eip eipMo = eipDaoService.allocateEip(eipConfig, null);
                if (null != eipMo) {
                    EipReturnBase eipInfo = new EipReturnBase();
                    BeanUtils.copyProperties(eipMo, eipInfo);

                    //Return message to the front desk
                    SendMQEIP sendMQEIP = new SendMQEIP();
                    sendMQEIP.setUserName(CommonUtil.getUserId());
                    sendMQEIP.setHandlerName("operateEipHandler");
                    sendMQEIP.setInstanceId(eipMo.getEipId());
                    sendMQEIP.setInstanceStatus("active");
                    sendMQEIP.setOperateType("create");
                    sendMQEIP.setMessageType("success");
                    sendMQEIP.setMessage(CodeInfo.getCodeMessage(CodeInfo.EIP_CREATION_SUCCEEDED));
                    String url=pushMq;
                    log.info(url);
                    String orderStr=JSONObject.toJSONString(sendMQEIP);
                    log.info("return mq body str {}",sendMQEIP);
                    Map<String,String> headers = new HashMap<>();
                    headers.put("Authorization", CommonUtil.getKeycloackToken());
                    headers.put(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
                    HttpResponse response = HttpUtil.post(url,headers,orderStr);

                    bssApiService.resultReturnMq(getEipOrderResult(eipOrder, eipMo.getEipId(),"success"));
                    return new ResponseEntity<>(ReturnMsgUtil.success(eipInfo), HttpStatus.OK);
                } else {
                    code = ReturnStatus.SC_OPENSTACK_FIPCREATE_ERROR;
                    msg = "Failed to create floating ip in external network:" + eipConfig.getRegion();
                    log.error(msg);
                }
            }else {
                bssApiService.resultReturnMq(getEipOrderResult(eipOrder, "",HsConstants.FAIL));
                code = ReturnStatus.SC_RESOURCE_ERROR;
                msg = "not payed.";
                log.info(msg);
            }
        }catch (Exception e){
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getCause()+"";
        }
        bssApiService.resultReturnMq(getEipOrderResult(eipOrder, "",HsConstants.FAIL));
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    private  EipAllocateParam getEipConfigByOrder(EipReciveOrder eipOrder){
        EipAllocateParam eipAllocateParam = new EipAllocateParam();
        eipAllocateParam.setDuration(eipOrder.getReturnConsoleMessage().getDuration());
        List<EipOrderProduct> eipOrderProducts = eipOrder.getReturnConsoleMessage().getProductList();

        eipAllocateParam.setBillType(eipOrder.getReturnConsoleMessage().getBillType());

        for(EipOrderProduct eipOrderProduct: eipOrderProducts){
            if(!eipOrderProduct.getProductLineCode().equals(HsConstants.EIP)){
                continue;
            }
            eipAllocateParam.setRegion(eipOrderProduct.getRegion());
            List<EipOrderProductItem> eipOrderProductItems = eipOrderProduct.getItemList();
            for(EipOrderProductItem eipOrderProductItem: eipOrderProductItems){
                if(eipOrderProductItem.getCode().equals(HsConstants.NET) &&
                        eipOrderProductItem.getUnit().equals(HsConstants.M)){
                    eipAllocateParam.setBandwidth(Integer.parseInt(eipOrderProductItem.getValue()));
                }else if(eipOrderProductItem.getCode().equals(HsConstants.PROVIDER) &&
                        eipOrderProductItem.getType().equals(HsConstants.IMPACTFACTOR)){
                    eipAllocateParam.setIptype(eipOrderProductItem.getValue());
                }
            }
        }
        log.info("Get eip param from order:{}", eipAllocateParam.toString());
        /*chargemode now use the default value */
        return eipAllocateParam;
    }
    private ReturnMsg preCheckParam(EipAllocateParam param){
        String errorMsg = "success";
        if(param.getBandwidth() > 2000){
            errorMsg = "value must be 1-2000.";
        }
        if(!param.getChargemode().equals(HsConstants.BANDWIDTH) &&
                !param.getChargemode().equals(HsConstants.SHAREDBANDWIDTH)){
            errorMsg = errorMsg + "Only Bandwidth,SharedBandwidth is allowed. ";
        }

        if(!param.getBillType().equals(HsConstants.MONTHLY) && !param.getBillType().equals(HsConstants.HOURLYSETTLEMENT)){
            errorMsg = errorMsg + "Only PrePaid,PostPaid is allowed. ";
        }
        if(param.getRegion().isEmpty()){
            errorMsg = errorMsg + "can not be blank.";
        }
        String tp = param.getIptype();
        if(!tp.equals("5_bgp") && !tp.equals("5_sbgp") && !tp.equals("5_telcom") &&
                !tp.equals("5_union") && !tp.equals("BGP")){
            errorMsg = errorMsg +"Only 5_bgp,5_sbgp, 5_telcom, 5_union is allowed. ";
        }
        if(errorMsg.equals("success")) {
            log.info(errorMsg);
           return ReturnMsgUtil.error(ReturnStatus.SC_OK, errorMsg);
        }else {
            log.error(errorMsg);
           return ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR,errorMsg);
        }
    }
    /**
     * 1.delete  floatingIp
     * 2.Determine if Snate and Qos is deleted
     * 3.delete eip
     *
     * @param eipIds  eip ids
     * @return       result: true/false
     */
    @Override
    @ICPServiceLog
    public ResponseEntity deleteEipList(List<String> eipIds) {
        String errorMsg;
        try {
            ActionResponse actionResponse;
            List<String > failedIds = new ArrayList<>();
            for (String eipId : eipIds) {
                log.info("delete eip " + eipId);
                //deleteEip(eipId, null);
                 actionResponse = eipDaoService.deleteEip(eipId);
                 if(!actionResponse.isSuccess()){
                     failedIds.add(eipId);
                     log.error("delete eip error, eipId:{}", eipId);
                 }
            }
            if(failedIds.isEmpty()){
                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
            }else {
                errorMsg = failedIds.toString();
                log.error(errorMsg);
            }
        }catch (Exception e){
            e.printStackTrace();
            errorMsg = e.getMessage();
        }
        return new ResponseEntity<>(
                ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, errorMsg),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     *  delete eip
     * @param eipId  eip id
     * @param eipOrder  eip order
     * @return return
     */
    @Override
    @ICPServiceLog
    public ResponseEntity deleteEip(String eipId, EipReciveOrder eipOrder) {
        String msg;
        String code;

        try {
            if(eipOrder.getOrderStatus().equals("createSuccess")) {
                ActionResponse actionResponse =  eipDaoService.deleteEip(eipId);
                if (actionResponse.isSuccess()){

                    //Return message to the front desk
                    SendMQEIP sendMQEIP = new SendMQEIP();
                    sendMQEIP.setUserName(CommonUtil.getUserId());
                    sendMQEIP.setHandlerName("operateEipHandler");
                    sendMQEIP.setInstanceId(eipId);
                    sendMQEIP.setInstanceStatus("active");
                    sendMQEIP.setOperateType("delete");
                    sendMQEIP.setMessageType("success");
                    sendMQEIP.setMessage(CodeInfo.getCodeMessage(CodeInfo.EIP_DELETE_SUCCEEDED));
                    String url=pushMq;
                    log.info(url);
                    String orderStr=JSONObject.toJSONString(sendMQEIP);
                    log.info("return mq body str {}",sendMQEIP);
                    Map<String,String> headers = new HashMap<>();
                    headers.put("Authorization", CommonUtil.getKeycloackToken());
                    headers.put(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
                    HttpResponse response = HttpUtil.post(url,headers,orderStr);

                    bssApiService.resultReturnMq(getEipOrderResult(eipOrder, eipId,"success"));
                    return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
                }else {
                    msg = actionResponse.getFault();
                    code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
                }
            }else{
                msg = "Failed to delete eip,failed to create delete order.";
                code = ReturnStatus.SC_PARAM_UNKONWERROR;
                log.error(msg);
            }
        }catch (Exception e){
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getCause()+"";
        }
        bssApiService.resultReturnMq(getEipOrderResult(eipOrder, eipId,HsConstants.FAIL));
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     *  list the eip
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity listEips(int currentPage,int limit,boolean returnFloatingip){

        try {
            String projcectid=CommonUtil.getUserId();
            log.info("listEips  of user, userId:{}", projcectid);

            if(projcectid==null){
                return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),"get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data=new JSONObject();
            JSONArray eips=new JSONArray();
            if(currentPage!=0){
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable =PageRequest.of(currentPage-1,limit,sort);
                Page<Eip> page=eipRepository.findByProjectId(projcectid,pageable);
                for(Eip eip:page.getContent()){
                    EipReturnDetail eipReturnDetail = new EipReturnDetail();
                    BeanUtils.copyProperties(eip, eipReturnDetail);
                    eipReturnDetail.setResourceset(Resourceset.builder()
                            .resourceid(eip.getInstanceId())
                            .resourcetype(eip.getInstanceType()).build());
                    eips.add(eipReturnDetail);
                }
                data.put("eips",eips);
                data.put("totalPages",page.getTotalPages());
                data.put("totalElements",page.getTotalElements());
                data.put("currentPage",currentPage);
                data.put("currentPagePer",limit);
            }else{
                List<Eip> eipList=eipRepository.findByProjectId(projcectid);
                for(Eip eip:eipList){
                    EipReturnDetail eipReturnDetail = new EipReturnDetail();
                    BeanUtils.copyProperties(eip, eipReturnDetail);
                    eipReturnDetail.setResourceset(Resourceset.builder()
                            .resourceid(eip.getInstanceId())
                            .resourcetype(eip.getInstanceType()).build());
                    eips.add(eipReturnDetail);
                }
                data.put("eips",eips);
                data.put("totalPages",1);
                data.put("totalElements",eips.size());
                data.put("currentPage",1);
                data.put("currentPagePer",eips.size());
            }
            return new ResponseEntity<>(data, HttpStatus.OK);
        }catch(KeycloakTokenException e){
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN,e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * get detail of the eip
     * @param eipId  the id of the eip instance
     * @return the json result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity getEipDetail(String eipId) {

        try {
            Optional<Eip> eip = eipRepository.findById(eipId);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();

                EipReturnDetail eipReturnDetail = new EipReturnDetail();
                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                                .resourceid(eipEntity.getInstanceId())
                                .resourcetype(eipEntity.getInstanceType()).build());

                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "Can not find eip by id:" + eipId+"."),
                        HttpStatus.NOT_FOUND);

            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * get eip by instance id
     * @param  instanceId  the instance id
     * @return the json result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity getEipByInstanceId(String instanceId) {

        try {
            Eip eipEntity = eipRepository.findByInstanceId(instanceId);

            if (null != eipEntity) {
                EipReturnDetail eipReturnDetail = new EipReturnDetail();

                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                        .resourceid(eipEntity.getInstanceId())
                        .resourcetype(eipEntity.getInstanceType()).build());
                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
            } else {
                log.warn("Failed to find eip by instance id, instanceId:{}", instanceId);
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "can not find instance by this id:" + instanceId+""),
                        HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * get eip by eip
     * @param  eip  the eip
     * @return the json result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity getEipByIpAddress(String eip) {

        try {
            Eip eipEntity = eipRepository.findByEipAddress(eip);

            if (null != eipEntity) {
                EipReturnDetail eipReturnDetail = new EipReturnDetail();

                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                        .resourceid(eipEntity.getInstanceId())
                        .resourcetype(eipEntity.getInstanceType()).build());
                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
            } else {
                log.warn("Failed to find eip by eip, eip:{}", eip);
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "can not find eip by this eip address:" + eip+""),
                        HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * update eip band width
     * @param id    id
     * @param param param
     * @return      result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity updateEipBandWidth(String id, EipUpdateParamWrapper param) {
        String code;
        String msg;
        try {
            JSONObject result = eipDaoService.updateEipEntity(id, param);
            if(!result.getString("interCode").equals(ReturnStatus.SC_OK)){
                code = result.getString("interCode");
                int httpResponseCode=result.getInteger("httpCode");
                msg = result.getString("reason");
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));
            }else{
                EipReturnDetail eipReturnDetail = new EipReturnDetail();
                Eip eipEntity=(Eip)result.get("data");
                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                        .resourceid(eipEntity.getInstanceId())
                        .resourcetype(eipEntity.getInstanceType()).build());
                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getCause()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);

    }

    /**
     * eip bind with port
     * @param id      id
     * @param serverId  server id
     * @return        result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity eipbindPort(String id, String type,String serverId, String portId){
        String code;
        String msg;
        try {
            switch(type){
                case "1":
                    log.info(serverId);
                    // 1：ecs
                    JSONObject result = eipDaoService.associateInstanceWithEip(id, serverId, type, portId);
                    if(!result.getString("interCode").equals(ReturnStatus.SC_OK)){
                        code = result.getString("interCode");
                        int httpResponseCode=result.getInteger("httpCode");
                        msg = result.getString("reason");
                        log.error(msg);
                        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));

                    }else{
                        EipReturnDetail eipReturnDetail = new EipReturnDetail();
                        Eip eipEntity=(Eip)result.get("data");
                        BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                        eipReturnDetail.setResourceset(Resourceset.builder()
                                .resourceid(eipEntity.getInstanceId())
                                .resourcetype(eipEntity.getInstanceType()).build());
                        return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
                    }
                case "2":
                case "3":
                default:
                    code = ReturnStatus.SC_PARAM_ERROR;
                    msg = "no support type param "+type;
                    log.warn(msg);
                    break;
            }
        } catch (Exception e) {
            log.error("eipbindPort error");
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getCause()+"";
        }
        log.info(code + msg);
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /**
     * un bind port
     * @param id    id
     * @return      result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity unBindPort(String id){

        String code;
        String msg;
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                String instanceType = eipEntity.getInstanceType();
                if(null != instanceType) {
                    switch (instanceType) {
                        case "1":
                            // 1：ecs
                            ActionResponse actionResponse = eipDaoService.disassociateInstanceWithEip(id);
                            if (actionResponse.isSuccess()){
                                EipReturnDetail eipReturnDetail = new EipReturnDetail();

                                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                                eipReturnDetail.setResourceset(Resourceset.builder()
                                        .resourceid(eipEntity.getInstanceId())
                                        .resourcetype(eipEntity.getInstanceType()).build());
                                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
                            }else{
                                code = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
                                msg = actionResponse.getFault();
                            }
                            break;
                        case "2":
                        case "3":
                        default:
                            //default ecs
                            code = ReturnStatus.SC_PARAM_ERROR;
                            msg = "no support instance type " + instanceType;
                            break;
                    }
                }else{
                    code = ReturnStatus.SC_RESOURCE_ERROR;
                    msg = "Failed to get instance type.";
                }
            } else {
                code = ReturnStatus.SC_NOT_FOUND;
                msg = "can not find eip wiht id ："+id;
            }
        } catch (Exception e) {
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getCause()+"";
        }
        log.error(msg);
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * add eip into eip pool for test
     */
    @Override
    public void addEipPool(String ip,String eip) {

        try {
            eipDaoService.addEipPool(ip, eip);
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    @Override
    @ICPServiceLog
    public ResponseEntity listServer(){
        log.info("listServer start execute");

        try {
            List<Server> serverList= (List<Server>) neutronService.listServer();
            JSONArray dataArray=new JSONArray();
            for(Server server:serverList){

                boolean bindFloatingIpFlag=true;
                Addresses addresses =server.getAddresses();

                Map<String, List<? extends Address>>  novaAddresses= addresses.getAddresses();
                Set<String> keySet =novaAddresses.keySet();
                for (String netname:keySet) {
                    List<? extends Address> address=novaAddresses.get(netname);
                    for(Address addr:address){
                        log.debug(server.getId()+server.getName()+"   "+addr.getType());
                        if (addr.getType().equals("floating")){
                            log.debug("===get this =======");
                            bindFloatingIpFlag=false;
                            break;
                        }
                    }
                }
                if(bindFloatingIpFlag){
                    JSONObject data=new JSONObject();
                    data.put("id",server.getId());
                    data.put("name",server.getName());
                    dataArray.add(data);
                }
            }

            return new ResponseEntity<>(ReturnMsgUtil.success(dataArray), HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
    /**
     * get number of the eip
     * @return the json result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity getEipNumber() {
        try {
            String projectid =CommonUtil.getProjectId();
            List<Eip> eips = eipDaoService.findByProjectId(projectid);
            return new ResponseEntity<>(ReturnMsgUtil.success(eips.size()), HttpStatus.OK);
        }catch (KeycloakTokenException e){
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN,e.getMessage()), HttpStatus.UNAUTHORIZED);
        }catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @ICPServiceLog
    public ResponseEntity getEipCount() {
        try {
            String projectid =CommonUtil.getProjectId();
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK,"get instance_num_success",eipDaoService.getInstanceNum(projectid)), HttpStatus.OK);
        }catch (KeycloakTokenException e){
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_FORBIDDEN,e.getMessage(),null), HttpStatus.UNAUTHORIZED);
        }catch(Exception e){
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage(),null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private   EipOrderResult getEipOrderResult(EipReciveOrder eipReciveOrder, String eipId, String result){
        EipOrder eipOrder = eipReciveOrder.getReturnConsoleMessage();
        List<EipOrderProduct> eipOrderProducts = eipOrder.getProductList();

        for(EipOrderProduct eipOrderProduct: eipOrderProducts){
            eipOrderProduct.setInstanceStatus(result);
            eipOrderProduct.setInstanceId(eipId);
            eipOrderProduct.setStatusTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }

        EipOrderResult eipOrderResult = new EipOrderResult();
        eipOrderResult.setUserId(eipOrder.getUserId());
        eipOrderResult.setConsoleOrderFlowId(eipReciveOrder.getConsoleOrderFlowId());
        eipOrderResult.setOrderId(eipReciveOrder.getOrderId());

        List<EipOrderResultProduct> eipOrderResultProducts = new ArrayList<>();
        EipOrderResultProduct eipOrderResultProduct = new EipOrderResultProduct();
        eipOrderResultProduct.setOrderDetailFlowId(eipReciveOrder.getOrderDetailFlowIdList().get(0));
        eipOrderResultProduct.setProductSetStatus(result);
        eipOrderResultProduct.setBillType(eipOrder.getBillType());
        eipOrderResultProduct.setDuration(eipOrder.getDuration());
        eipOrderResultProduct.setOrderType(eipOrder.getOrderType());
        eipOrderResultProduct.setProductList(eipOrder.getProductList());


        eipOrderResultProducts.add(eipOrderResultProduct);
        eipOrderResult.setProductSetList(eipOrderResultProducts);
        return eipOrderResult;
    }
}
