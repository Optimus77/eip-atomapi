package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.MethodReturn;
import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.entity.MethodSbwReturn;
import com.inspur.eipatomapi.entity.eip.EipUpdateParam;
import com.inspur.eipatomapi.entity.fw.Firewall;
import com.inspur.eipatomapi.entity.sbw.Sbw;
import com.inspur.eipatomapi.entity.sbw.SbwAllocateParam;
import com.inspur.eipatomapi.entity.sbw.SbwUpdateParam;
import com.inspur.eipatomapi.entity.sbw.SbwUpdateParamWrapper;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.repository.SbwRepository;
import com.inspur.eipatomapi.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.openstack4j.model.common.ActionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
public class SbwDaoService {
    @Autowired
    private SbwRepository sbwRepository;

    @Autowired
    private EipRepository eipRepository;

    @Autowired
    private FirewallService firewallService;

    @Autowired
    private FirewallRepository firewallRepository;

    @Autowired
    private QosService qosService;

    public List<Sbw> findByProjectId(String projectId) {
        return sbwRepository.findByProjectIdAndIsDelete(projectId, 0);
    }

    @Transactional
    public Sbw allocateSbw(SbwAllocateParam sbwConfig)  {
        Sbw sbwMo = null;
        try {
            String userId = CommonUtil.getUserId();

            sbwMo = new Sbw();
            sbwMo.setRegion(sbwConfig.getRegion());
            sbwMo.setSharedbandwidthName(sbwConfig.getSbwName());
            sbwMo.setBandWidth(sbwConfig.getBandwidth());
            sbwMo.setBillType(sbwConfig.getBillType());
            sbwMo.setDuration(sbwConfig.getDuration());
            sbwMo.setProjectId(userId);
            sbwMo.setProjectName(CommonUtil.getProjectName());
            sbwMo.setIsDelete(0);
            sbwMo.setCreateTime(CommonUtil.getGmtDate());
            Sbw sbw = sbwRepository.saveAndFlush(sbwMo);

            Firewall firewall = firewallRepository.findFirewallByRegion(sbwConfig.getRegion());

            String pipeId = firewallService.addQos(null, sbw.getSbwId(), String.valueOf(sbw.getBandWidth()), firewall.getId());
            if (StringUtils.isNotBlank(pipeId)) {
                sbwMo.setPipeId(pipeId);
                sbwRepository.saveAndFlush(sbwMo);
            } else {
                sbwRepository.deleteById(sbw.getSbwId());
                log.info("Failed to CreateQos in FireWall,pipe create failure");
            }
            log.info("User:{} success allocate sbwId:{} ,sbw:{}", userId, sbw.getSbwId(), sbw.toString());
        } catch (KeycloakTokenException e) {
            log.error("KeycloakTokenException", e);
        } catch (Exception e) {
            log.error("Exception", e);
        }
        return sbwMo;
    }

    public Sbw getSbwById(String id) {

        Sbw sbwEntity = null;
        Optional<Sbw> sbw = sbwRepository.findById(id);
        if (sbw.isPresent()) {
            sbwEntity = sbw.get();
        }
        return sbwEntity;
    }

