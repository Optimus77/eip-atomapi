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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${mq.pushMq}")
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

            log.error("Exception in atomCreateEip", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
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
            log.error("Exception in atomDeleteEip", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
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
                    returnsWebsocket(eipMo.getEipId(),eipOrder,HsConstants.SUCCESS);

                    bssApiService.resultReturnMq(getEipOrderResult(eipOrder, eipMo.getEipId(),HsConstants.SUCCESS));
                    return new ResponseEntity<>(ReturnMsgUtil.success(eipInfo), HttpStatus.OK);
                } else {
                    code = ReturnStatus.SC_OPENSTACK_FIPCREATE_ERROR;
                    msg = "Failed to allocate eip by config:" + eipConfig.toString();
                    log.error(msg);
                }
            }else {
                bssApiService.resultReturnMq(getEipOrderResult(eipOrder, "",HsConstants.FAIL));
                code = ReturnStatus.SC_RESOURCE_ERROR;
                msg = "not payed.";
                log.info(msg);
            }
        }catch (Exception e){
            log.error("Exception in createEip", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
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
                if(eipOrderProductItem.getCode().equals("bandwidth") &&
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
        String errorMsg = "Param:";
        if(param.getBandwidth() > 2000 || param.getBandwidth() < 1){
            errorMsg = "bandwidht:value must be 1-2000.";
        }
        if(!param.getChargemode().equals(HsConstants.BANDWIDTH) &&
                !param.getChargemode().equals(HsConstants.SHAREDBANDWIDTH)){
            errorMsg = errorMsg + "chargemode:Only Bandwidth,SharedBandwidth is allowed. ";
        }

        if(!param.getBillType().equals(HsConstants.MONTHLY) && !param.getBillType().equals(HsConstants.HOURLYSETTLEMENT)){
            errorMsg = errorMsg + "billType:Only monthly,hourlySettlement is allowed. ";
        }
        if(param.getRegion().isEmpty()){
            errorMsg = errorMsg + "region:can not be blank.";
        }
        String tp = param.getIptype();
        if(!tp.equals("5_bgp") && !tp.equals("5_sbgp") && !tp.equals("5_telcom") &&
                !tp.equals("5_union") && !tp.equals("BGP")){
            errorMsg = errorMsg +"iptype:Only 5_bgp,5_sbgp, 5_telcom, 5_union ,  BGP is allowed. ";
        }
        if(errorMsg.equals("Param:")) {
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
                log.info("delete eip {}", eipId);
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
            log.error("Exception in deleteEipList", e);
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
            EipOrder retrunMsg =  eipOrder.getReturnConsoleMessage();
            if(eipOrder.getOrderStatus().equals("createSuccess")  ||
             retrunMsg.getBillType().equals(HsConstants.HOURLYSETTLEMENT)) {
                ActionResponse actionResponse =  eipDaoService.deleteEip(eipId);
                if (actionResponse.isSuccess()){


                    //Return message to the front des
                    returnsWebsocket(eipId,eipOrder,"delete");

                    bssApiService.resultReturnMq(getEipOrderResult(eipOrder, eipId,HsConstants.SUCCESS));
                    return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
                }else {
                    msg = actionResponse.getFault();
                    code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
                }
            }else{
                msg = "Failed to delete eip,failed to create delete. orderStatus: "+eipOrder.getOrderStatus();
                code = ReturnStatus.SC_PARAM_UNKONWERROR;
                log.error(msg);
            }
        }catch (Exception e){
            log.error("Exception in deleteEip", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        bssApiService.resultReturnMq(getEipOrderResult(eipOrder, eipId,HsConstants.FAIL));
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ICPServiceLog
    public ResponseEntity softDownEip(String eipId, EipSoftDownOrder eipOrder) {
        String msg = "";
        String code;
        int failFlag = 0;

        try {
            List<EipSoftDownInstance>  eipRenewInstances  = eipOrder.getInstanceList();
            for(EipSoftDownInstance eipRenewInstance: eipRenewInstances){
                ActionResponse actionResponse = eipDaoService.softDownEip(eipRenewInstance.getInstanceId());
                if(!actionResponse.isSuccess()){
                    failFlag = failFlag + 1;
                    msg = msg +  actionResponse.getFault();
                }
            }
            if(failFlag == 0){
                bssApiService.resultReturnMq(getEipSoftDownOrderResult(eipOrder,HsConstants.SUCCESS));
                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
            }else {
                code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            }
        }catch (Exception e){
            log.error("Exception in deleteEip", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        bssApiService.resultReturnMq(getEipSoftDownOrderResult(eipOrder,HsConstants.FAIL));
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ICPServiceLog
    public ResponseEntity renewEip(String eipId,  EipAllocateParam eipUpdateInfo) {
        String msg = "";
        String code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;

        try {
            String addTime = eipUpdateInfo.getDuration();

            ActionResponse actionResponse = eipDaoService.reNewEipEntity(eipId, addTime);
            if(actionResponse.isSuccess()){
                log.info("renew eip:{} , add duration:{}",eipId, addTime);

                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
            }else{
                msg = actionResponse.getFault();
                log.error(msg);
            }
        }catch (Exception e){
            log.error("Exception in deleteEip", e);
            msg = e.getMessage()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

//
//    @ICPServiceLog
//    public ResponseEntity renewEip(String eipId, EipReciveOrder eipOrder) {
//        String msg = "";
//        String code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
//
//        try {
//            EipOrder eipReturn = eipOrder.getReturnConsoleMessage();
//            String addTime = eipReturn.getDuration();
//
//            ActionResponse actionResponse = eipDaoService.reNewEipEntity(eipId, addTime);
//            if(actionResponse.isSuccess()){
//                log.info("renew eip:{} , add duration:{}",eipId, addTime);
//
//                //Return message to the front des
//                returnsWebsocket(eipId,eipOrder,"renew");
//
//                bssApiService.resultReturnMq(getEipOrderResult(eipOrder, eipId, HsConstants.SUCCESS));
//                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
//            }else{
//                msg = actionResponse.getFault();
//                log.error(msg);
//            }
//        }catch (Exception e){
//            log.error("Exception in deleteEip", e);
//            msg = e.getMessage()+"";
//        }
//        bssApiService.resultReturnMq(getEipOrderResult(eipOrder,eipId,HsConstants.FAIL));
//        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
//    }

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
                return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
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
                List<Eip> eipList=eipDaoService.findByProjectId(projcectid);
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
            log.error("Exception in listEips", e);
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
            Eip eipEntity = eipDaoService.getEipById(eipId);
            if (null != eipEntity) {
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
            log.error("Exception in getEipDetail", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
            Eip eipEntity = eipDaoService.findByInstanceId(instanceId);

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
            log.error("Exception in getEipByInstanceId", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
            Eip eipEntity = eipDaoService.findByEipAddress(eip);

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
            log.error("Exception in getEipByIpAddress", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
            log.error("Exception in updateEipBandWidth", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
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
                    log.info("bind a server:{} with eipId:{}",serverId,id);
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
            log.error("eipbindPort exception", e);

            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        log.info("Error when bind port，code:{}, msg:{}.", code, msg);
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
            Eip eipEntity = eipDaoService.getEipById(id);
            if (null != eipEntity) {
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
            log.error("Exception in unBindPort", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
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
            log.error("Exception in addEipPool", e);
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
                        log.debug("Get server: id:{}, name:{}, addr:{}.",server.getId(),server.getName(),addr.getType());
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
            log.error("Exception in listServer", e);
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
            String projectid =CommonUtil.getUserId();
            List<Eip> eips = eipDaoService.findByProjectId(projectid);
            log.info("Get projectid:{} eip number:{}.", projectid, eips.size());
            return new ResponseEntity<>(ReturnMsgUtil.success(eips.size()), HttpStatus.OK);
        }catch (KeycloakTokenException e){
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN,e.getMessage()), HttpStatus.UNAUTHORIZED);
        }catch(Exception e){
            log.error("Exception in getEipNumber", e);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @ICPServiceLog
    public ResponseEntity getEipCount() {
        try {
            String projectid =CommonUtil.getUserId();
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


    private   EipOrderResult getEipSoftDownOrderResult(EipSoftDownOrder eipReciveOrder, String result){
        EipOrderResult eipSoftDownOrder = new EipOrderResult();
        eipReciveOrder.setFlowId("test");
        return eipSoftDownOrder;
    }

    public void returnsWebsocket(String eipId,EipReciveOrder eipOrder,String type){
        if ("console".equals(eipOrder.getReturnConsoleMessage().getOrderSource())){
            try {
                SendMQEIP sendMQEIP = new SendMQEIP();
                sendMQEIP.setUserName(CommonUtil.getUsername());
                sendMQEIP.setHandlerName("operateEipHandler");
                sendMQEIP.setInstanceId(eipId);
                sendMQEIP.setInstanceStatus("active");
                sendMQEIP.setOperateType(type);
                sendMQEIP.setMessageType(HsConstants.SUCCESS);
                sendMQEIP.setMessage(CodeInfo.getCodeMessage(CodeInfo.EIP_RENEWAL_SUCCEEDED));
                String url=pushMq;
                log.info(url);
                String orderStr=JSONObject.toJSONString(sendMQEIP);
                log.info("return mq body str {}",orderStr);
                Map<String,String> headers = new HashMap<>();
                headers.put("Authorization", CommonUtil.getKeycloackToken());
                headers.put(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
                HttpResponse response = HttpUtil.post(url,headers,orderStr);
                log.info(response.getEntity().toString());
                log.info(String.valueOf(response.getStatusLine().getStatusCode()));
            } catch (KeycloakTokenException e) {
                e.printStackTrace();
            }
        }else {
            log.info("Wrong source of order",eipOrder.getReturnConsoleMessage().getOrderSource());
        }
    }

}
