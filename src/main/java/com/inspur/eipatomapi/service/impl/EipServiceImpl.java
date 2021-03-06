package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.MethodReturn;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.entity.eipv6.EipV6;
import com.inspur.eipatomapi.entity.sbw.Sbw;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.EipV6Repository;
import com.inspur.eipatomapi.service.*;
import com.inspur.eipatomapi.util.*;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    private EipDaoService eipDaoService;

    @Autowired
    private SbwDaoService sbwDaoService;

    @Autowired
    private EipV6Repository eipV6Repository;

    @Autowired
    private EipV6ServiceImpl eipV6Service;


    /**
     * create a eip
     *
     * @param eipConfig config
     * @return json info of eip
     */
    public ResponseEntity atomCreateEip(EipAllocateParam eipConfig) {

        String code;
        String msg;
        try {
            String sbwId = eipConfig.getSbwId();
            if (null != sbwId) {
                Sbw sbwEntity = sbwDaoService.getSbwById(sbwId);
                if (null == sbwEntity || (!sbwEntity.getProjectId().equalsIgnoreCase(CommonUtil.getUserId()))) {
                    log.warn(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), sbwId);
                    return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_RESOURCE_NOTENOUGH,
                            "Can not find sbw"), HttpStatus.FAILED_DEPENDENCY);
                }
            }
            EipPool eip = eipDaoService.getOneEipFromPool();
            if (null == eip) {
                msg = "Failed, no eip in eip pool.";
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_RESOURCE_NOTENOUGH, msg),
                        HttpStatus.FAILED_DEPENDENCY);
            }

            Eip eipMo = eipDaoService.allocateEip(eipConfig, eip, null);
            if (null != eipMo) {
                EipReturnBase eipInfo = new EipReturnBase();
                BeanUtils.copyProperties(eipMo, eipInfo);
                log.info("Atom create a eip success:{}", eipMo);
                if (eipConfig.getIpv6().equalsIgnoreCase("yes")) {
                    eipV6Service.atomCreateEipV6(eipMo.getId());
                }
                return new ResponseEntity<>(ReturnMsgUtil.success(eipInfo), HttpStatus.OK);
            } else {
                code = ReturnStatus.SC_OPENSTACK_FIPCREATE_ERROR;
                msg = "Failed to create floating ip in external network:" + eipConfig.getRegion();
                log.error(msg);
            }

        } catch (Exception e) {
            log.error("Exception in atomCreateEip", e.getMessage());
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage() + "";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * delete eip
     *
     * @param eipId eipid
     * @return return
     */
    public ResponseEntity atomDeleteEip(String eipId) {
        String msg;
        String code;

        try {
            ActionResponse actionResponse = eipDaoService.deleteEip(eipId);
            if (actionResponse.isSuccess()) {
                log.info("Atom delete eip successfully, eipId:{}", eipId);
                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
            } else {
                msg = actionResponse.getFault();
                code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
                log.info("Atom delete eip failed,{}", msg);
            }
        } catch (Exception e) {
            log.error("Exception in atomDeleteEip", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage() + "";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 1.delete  floatingIp
     * 2.Determine if Snate and Qos is deleted
     * 3.delete eip
     *
     * @param eipIds eip ids
     * @return result: true/false
     */
    @Override
    public ResponseEntity deleteEipList(List<String> eipIds) {
        String errorMsg;
        try {
            ActionResponse actionResponse;
            List<String> failedIds = new ArrayList<>();
            for (String eipId : eipIds) {
                log.info("delete eip {}", eipId);
                actionResponse = eipDaoService.deleteEip(eipId);
                if (!actionResponse.isSuccess()) {
                    failedIds.add(eipId);
                    log.error("delete eip error, eipId:{}", eipId);
                }
            }
            if (failedIds.isEmpty()) {
                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
            } else {
                errorMsg = failedIds.toString();
                log.error(errorMsg);
            }
        } catch (Exception e) {
            log.error("Exception in deleteEipList", e);
            errorMsg = e.getMessage();
        }
        return new ResponseEntity<>(
                ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, errorMsg),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }


    public ResponseEntity renewEip(String eipId, EipAllocateParam eipUpdateInfo) {
        String msg = "";
        String code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;

        try {

            String addTime = eipUpdateInfo.getDuration();
            if (null == addTime) {
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.BAD_REQUEST);
            } else if (addTime.trim().equals("0")) {

                ActionResponse actionResponse = eipDaoService.softDownEip(eipId);
                if (actionResponse.isSuccess()) {
                    return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
                } else if (actionResponse.getCode() == HttpStatus.NOT_FOUND.value()) {
                    return new ResponseEntity<>(ReturnMsgUtil.error(
                            String.valueOf(actionResponse.getCode()), actionResponse.getFault()),
                            HttpStatus.NOT_FOUND);
                }
            } else {
                ActionResponse actionResponse = eipDaoService.reNewEipEntity(eipId, addTime);
                if (actionResponse.isSuccess()) {
                    log.info("renew eip:{} , add duration:{}", eipId, addTime);

                    return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
                } else {
                    msg = actionResponse.getFault();
                    log.error(msg);
                }
            }
        } catch (Exception e) {
            log.error("Exception in deleteEip", e);
            msg = e.getMessage() + "";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * listShareBandWidth the eip
     *
     * @param currentPage the current page
     * @param limit       element of per page
     * @return result
     */
    @Override
    public ResponseEntity listEips(int currentPage, int limit, String status) {

        try {
            String projcectid = CommonUtil.getUserId();
            log.debug("listEips  of user, userId:{}", projcectid);
            if (projcectid == null) {
                return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data = new JSONObject();
            JSONArray eips = new JSONArray();
            if (currentPage != 0) {
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable = PageRequest.of(currentPage - 1, limit, sort);
                Page<Eip> page = eipRepository.findByUserIdAndIsDelete(projcectid, 0, pageable);
                for (Eip eip : page.getContent()) {
                    if ((null != status) && (!eip.getStatus().trim().equalsIgnoreCase(status))) {
                        continue;
                    }
                    EipReturnDetail eipReturnDetail = new EipReturnDetail();
                    BeanUtils.copyProperties(eip, eipReturnDetail);
                    eipReturnDetail.setResourceset(Resourceset.builder()
                            .resourceid(eip.getInstanceId())
                            .resourcetype(eip.getInstanceType()).build());
                    if (StringUtils.isNotBlank(eip.getEipV6Id())) {
                        EipV6 eipV6 = eipV6Service.findEipV6ByEipV6Id(eip.getEipV6Id());
                        if (eipV6 != null) {
                            eipReturnDetail.setIpv6(eipV6.getIpv6());
                        }
                    }
                    eips.add(eipReturnDetail);
                }
                data.put("eips", eips);
                data.put("totalPages", page.getTotalPages());
                data.put("totalElements", page.getTotalElements());
                data.put("currentPage", currentPage);
                data.put("currentPagePer", limit);
            } else {
                List<Eip> eipList = eipDaoService.findByUserId(projcectid);
                for (Eip eip : eipList) {
                    if ((null != status) && (!eip.getStatus().trim().equalsIgnoreCase(status))) {
                        continue;
                    }
                    EipReturnDetail eipReturnDetail = new EipReturnDetail();
                    BeanUtils.copyProperties(eip, eipReturnDetail);
                    eipReturnDetail.setResourceset(Resourceset.builder()
                            .resourceid(eip.getInstanceId())
                            .resourcetype(eip.getInstanceType()).build());
                    if (StringUtils.isNotBlank(eip.getEipV6Id())) {
                        EipV6 eipV6 = eipV6Service.findEipV6ByEipV6Id(eip.getEipV6Id());
                        if (eipV6 != null) {
                            eipReturnDetail.setIpv6(eipV6.getIpv6());
                        }
                    }
                    eips.add(eipReturnDetail);
                }
                data.put("eips", eips);
                data.put("totalPages", 1);
                data.put("totalElements", eips.size());
                data.put("currentPage", 1);
                data.put("currentPagePer", eips.size());
            }
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (KeycloakTokenException e) {
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN, e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Exception in listEips", e);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * get detail of the eip
     *
     * @param eipId the id of the eip instance
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
                if (StringUtils.isNotBlank(eipEntity.getEipV6Id())) {
                    EipV6 eipV6 = eipV6Service.findEipV6ByEipV6Id(eipEntity.getEipV6Id());
                    if (eipV6 != null) {
                        eipReturnDetail.setIpv6(eipV6.getIpv6());
                    }
                }
                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "Can not find eip by id:" + eipId + "."),
                        HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Exception in getEipDetail", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * get eip by instance id
     *
     * @param instanceId the instance id
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
                        "can not find instance by this id:" + instanceId + ""),
                        HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            log.error("Exception in getEipByInstanceId", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * get eip by eip
     *
     * @param eip the eip
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
                        "can not find eip by this eip address:" + eip + ""),
                        HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            log.error("Exception in getEipByIpAddress", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * update eip band width
     *
     * @param id    id
     * @param param param
     * @return result
     */
    @Override
    public ResponseEntity updateEipBandWidth(String id, EipUpdateParamWrapper param) {
        String code;
        String msg;
        try {
            MethodReturn result = eipDaoService.updateEipEntity(id, param);
            if (!result.getInnerCode().equals(ReturnStatus.SC_OK)) {
                code = result.getInnerCode();
                int httpResponseCode = result.getHttpCode();
                msg = result.getMessage();
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));
            } else {
                EipReturnDetail eipReturnDetail = new EipReturnDetail();
                Eip eipEntity = (Eip) result.getEip();
                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                        .resourceid(eipEntity.getInstanceId())
                        .resourcetype(eipEntity.getInstanceType()).build());
                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Exception in updateEipBandWidth", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage() + "";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);

    }

    /**
     * eip bind with port
     *
     * @param id       id
     * @param serverId server id
     * @return result
     */
    @Override
    public ResponseEntity eipBindWithInstance(String id, String type, String serverId, String portId, String addrIp) {

        MethodReturn result = null;

        if (StringUtils.isEmpty(serverId)) {
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_PARA_SERVERID_ERROR)), HttpStatus.BAD_REQUEST);
        }

        Eip eipCheck = eipRepository.findByInstanceIdAndIsDelete(serverId, 0);
        if (eipCheck != null) {
            log.error("The binding failed,  the instanceid  has already bind  eip,instanceid", serverId);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.EIP_BIND_HAS_BAND,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_HAS_BAND)), HttpStatus.BAD_REQUEST);
        }

        switch (type) {
            case "1":
                log.debug("bind a server:{} port:{} with eipId:{}", serverId, portId, id);
                // 1：ecs
                if (!StringUtils.isEmpty(portId)) {
                    result = eipDaoService.associateInstanceWithEip(id, serverId, type, portId, null);
                }
                break;
            case "2":
            case "3":
                if (!StringUtils.isEmpty(addrIp)) {
                    result = eipDaoService.associateInstanceWithEip(id, serverId, type, null, addrIp);
                }
                break;
            default:
                log.warn("no support type param： " + type);
                break;
        }


        if (null != result) {
            if (result.getInnerCode().equals(ReturnStatus.SC_OK)) {
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_OK, "success"), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(ReturnMsgUtil.error(result.getInnerCode(), result.getMessage()), HttpStatus.valueOf(result.getHttpCode()));
            }
        }
        String msg = "Can not get bind responds when bind eip with server" + serverId;
        log.error(msg);
        return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Override
    public ResponseEntity eipUnbindWithInstacnce(String eipId, String instanceId) {
        String code = ReturnStatus.SC_PARAM_ERROR;
        String msg = "param error";
        ActionResponse actionResponse = null;
        Eip eipEntity = null;
        try {
            if (!StringUtils.isEmpty(eipId)) {
                eipEntity = eipDaoService.getEipById(eipId);
            } else if (!StringUtils.isEmpty(instanceId)) {
                eipEntity = eipRepository.findByInstanceIdAndIsDelete(instanceId, 0);
            }
            if (null != eipEntity) {
                String instanceType = eipEntity.getInstanceType();
                if (null != instanceType) {
                    switch (instanceType) {
                        case "1":
                            // 1：ecs
                            actionResponse = eipDaoService.disassociateInstanceWithEip(eipEntity);
                            break;
                        case "2":
                        case "3":
                            actionResponse = eipDaoService.disassociateInstanceWithEip(eipEntity);
                            break;
                        default:
                            //default ecs
                            code = ReturnStatus.SC_PARAM_ERROR;
                            msg = "no support instance type " + instanceType;
                            break;
                    }
                } else {
                    code = ReturnStatus.SC_RESOURCE_ERROR;
                    msg = "Failed to get instance type.";
                }
            } else {
                code = ReturnStatus.SC_NOT_FOUND;
                msg = "can not find eip id ：" + eipId;
            }
        } catch (Exception e) {
            log.error("Exception in unBindPort", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage() + "";
        }
        if (actionResponse != null) {
            if (actionResponse.isSuccess()) {
                code = ReturnStatus.SC_OK;
                msg = ("unbind successfully");
                log.info(code);
                log.info(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.OK);
            } else {
                code = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
                msg = actionResponse.getFault();
                log.error(code);
                log.error(msg);
            }
        }
        log.error(msg);
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Override
    public ResponseEntity getEipCount() {
        try {
            String userId = CommonUtil.getUserId();
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK, "get instance_num_success", eipDaoService.getInstanceNum(userId)), HttpStatus.OK);
        } catch (KeycloakTokenException e) {
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_FORBIDDEN, e.getMessage(), null), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public ResponseEntity getEipStatistics() {
        JSONObject data=new JSONObject();
        try {
            int  freeCount = eipDaoService.getFreeEipCount();
            int  usingCount = eipDaoService.getUsingEipCount();
            int errorCount = eipDaoService.getUsingEipCountByStatus("ERROR");
            int totalBandWidth = eipDaoService.getTotalBandWidth();
            data.put("freeEip", freeCount);
            data.put("errorEip", errorCount);
            data.put("usingEip", usingCount);
            data.put("totalEip", freeCount+usingCount);
            data.put("totalBandWidth", totalBandWidth);
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK, "get statistics success", data), HttpStatus.OK);
        } catch (Exception e) {
            log.info("error", e);
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getFreeEipCount() {
        try {
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK, "get free_eip_num_success", eipDaoService.getFreeEipCount()), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity getUsingEipCount() {
        try {
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK, "get using_eip_num_success", eipDaoService.getUsingEipCount()), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity getUsingEipCountByStatus(String status) {
        try {
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK, "get using_eip_num_success", eipDaoService.getUsingEipCountByStatus(status)), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity getTotalEipCount() {
        try {
            int usingEipCount = eipDaoService.getUsingEipCount();
            int freeEipCount = eipDaoService.getFreeEipCount();
            int totalEipCount = usingEipCount + freeEipCount;
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_OK, "get total_eip_num_success", totalEipCount), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(ReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @ICPServiceLog
    @Override
    public ResponseEntity addEipToSbw(String eipId, EipUpdateParam eipUpdateParam) {
        String code;
        String msg;
        MethodReturn result;
        try {
            result = sbwDaoService.addEipIntoSbw(eipId, eipUpdateParam);
            if (!result.getInnerCode().equals(ReturnStatus.SC_OK)) {
                msg = result.getMessage();
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(result.getInnerCode(), msg),
                        HttpStatus.valueOf(result.getHttpCode()));
            } else {
                msg = "The Eip add to shared band succeeded";
                log.info(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_OK, msg), HttpStatus.OK);
            }

        } catch (Exception e) {
            log.error("eip add to shared band exception", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage() + "";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Override
    public ResponseEntity removeEipFromSbw(String eipId, EipUpdateParam eipUpdateParam) {
        String code;
        String msg;

        try {
            ActionResponse actionResponse = sbwDaoService.removeEipFromSbw(eipId, eipUpdateParam);
            if (actionResponse.isSuccess()) {
                code = ReturnStatus.SC_OK;
                msg = ("remove from shared successfully");
                log.info(code + ":" + msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.OK);
            } else {
                code = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
                msg = actionResponse.getFault();
                log.info(code + ":" + msg);
            }
        } catch (Exception e) {
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage() + "";
            log.error("code:{}, msg:{},remove from sbw  exception", code, msg, e);
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * the eipV6
     *
     * @return result
     */
    @Override
    public ResponseEntity listEipsByBandWidth(String status) {

        try {
            String userId = CommonUtil.getUserId();
            log.debug("listEips  of user, userId:{}", userId);
            if (userId == null) {
                return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data = new JSONObject();
            JSONArray eips = new JSONArray();
            ArrayList<Eip> newList = new ArrayList();
            ArrayList<Eip> newEipList = new ArrayList();
            List<Eip> eipList = eipDaoService.findByUserId(userId);
            for (Eip eip : eipList) {
                String eipAddress = eip.getEipAddress();
                EipV6 eipV6 = eipV6Repository.findByIpv4AndUserIdAndIsDelete(eipAddress, userId, 0);
                if (eipV6 == null) {
                    newEipList.add(eip);
                }
            }
            for (Eip eip : newEipList) {
                if ((null != status) && (!eip.getStatus().trim().equalsIgnoreCase(status))) {
                    continue;
                }
                if (eip.getBandWidth() <= 10) {
                    if (!StringUtils.isNotEmpty(eip.getSbwId())) {
                        EipReturnByBandWidth eipReturnDetail = new EipReturnByBandWidth();
                        BeanUtils.copyProperties(eip, eipReturnDetail);
                        eips.add(eipReturnDetail);
                        data.put("eip", eips);
                        newList.add(eip);
                        data.put("totalElements", newList.size());
                    }
                }
            }
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (KeycloakTokenException e) {
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN, e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Exception in listEips", e);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
    @Override
    public ResponseEntity getEipDetailsByIpAddress(String eipAddress) {
        JSONObject data=new JSONObject();
        JSONArray eips=new JSONArray();
        if(eipAddress!=null){
            Eip eip = eipRepository.findByEipAddressAndIsDelete(eipAddress, 0);
            if(eip == null){
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "can not find eip by this eip address:" + eipAddress+""),
                        HttpStatus.NOT_FOUND);
            }
            EipReturnUserDetail eipReturnUserDetail = new EipReturnUserDetail();
            BeanUtils.copyProperties(eip, eipReturnUserDetail);
            eips.add(eipReturnUserDetail);
            data.put("eips",eips);
        }else{
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                    "eipaddress cannot be null:" + eipAddress+""),
                    HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

}
