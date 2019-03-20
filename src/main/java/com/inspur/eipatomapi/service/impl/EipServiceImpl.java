package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.MethodReturn;
import com.inspur.eipatomapi.entity.NovaServerEntity;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.entity.eipv6.EipV6;
import com.inspur.eipatomapi.entity.eipv6.EipV6AllocateParam;
import com.inspur.eipatomapi.entity.sbw.Sbw;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.EipV6Repository;
import com.inspur.eipatomapi.service.*;
import com.inspur.eipatomapi.util.*;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.ActionResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class EipServiceImpl implements IEipService {

    @Autowired
    private EipRepository eipRepository;

    @Autowired
    private NeutronService neutronService;

    @Autowired
    private EipDaoService eipDaoService;

    @Autowired
    private PortService portService;

    @Autowired
    private SbwDaoService sbwDaoService;

    @Autowired
    private EipV6Repository eipV6Repository;

    @Autowired
    private EipV6ServiceImpl eipV6Service;


    /**
     * create a eip
     * @param eipConfig          config
     * @return                   json info of eip
     */
    public ResponseEntity atomCreateEip(EipAllocateParam eipConfig) {

        String code;
        String msg;
        try {
            String sbwId = eipConfig.getSharedBandWidthId();
            if(null != sbwId) {
                Sbw sbwEntity = sbwDaoService.getSbwById(sbwId);
                if (null == sbwEntity || (!sbwEntity.getProjectId().equalsIgnoreCase(CommonUtil.getUserId()))){
                    log.warn(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), sbwId);
                    return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_RESOURCE_NOTENOUGH,
                            "Can not find sbw"), HttpStatus.FAILED_DEPENDENCY);
                }
            }
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
                if(eipConfig.getIpv6().equalsIgnoreCase("yes")){
                    EipV6AllocateParam eipV6AllocateParam = new EipV6AllocateParam();
                    eipV6AllocateParam.setEipId(eipMo.getEipId());
                    eipV6Service.atomCreateEipV6(eipV6AllocateParam);
                }
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
    public ResponseEntity deleteEipList(List<String> eipIds) {
        String errorMsg;
        try {
            ActionResponse actionResponse;
            List<String > failedIds = new ArrayList<>();
            for (String eipId : eipIds) {
                log.info("delete eip {}", eipId);
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
            }else {
                ActionResponse actionResponse = eipDaoService.reNewEipEntity(eipId, addTime);
                if (actionResponse.isSuccess()) {
                    log.info("renew eip:{} , add duration:{}", eipId, addTime);

                    return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
                } else {
                    msg = actionResponse.getFault();
                    log.error(msg);
                }
            }
        }catch (Exception e){
            log.error("Exception in deleteEip", e);
            msg = e.getMessage()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     *  listShareBandWidth the eip
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    @Override
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
                Page<Eip> page=eipRepository.findByUserIdAndIsDelete(projcectid, 0, pageable);
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
                List<Eip> eipList=eipDaoService.findByUserId(projcectid);
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
                log.debug("Failed to find eip by instance id, instanceId:{}", instanceId);
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
    public ResponseEntity updateEipBandWidth(String id, EipUpdateParamWrapper param) {
        String code;
        String msg;
        try {
            MethodReturn result = eipDaoService.updateEipEntity(id, param);
            if(!result.getInnerCode().equals(ReturnStatus.SC_OK)){
                code = result.getInnerCode();
                int httpResponseCode=result.getHttpCode();
                msg = result.getMessage();
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));
            }else{
                EipReturnDetail eipReturnDetail = new EipReturnDetail();
                Eip eipEntity=(Eip)result.getEip();
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
    public ResponseEntity eipbindPort(String id, String type,String serverId, String portId,String addrIp){
        String code;
        String msg;
        try {
            switch(type){
                case "1":
                    log.debug("bind a server:{} port:{} with eipId:{}",serverId, portId, id);
                    // 1：ecs
                    MethodReturn result = eipDaoService.associateInstanceWithEip(id, serverId, type, portId);
                    if(!result.getInnerCode().equals(ReturnStatus.SC_OK)){
                        msg = result.getMessage();
                        log.error(msg);
                        return new ResponseEntity<>(ReturnMsgUtil.error(result.getInnerCode(), msg),
                                HttpStatus.valueOf(result.getHttpCode()));
                    }else{
                        EipReturnDetail eipReturnDetail = new EipReturnDetail();
                        Eip eipEntity=(Eip)result.getEip();
                        BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                        eipReturnDetail.setResourceset(Resourceset.builder()
                                .resourceid(eipEntity.getInstanceId())
                                .resourcetype(eipEntity.getInstanceType()).build());
                        return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
                    }
                case "2":
                case "3":
                    return eipbindInstance( id,  serverId,  addrIp, type);

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
                            return unBindInstance(eipEntity.getInstanceId());
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
    public ResponseEntity listServer(String region, String tag){
        log.info("listServer start execute");

        try {
            OSClient.OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
            List<NovaServerEntity> serverList= portService.listServerByTags(tag, osClientV3);
            JSONArray dataArray=new JSONArray();
            for(NovaServerEntity server:serverList){
                log.debug("Server listShareBandWidth , name:{}.",server.getName());
                String serverId = server.getId();
                if(null == eipDaoService.findByInstanceId(serverId)) {
                    List<String> portIds = neutronService.getPortIdByServerId(serverId, osClientV3);
                    JSONObject data = new JSONObject();
                    data.put("id", server.getId());
                    data.put("name", server.getName());
                    for (int i = 0; i < portIds.size(); i++) {
                        data.put("port" + i, portIds.get(i));
                    }
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


    @Override
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
     * @param InstanceId     eipid
     * @param ipAddr    Addrip
     * @return        result
     */
    public ResponseEntity eipbindInstance(String eipId, String InstanceId, String ipAddr,String type) {
        String code;
        String msg;
        // bind slb
        MethodReturn result;
        try {
            result = eipDaoService.cpsOrSlbBindEip(eipId, InstanceId, ipAddr,type);
            if (!result.getInnerCode().equals(ReturnStatus.SC_OK)){
                msg = result.getMessage();
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(result.getInnerCode(), msg),
                        HttpStatus.valueOf(result.getHttpCode()));
            }else {
                msg = "The Eip binding Instance succeeded";
                log.info(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error( ReturnStatus.SC_OK, msg), HttpStatus.OK);
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
     * @param instanceId     eipid
     * @return        result
     */
    public ResponseEntity unBindInstance(String instanceId) {
        String code;
        String msg;

        try {
            ActionResponse actionResponse = eipDaoService.unCpcOrSlbBindEip(instanceId);
            if (actionResponse.isSuccess()){
                code = ReturnStatus.SC_OK;
                msg=("The Eip unbinds the instance successfully");
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
            log.error("eipbindInstance exception", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ICPServiceLog
    @Override
    public ResponseEntity addEipToSbw(String eipId ,EipUpdateParam eipUpdateParam ){
        String code;
        String msg;
        MethodReturn result ;
        try {
            result = sbwDaoService.addEipIntoSbw(eipId, eipUpdateParam);
            if(!result.getInnerCode().equals(ReturnStatus.SC_OK)){
                msg = result.getMessage();
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(result.getInnerCode(), msg),
                        HttpStatus.valueOf(result.getHttpCode()));
            }else {
                msg = "The Eip add to shared band succeeded";
                log.info(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_OK, msg), HttpStatus.OK);
            }

        } catch (Exception e) {
            log.error("eip add to shared band exception", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Override
    public ResponseEntity removeEipFromSbw(String eipId, EipUpdateParam eipUpdateParam) {
        String code;
        String msg;

        try {
            ActionResponse actionResponse = sbwDaoService.removeEipFromSbw(eipId, eipUpdateParam);
            if (actionResponse.isSuccess()){
                code = ReturnStatus.SC_OK;
                msg=("remove from shared successfully");
                log.info(code+":"+msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.OK);
            }else {
                code = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
                msg = actionResponse.getFault();
                log.info(code+":"+msg);
            }
        } catch (Exception e) {
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
            log.error("code:{}, msg:{},remove from sbw  exception",code, msg, e);
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }



    /**
     *   the eipV6
     * @return       result
     */
    @Override
    public ResponseEntity listEipsByBandWidth( String status){

        try {
            String projcectid=CommonUtil.getUserId();
            log.debug("listEips  of user, userId:{}", projcectid);
            if(projcectid==null){
                return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data=new JSONObject();
            JSONArray eips=new JSONArray();
            ArrayList<Eip> newList = new ArrayList();
            ArrayList<Eip> newEipList = new ArrayList();
            List<Eip> eipList=eipDaoService.findByUserId(projcectid);
            for (Eip eip : eipList) {
                String eipAddress = eip.getEipAddress();
                EipV6 eipV6 = eipV6Repository.findByIpv4AndUserIdAndIsDelete(eipAddress, projcectid, 0);
                if (eipV6 == null) {
                    newEipList.add(eip);
                }
            }
            for (Eip eip : newEipList) {
                if((null != status) && (!eip.getStatus().trim().equalsIgnoreCase(status))){
                    continue;
                }
                if(eip.getBandWidth()<=10){
                    if(!StringUtils.isNotEmpty(eip.getSharedBandWidthId())){
                        EipReturnByBandWidth eipReturnDetail = new EipReturnByBandWidth();
                        BeanUtils.copyProperties(eip, eipReturnDetail);
                        eips.add(eipReturnDetail);
                        data.put("eip",eips);
                        newList.add(eip);
                        data.put("totalElements",newList.size());
                    }
                }
            }
            return new ResponseEntity<>(data, HttpStatus.OK);
        }catch(KeycloakTokenException e){
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN,e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e){
            log.error("Exception in listEips", e);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
