package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.inspur.eipatomapi.entity.bss.*;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.service.BssApiService;
import com.inspur.eipatomapi.service.EipDaoService;
import com.inspur.eipatomapi.service.IEipService;
import com.inspur.eipatomapi.service.NeutronService;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.KeycloakTokenException;
import com.inspur.eipatomapi.util.ReturnMsgUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
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

import java.util.*;

/**
 * @Auther: jiasirui
 * @Date: 2018/9/14 09:32
 * @Description:  the Eip Service Interface
 */

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

    public final static Logger log = LoggerFactory.getLogger(EipServiceImpl.class);


    //1.2.8	订单提交接口
    @Override
    @ICPServiceLog
    public ResponseEntity createOrder(EipAllocateParam eipAllocateParam) {
        EipOrder order = getOrderByEipParam(eipAllocateParam.getBandwidth(), eipAllocateParam.getIptype(),
                eipAllocateParam.getRegion(), eipAllocateParam.getPurchasetime());
        order.setConsoleCustomization((JSONObject)JSON.toJSON(eipAllocateParam));
        try{
            JSONObject result=bssApiService.createOrder(order);
            if(result.getString("code").equals("0")){
                return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK,"success",result.get("data")), HttpStatus.OK);
            }else{
                return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,"fail",result.get("data")), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception e){
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage(),null), HttpStatus.INTERNAL_SERVER_ERROR);
        }

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
            String eipConfigJson = eipOrder.getReturnConsoleMessage().getConsoleCustomization().toJSONString();
            EipAllocateParam eipConfig = JSONObject.parseObject(eipConfigJson, new TypeReference<EipAllocateParam>(){});

            Eip eipMo = eipDaoService.allocateEip(eipConfig, null);
            if (null != eipMo) {
                EipReturnBase eipInfo = new EipReturnBase();
                BeanUtils.copyProperties(eipMo, eipInfo);

                bssApiService.resultReturnMq(getEipOrderResult(eipOrder, "success"));
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
        bssApiService.resultReturnMq(getEipOrderResult(eipOrder, "failed"));
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
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
        for(String eipId: eipIds) {
            log.info("delete eip "+eipId);
            deleteEip(eipId, null);
        }
        return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
    }

    /**
     * delete eip
     * @param eipId eipid
     * @return return
     */
    @ICPServiceLog
    public ResponseEntity deleteEipOrder(String eipId) {
        try{
            Eip eipEntity = eipRepository.findByEipId(eipId);
            if(null == eipEntity){
                log.error("In disassociate process,failed to find the eip by id:{} ",eipId);
                return new ResponseEntity<>(ReturnMsgUtil.error( ReturnStatus.SC_PARAM_UNKONWERROR,
                        "Can not find eip"),
                        HttpStatus.NOT_FOUND);
            }
            EipOrder order = getOrderByEipParam(eipEntity.getBandWidth(), eipEntity.getIpType(), null,
                    eipEntity.getPurchaseTime());
            order.setOrderType("unsubscribe");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("eipId", eipEntity.getEipId());
            order.setConsoleCustomization(jsonObject);

            JSONObject result=bssApiService.createOrder(order);
            if(result.getString("code").equals("0")){
                return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK,"success",
                        result.get("data")), HttpStatus.OK);
            }else{
                return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,
                        "fail",result.get("data")), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception e){
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage(),null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
            if(eipDaoService.deleteEip(eipId)){
                bssApiService.resultReturnMq(getEipOrderResult(eipOrder, "success"));
                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
            } else {
                msg = "Failed to delete eip,eip is bind to port or fip not found.";
                code = ReturnStatus.SC_PARAM_UNKONWERROR;
                log.error(msg);
            }
        }catch (Exception e){
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getCause()+"";
        }
        bssApiService.resultReturnMq(getEipOrderResult(eipOrder, "failed"));
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
        log.info("listEips  service start execute");
        try {
            String projcectid=CommonUtil.getProjectId();
            log.info(projcectid);
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
                            .resource_id(eip.getInstanceId())
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
                            .resource_id(eip.getInstanceId())
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
                                .resource_id(eipEntity.getInstanceId())
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
                        .resource_id(eipEntity.getInstanceId())
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
                        .resource_id(eipEntity.getInstanceId())
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
            if(!result.getBoolean("flag")){
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
                        .resource_id(eipEntity.getInstanceId())
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
                    if(!result.getBoolean("flag")){
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
                                .resource_id(eipEntity.getInstanceId())
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
                            if (!eipDaoService.disassociateInstanceWithEip(id)) {
                                code = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
                                msg = "Failed to disassociate  port with eip " + id;
                            } else {
                                EipReturnDetail eipReturnDetail = new EipReturnDetail();

                                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                                eipReturnDetail.setResourceset(Resourceset.builder()
                                        .resource_id(eipEntity.getInstanceId())
                                        .resourcetype(eipEntity.getInstanceType()).build());
                                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
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
    public void addEipPool(String ip) {

        try {
            eipDaoService.addEipPool(ip);
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

    //1.2.1 查询当前用户余额
    @Override
    @ICPServiceLog
    public ResponseEntity userBanlance() {
        try{
            JSONObject result=bssApiService.getUserBalance(CommonUtil.getUserId());
            if(result.getBoolean("success")){
                return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK,"success",result.get("data")), HttpStatus.OK);
            }else{
                return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,"fail",result.get("data")), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch(KeycloakTokenException e){
            e.printStackTrace();
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_PARAM_ERROR,e.getMessage(),null), HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage(),null), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    @ICPServiceLog
    public ResponseEntity orderUpdate(String eipId, EipUpdateParam param) {
        try {
            Eip eipEntity = eipRepository.findByEipId(eipId);
            if(null == eipEntity){
                log.error("In disassociate process,failed to find the eip by id:{} ",eipId);
                return new ResponseEntity<>(ReturnMsgUtil.error( ReturnStatus.SC_PARAM_UNKONWERROR,
                        "Can not find eip"),
                        HttpStatus.NOT_FOUND);
            }
            EipOrder order =  getOrderByEipParam(param.getBandWidth(), param.getType(), null, null);
            order.setOrderType("upgrade");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("eipId", eipId);
            jsonObject.put("bandwidth", param.getBandWidth());
            jsonObject.put("chargetype", param.getChargeType());
            order.setConsoleCustomization(jsonObject);

            JSONObject result=bssApiService.createOrder(order);
            if(result.getBoolean("success")){
                return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK,"success",
                        result.get("data")), HttpStatus.OK);
            }else{
                return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,
                        "fail",result.get("data")), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception e){
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage(),null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    //1.2.13	查询用户配额的接口
    @Override
    @ICPServiceLog
    public ResponseEntity getQuota(){
        try{
            EipQuota quota=new EipQuota();
            quota.setProductLineCode("EIP");
            quota.setRegion(CommonUtil.getReginInfo());
            quota.setProductTypeCode(null);
            quota.setUserId(CommonUtil.getUserId());
            JSONObject result=bssApiService.getQuota(quota);
            if(result.getBoolean("success")){
                return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK,"success",result.get("data")), HttpStatus.OK);
            }else{
                return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,"fail",result.get("data")), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }catch (Exception e){
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage(),null), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    private EipOrder getOrderByEipParam(int bandWidth, String ipType, String region, String purchasetime) {

        List<EipOrderProductItem> itemList = new ArrayList<>();
        EipOrderProductItem bandWidthItem = new EipOrderProductItem();
        bandWidthItem.setCode("net");
        bandWidthItem.setName("bandwidth");
        bandWidthItem.setUnit("M");
        bandWidthItem.setValue(String.valueOf(bandWidth));
        bandWidthItem.setPropertyType("billingItem");

        EipOrderProductItem ipTypeItem = new EipOrderProductItem();
        ipTypeItem.setCode("provider");
        ipTypeItem.setName(ipType);
        ipTypeItem.setValue("BGP");
        ipTypeItem.setPropertyType("impactFactor");

        itemList.add(bandWidthItem);
        itemList.add(ipTypeItem);

        EipOrderProduct eipOrderProduct = new EipOrderProduct();
        eipOrderProduct.setItemList(itemList);
        eipOrderProduct.setRegion(region);
        eipOrderProduct.setAvailableZone("");
        eipOrderProduct.setInstanceId("");

        EipOrder eipOrder = new EipOrder();
        try {
//            eipOrder.setUserId(CommonUtil.getUserId());
            eipOrder.setUserId("useriduseriduserid");
        }catch (Exception e){
            e.printStackTrace();
        }
        eipOrder.setConsoleOrderFlowId(UUID.randomUUID().toString());
        List<EipOrderProduct> orders = new ArrayList<>();
        orders.add(eipOrderProduct);
        eipOrder.setDuration(purchasetime);
        eipOrder.setProductList(orders);

        return eipOrder;
    }

    private   EipOrderResult getEipOrderResult(EipReciveOrder eipReciveOrder, String result){
        EipOrder eipOrder = eipReciveOrder.getReturnConsoleMessage();
        EipOrderResult eipOrderResult = new EipOrderResult();
        eipOrderResult.setUserId(eipOrder.getUserId());
        eipOrderResult.setProductLineCode("EIP");
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