    /**
     * delete
     *
     * @param sbwId id
     * @return ret
     */
    @Transactional
    public ActionResponse deleteSbw(String sbwId) {
        String msg;
        long ipCount;
        Sbw sbwEntity = sbwRepository.findBySbwId(sbwId);
        if (null == sbwEntity) {
            msg = "Faild to find sbw by id:" + sbwId;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        if (!CommonUtil.isAuthoried(sbwEntity.getProjectId())) {
            log.error(CodeInfo.getCodeMessage(CodeInfo.SBW_FORBIDDEN), sbwId);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }
        if (null != sbwEntity.getBillType() && !sbwEntity.getBillType().equalsIgnoreCase(HsConstants.HOURLYSETTLEMENT)) {
            msg = "Only hourlysettlement is allowed for billType";
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
        }
        ipCount = eipRepository.countBySharedBandWidthIdAndIsDelete(sbwEntity.getSbwId(), 0);
        if (ipCount != 0) {
            msg = "EIP in sbw so that sbw cannot be removed ，please remove first !,ipCount:{}" + ipCount;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
        }
        Firewall firewall = firewallRepository.findFirewallByRegion(sbwEntity.getRegion());
        if (sbwEntity.getPipeId() == null || "".equals(sbwEntity.getPipeId())) {
            sbwEntity.setIsDelete(1);
            sbwEntity.setUpdateTime(CommonUtil.getGmtDate());
            sbwRepository.saveAndFlush(sbwEntity);
            return ActionResponse.actionSuccess();
        }
        boolean delQos = firewallService.delQos(sbwEntity.getPipeId(), firewall.getId());
        if (delQos) {
            sbwEntity.setIsDelete(1);
            sbwEntity.setUpdateTime(CommonUtil.getGmtDate());
            sbwEntity.setPipeId(null);
            sbwRepository.saveAndFlush(sbwEntity);
            return ActionResponse.actionSuccess();
        }
        return ActionResponse.actionFailed(CodeInfo.SBW_DELETE_ERROR, HttpStatus.SC_FORBIDDEN);
    }


    @Transactional
    public ActionResponse softDownSbw(String sbwId) {
        String msg;
        Sbw sbw = sbwRepository.findBySbwId(sbwId);
        if (null == sbw) {
            msg = "Faild to find sbw by id:" + sbwId + " ";
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        if (!CommonUtil.isAuthoried(sbw.getProjectId())) {
            log.error(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), sbwId);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }
        Firewall firewall = firewallRepository.findFirewallByRegion(sbw.getRegion());
        if (firewall == null){
            msg = "Can't find firewall by sbw region:{}"+ sbw.getRegion();
            log.error("Can't find firewall by sbw region:{}, sbwId:{}" + sbw.getRegion() +sbw.getSbwId() );
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        MethodReturn methodReturn = qosService.controlPipe(firewall.getId(), sbwId, true);
        if (methodReturn.getHttpCode() == HttpStatus.SC_OK){
            sbw.setUpdateTime(CommonUtil.getGmtDate());
            sbw.setStatus("STOP");
            sbwRepository.saveAndFlush(sbw);
            return ActionResponse.actionSuccess();
        }else {
            return ActionResponse.actionFailed(methodReturn.getMessage(), methodReturn.getHttpCode());
        }
    }

    @Transactional
    public ActionResponse reNewSbwEntity(String sbwId, String renewTime) {
        String msg;
        Sbw sbw = sbwRepository.findBySbwId(sbwId);
        if (null == sbw) {
            return ActionResponse.actionFailed("Can not find the sbw by id:{}" + sbwId, HttpStatus.SC_NOT_FOUND);
        }
        if (!sbw.getBillType().equals(HsConstants.MONTHLY)) {
            return ActionResponse.actionFailed("Non - packet year - and - month Shared bandWidth cannot be renewed:{}" + sbwId, HttpStatus.SC_NOT_FOUND);
        }
        Firewall firewall = firewallRepository.findFirewallByRegion(sbw.getRegion());
        if (firewall == null){
            msg = "Can't find firewall by sbw region:{}"+ sbw.getRegion();
            log.error("Can't find firewall by sbw region:{}, sbwId:{}" + sbw.getRegion() +sbw.getSbwId() );
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        String oldTime = sbw.getDuration();
        int newTime = Integer.valueOf(renewTime) + Integer.valueOf(oldTime);
        sbw.setDuration(String.valueOf(newTime));
        MethodReturn methodReturn = qosService.controlPipe(firewall.getId(), sbwId, false);
        if (methodReturn.getHttpCode() == HttpStatus.SC_OK){
            sbw.setUpdateTime(CommonUtil.getGmtDate());
            sbw.setStatus("ACTIVE");
            sbwRepository.saveAndFlush(sbw);
            return ActionResponse.actionSuccess();
        }else {
            return ActionResponse.actionFailed(methodReturn.getMessage(), methodReturn.getHttpCode());
        }
    }

    @Transactional
    public JSONObject renameSbw(String sbwId, SbwUpdateParam param) {
        JSONObject data = new JSONObject();
        String newSbwName = param.getSbwName();
        Sbw sbw = sbwRepository.findBySbwId(sbwId);
        if (null == sbw) {
            log.error("In rename sbw process,failed to find the sbw by id:{} ", sbwId);
            data.put(HsConstants.REASON, CodeInfo.getCodeMessage(CodeInfo.SBW_NOT_FOND_BY_ID));
            data.put(HsConstants.HTTP_CODE, HttpStatus.SC_NOT_FOUND);
            data.put(HsConstants.INTER_CODE, ReturnStatus.SC_NOT_FOUND);
            return data;
        }
        if (!CommonUtil.isAuthoried(sbw.getProjectId())) {
            log.error("User have no write to operate sbw:{}", sbwId);
            data.put(HsConstants.REASON, CodeInfo.getCodeMessage(CodeInfo.SBW_FORBIDDEN));
            data.put(HsConstants.HTTP_CODE, HttpStatus.SC_FORBIDDEN);
            data.put(HsConstants.INTER_CODE, ReturnStatus.SC_FORBIDDEN);
            return data;
        }
        sbw.setSharedbandwidthName(newSbwName);
        sbw.setUpdateTime(CommonUtil.getGmtDate());
        sbwRepository.saveAndFlush(sbw);
        data.put(HsConstants.REASON, "");
        data.put(HsConstants.HTTP_CODE, HttpStatus.SC_OK);
        data.put(HsConstants.INTER_CODE, ReturnStatus.SC_OK);
        data.put("data", sbw);
        return data;
    }

    @Transactional
    public MethodSbwReturn updateSbwEntity(String sbwId, SbwUpdateParam param) {

        Sbw sbwEntity = sbwRepository.findBySbwId(sbwId);
        if (null == sbwEntity) {
            log.error("In update sbw bandWidth  process,failed to find the sbw by id:{} ", sbwId);
            return MethodReturnUtil.errorSbw(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_NOT_FOND_BY_ID));
        }
        if (!CommonUtil.isAuthoried(sbwEntity.getProjectId())) {
            log.error("User  not have permission to update sbw bandWidth sbwId:{}", sbwId);
            return MethodReturnUtil.errorSbw(HttpStatus.SC_FORBIDDEN, ReturnStatus.SC_FORBIDDEN,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_FORBIDDEN));
        }
        if (param.getBillType().equals(HsConstants.MONTHLY) && param.getBandWidth() < sbwEntity.getBandWidth()) {
            //can’t  modify
            return MethodReturnUtil.errorSbw(HttpStatus.SC_BAD_REQUEST, ReturnStatus.SC_PARAM_ERROR,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_THE_NEW_BANDWIDTH_VALUE_ERROR));
        }
        if ( sbwEntity.getPipeId() ==null) {
            sbwEntity.setBandWidth(param.getBandWidth());
            sbwEntity.setBillType(param.getBillType());
            sbwEntity.setUpdateTime(CommonUtil.getGmtDate());
            sbwRepository.saveAndFlush(sbwEntity);
            return MethodReturnUtil.successSbw(sbwEntity);
        }
        Firewall firewall = firewallRepository.findFirewallByRegion(sbwEntity.getRegion());
        boolean updateStatus = firewallService.updateQosBandWidth(firewall.getId(), sbwEntity.getPipeId(), sbwEntity.getSbwId(), String.valueOf(param.getBandWidth()));
        if (updateStatus || CommonUtil.qosDebug) {
            sbwEntity.setBandWidth(param.getBandWidth());
            sbwEntity.setBillType(param.getBillType());
            sbwEntity.setUpdateTime(CommonUtil.getGmtDate());
            sbwRepository.saveAndFlush(sbwEntity);

            Stream<Eip> stream = eipRepository.findByUserIdAndIsDeleteAndSharedBandWidthId(sbwEntity.getProjectId(), 0, sbwId).stream();
            stream.forEach(eip -> {
                eip.setBandWidth(param.getBandWidth());
                eip.setUpdateTime(CommonUtil.getGmtDate());
                eipRepository.saveAndFlush(eip);
            });
            return MethodReturnUtil.successSbw(sbwEntity);
        }
        return MethodReturnUtil.errorSbw(HttpStatus.SC_INTERNAL_SERVER_ERROR, ReturnStatus.SC_FIREWALL_SERVER_ERROR,
                CodeInfo.getCodeMessage(CodeInfo.SBW_CHANGE_BANDWIDTH_ERROR));

    }

