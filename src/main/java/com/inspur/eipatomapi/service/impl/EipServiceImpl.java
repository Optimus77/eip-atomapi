package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.service.EipDaoService;
import com.inspur.eipatomapi.service.IEipService;
import com.inspur.eipatomapi.service.NeutronService;
import com.inspur.eipatomapi.util.*;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
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

import java.util.*;


@Service
public class EipServiceImpl implements IEipService {

    @Autowired
    private EipRepository eipRepository;

    @Autowired
    private NeutronService neutronService;

    @Autowired
    private EipDaoService eipDaoService;

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
            EipPool eip = eipDaoService.getOneEipFromPool();
            if(null == eip) {
                msg = "Failed, no eip in eip pool.";
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_RESOURCE_NOTENOUGH, msg),
                        HttpStatus.FAILED_DEPENDENCY);
            }

            Eip eipMo = eipDaoService.allocateEip(eipConfig, eip,null);
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
            log.error("Exception in atomCreateEip", e.getMessage());
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
                log.info("Atom delete eip failed,{}", msg);
            }
        }catch (Exception e){
            log.error("Exception in atomDeleteEip", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
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



    @ICPServiceLog
    public ResponseEntity renewEip(String eipId,  EipAllocateParam eipUpdateInfo) {
        String msg = "";
        String code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;

        try {
            String addTime = eipUpdateInfo.getDuration();
            if(null == addTime){
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.BAD_REQUEST);
            }else if(addTime.trim().equals("0")){

                ActionResponse actionResponse = eipDaoService.softDownEip(eipId);
                if(actionResponse.isSuccess()) {
                    return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
                }else {
                    return new ResponseEntity<>(ReturnMsgUtil.error(
                            String.valueOf(actionResponse.getCode()), actionResponse.getFault()),
                            HttpStatus.BAD_REQUEST);
                }
            }
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


    /**
     *  list the eip
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity listEips(int currentPage,int limit, String status){

        try {
            String projcectid=CommonUtil.getUserId();
            log.debug("listEips  of user, userId:{}", projcectid);
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
                    if((null != status) && (!eip.getStatus().trim().equalsIgnoreCase(status))){
                        continue;
                    }
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
                    if((null != status) && (!eip.getStatus().trim().equalsIgnoreCase(status))){
                        continue;
                    }
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
//                    log.info("bind a slb:{} with eipId:{}",slbId,,Type,slbIp);
//                    // 3：slb
//                    JSONObject result = eipDaoService.associateInstanceWithEip(id, slbId, type, slbIp);
//                    if(!result.getString("interCode").equals(ReturnStatus.SC_OK)){
//                        code = result.getString("interCode");
//                        int httpResponseCode=result.getInteger("httpCode");
//                        msg = result.getString("reason");
//                        log.error(msg);
//                        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));
//
//                    }else{
//                        EipReturnDetail eipReturnDetail = new EipReturnDetail();
//                        Eip eipEntity=(Eip)result.get("data");
//                        BeanUtils.copyProperties(eipEntity, eipReturnDetail);
//                        eipReturnDetail.setResourceset(Resourceset.builder()
//                                .resourceid(eipEntity.getInstanceId())
//                                .resourcetype(eipEntity.getInstanceType()).build());
//                        return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
//                    }
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
                msg = "can not find eip id ："+id;
            }
        } catch (Exception e) {
            log.error("Exception in unBindPort", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        log.error(msg);
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }



    @Override
    @ICPServiceLog
    public ResponseEntity listServer(String region){
        log.info("listServer start execute");

        try {
            List<Server> serverList= (List<Server>) neutronService.listServer(region);
            JSONArray dataArray=new JSONArray();
            for(Server server:serverList){
                Addresses addresses =server.getAddresses();
                if(!server.getName().trim().startsWith("CPS")) {
                    boolean bindFloatingIpFlag=true;
                    Map<String, List<? extends Address>> novaAddresses = addresses.getAddresses();
                    Set<String> keySet = novaAddresses.keySet();
                    for (String netname : keySet) {
                        List<? extends Address> address = novaAddresses.get(netname);
                        for (Address addr : address) {
                            log.debug("Get server: id:{}, name:{}, addr:{}.", server.getId(), server.getName(), addr.getType());
                            if (addr.getType().equals("floating")) {
                                log.debug("===get this =======");
                                bindFloatingIpFlag = false;
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
            }

            return new ResponseEntity<>(ReturnMsgUtil.success(dataArray), HttpStatus.OK);
        }catch (Exception e){
            log.error("Exception in listServer", e);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
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



    /**
     * eip bind with port
     * @param eipId     eip id
     * @param slbId     eipid
     * @param ipAddr    slbip
     * @return        result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity eipbindSlb(String eipId, String slbId, String ipAddr) {
        String code;
        String msg;
        // bind slb
        JSONObject result = null;
        try {
            result = eipDaoService.associateSlbWithEip(eipId, slbId, ipAddr);
            if (!result.getString("interCode").equals(ReturnStatus.SC_OK)){
                code = result.getString("interCode");
                int httpResponseCode=result.getInteger("httpCode");
                msg = result.getString("reason");
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));
            }else {
                code = ReturnStatus.SC_OK;
                msg = "The Eip binding Slb succeeded";
                log.info(msg);
                log.info(code);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.OK);
            }
        }catch (Exception e){
            log.error("eipbindSlb exception", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * eip bind with port
     * @param slbId     eipid
     * @return        result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity unBindSlb(String slbId) {
        String code;
        String msg;
        // slb
        JSONObject result = null;
        try {
            ActionResponse actionResponse = eipDaoService.disassociateSlbWithEip(slbId);
            if (actionResponse.isSuccess()){
                code = ReturnStatus.SC_OK;
                msg=("The Eip unbinds the Slb successfully");
                log.info(code);
                log.info(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.OK);
            }else {
                code = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
                msg = actionResponse.getFault();
                log.error(code);
                log.error(msg);
            }
        }catch (Exception e){
            log.error("eipbindSlb exception", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }




}