    @Transactional
    public MethodReturn addEipIntoSbw(String eipid, EipUpdateParam eipUpdateParam) {


        String sbwId = eipUpdateParam.getSharedBandWidthId();
        Eip eipEntity = eipRepository.findByEipId(eipid);
        String pipeId;
        if (null == eipEntity) {
            log.error("In addEipIntoSbw process,failed to find the eip by id:{} ", eipid);
            return MethodReturnUtil.error(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
        }
        if(StringUtils.isNotBlank(eipEntity.getEipV6Id())){
            log.error("EIP is already bound to eipv6");
            return MethodReturnUtil.error(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_EIPV6_ERROR));
        }
        if (!CommonUtil.isAuthoried(eipEntity.getUserId())) {
            log.error("User have no write to operate eip:{}", eipid);
            return MethodReturnUtil.error(HttpStatus.SC_FORBIDDEN, ReturnStatus.SC_FORBIDDEN,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
        }
        //1.ensure eip is billed on hourlySettlement
        if (eipEntity.getBillType().equals(HsConstants.MONTHLY)) {
            log.error("The eip billType isn't hourlySettment!", eipEntity.getBillType());
            return MethodReturnUtil.error(HttpStatus.SC_BAD_REQUEST, ReturnStatus.SC_PARAM_ERROR,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BILLTYPE_NOT_HOURLYSETTLEMENT));
        }
        //3.check eip had not adding any Shared bandWidth
        if ((null != eipEntity.getSharedBandWidthId() && !"".equals(eipEntity.getSharedBandWidthId().trim()))) {
            log.error("The shared band id not null, this mean the eip had already added other SBW !", eipEntity.getSharedBandWidthId());
            return MethodReturnUtil.error(HttpStatus.SC_BAD_REQUEST, ReturnStatus.SC_PARAM_ERROR,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_SHARED_BAND_WIDTH_ID_NOT_NULL));
        }
        Sbw sbwEntiy = sbwRepository.findBySbwId(sbwId);
        if (null == sbwEntiy) {
            log.error("Failed to find sbw by id:{} ", sbwId);
            return MethodReturnUtil.error(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_NOT_FOND_BY_ID));
        }
        boolean updateStatus = true;
        if (eipEntity.getStatus().equalsIgnoreCase(HsConstants.ACTIVE)) {
            log.info("FirewallId: " + eipEntity.getFirewallId() + " FloatingIp: " + eipEntity.getFloatingIp() + " ShardBandId: " + sbwId);
            pipeId = firewallService.addFloatingIPtoQos(eipEntity.getFirewallId(), eipEntity.getFloatingIp(), sbwEntiy.getPipeId(), sbwId, eipUpdateParam.getBandWidth());
            if (null != pipeId) {
                updateStatus = firewallService.delQos(eipEntity.getPipId(), eipEntity.getFirewallId());
                if (sbwEntiy.getPipeId() == null || sbwEntiy.getPipeId().isEmpty()) {
                    sbwEntiy.setPipeId(pipeId);
                }
            } else {
                updateStatus = false;
            }
        }

        if (updateStatus || CommonUtil.qosDebug) {
            eipEntity.setPipId(sbwEntiy.getPipeId());
            eipEntity.setUpdateTime(new Date());
            eipEntity.setSharedBandWidthId(sbwId);
            eipEntity.setOldBandWidth(eipEntity.getBandWidth());
            eipEntity.setChargeMode(HsConstants.SHAREDBANDWIDTH);
            eipEntity.setBandWidth(eipUpdateParam.getBandWidth());
            eipRepository.saveAndFlush(eipEntity);

            sbwEntiy.setUpdateTime(new Date());
            sbwRepository.saveAndFlush(sbwEntiy);

            return MethodReturnUtil.success(eipEntity);
        }

        return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, ReturnStatus.SC_FIREWALL_SERVER_ERROR,
                CodeInfo.getCodeMessage(CodeInfo.EIP_CHANGE_BANDWIDTH_ERROR));

    }

    @Transactional
    public ActionResponse removeEipFromSbw(String eipid, EipUpdateParam eipUpdateParam) {
        Eip eipEntity = eipRepository.findByEipId(eipid);
        String msg;
        String sbwId = eipUpdateParam.getSharedBandWidthId();
        Sbw sbw = sbwRepository.findBySbwId(sbwId);
        if (null == sbw) {
            log.error("In removeEipFromSbw process,failed to find sbw by id:{} ", sbwId);
            return ActionResponse.actionFailed("Eip Not found.", HttpStatus.SC_NOT_FOUND);
        }
        if (null == eipEntity) {
            log.error("In removeEipFromSbw process,failed to find the eip by id:{} ", eipid);
            return ActionResponse.actionFailed("Eip Not found.", HttpStatus.SC_NOT_FOUND);
        }

        if (!CommonUtil.isAuthoried(eipEntity.getUserId())) {
            log.error("User have no write to delete eip:{}", eipid);
            return ActionResponse.actionFailed("Forbiden.", HttpStatus.SC_FORBIDDEN);
        }
        boolean removeStatus = true;
        String newPipId = null;
        if (eipEntity.getStatus().equalsIgnoreCase(HsConstants.ACTIVE)) {
            log.info("FirewallId: " + eipEntity.getFirewallId() + " FloatingIp: " + eipEntity.getFloatingIp() + " ShardBandId: " + sbwId);
            newPipId = firewallService.addQos(eipEntity.getFloatingIp(), eipEntity.getEipAddress(), String.valueOf(eipUpdateParam.getBandWidth()),
                    eipEntity.getFirewallId());
            if (null != newPipId) {
                removeStatus = firewallService.removeFloatingIpFromQos(eipEntity.getFirewallId(), eipEntity.getFloatingIp(), eipEntity.getPipId(), sbwId);
            } else {
                removeStatus = false;
            }
        }

        if (removeStatus || CommonUtil.qosDebug) {
            eipEntity.setUpdateTime(new Date());
            //update the eip table
            eipEntity.setPipId(newPipId);
            eipEntity.setSharedBandWidthId(null);
            eipEntity.setBandWidth(eipUpdateParam.getBandWidth());
            eipEntity.setChargeMode(HsConstants.BANDWIDTH);
            eipRepository.saveAndFlush(eipEntity);

            sbw.setUpdateTime(new Date());
            sbwRepository.saveAndFlush(sbw);
            return ActionResponse.actionSuccess();
        }

        msg = "Failed to remove ip in sharedBand,eipId:" + eipEntity.getEipId() + " sharedBandWidthId:" + sbwId + "";
        log.error(msg);
        return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);

    }
    @Transactional
    public Page<Sbw> findByIdAndIsDelete(String sbwId, String userId, int isDelete, Pageable pageable){
        return sbwRepository.findBySbwIdAndProjectIdAndIsDelete(sbwId, userId, 0, pageable );
    }

    @Transactional
    public Page<Sbw> findByIsDeleteAndSbwName(String userId, int isDelete, String name, Pageable pageable){
        return sbwRepository.findByProjectIdAndIsDeleteAndSharedbandwidthNameContaining(userId, 0, name, pageable);
    }
    @Transactional
    public Page<Sbw> findByIsDelete(String userId, int isDelte, Pageable pageable){
        return sbwRepository.findByProjectIdAndIsDelete(userId ,0, pageable);
    }



}
