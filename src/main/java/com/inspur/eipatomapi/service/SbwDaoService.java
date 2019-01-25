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
import com.inspur.eipatomapi.entity.sbw.SbwUpdateParamWrapper;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.repository.SbwRepository;
import com.inspur.eipatomapi.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.openstack4j.model.common.ActionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class SbwDaoService {
    @Autowired
    private SbwRepository sbwRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EipRepository eipRepository;

    @Autowired
    private FirewallService firewallService;

    @Autowired
    private FirewallRepository firewallRepository;

    public List<Sbw> findByProjectId(String projectId) {
        return sbwRepository.findByProjectIdAndIsDelete(projectId, 0);
    }

    @Transactional
    public Sbw allocateSbw(SbwAllocateParam sbwConfig) throws KeycloakTokenException {
        String userId =  CommonUtil.getUserId();

        Sbw sbwMo = new Sbw();
        sbwMo.setRegion(sbwConfig.getRegion());
        sbwMo.setSharedbandwidthName(sbwConfig.getSbwName());
        sbwMo.setBandWidth(sbwConfig.getBandwidth());
        sbwMo.setBillType(sbwConfig.getBillType());
        sbwMo.setChargeMode(sbwConfig.getChargemode());
        sbwMo.setDuration(sbwConfig.getDuration());
        sbwMo.setDurationUnit(sbwConfig.getDurationUnit());
        sbwMo.setProjectId(userId);
        sbwMo.setIsDelete(0);
        sbwMo.setCreateTime(CommonUtil.getGmtDate());

        Sbw sbw = sbwRepository.saveAndFlush(sbwMo);
        Firewall firewall=firewallRepository.findFirewallByRegion(sbwConfig.getRegion());

        String pipeId = firewallService.addQos(null, sbw.getSbwId(), String.valueOf(sbw.getBandWidth()), firewall.getId());
        sbwMo.setPipeId(pipeId);
        sbwRepository.saveAndFlush(sbwMo);
        log.info("User:{} success allocate sbwId:{} ,sbw:{}", userId, sbw.getSbwId(), sbw.toString());
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
        String msg ;
        long ipCount ;
        Sbw sbwEntity = sbwRepository.findBySbwId(sbwId);
        if (null == sbwEntity) {
            msg = "Faild to find sbw by id:" + sbwId;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        if (!CommonUtil.isAuthoried(sbwEntity.getProjectId())) {
            log.error(CodeInfo.getCodeMessage(CodeInfo.SBW_FORBIDEN_WITH_ID), sbwId);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }
        if (null != sbwEntity.getChargeMode()&&!sbwEntity.getChargeMode().equalsIgnoreCase(HsConstants.SHAREDBANDWIDTH)) {
                msg = "Only Sharedbandwidth is allowed for chargeMode";
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
        }
        if (null != sbwEntity.getBillType()&&!sbwEntity.getBillType().equalsIgnoreCase(HsConstants.HOURLYSETTLEMENT)) {
                msg = "Only hourlysettlement is allowed for billType";
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
        }
        ipCount = eipRepository.countBySharedBandWidthIdAndIsDelete(sbwEntity.getSharedbandwidthName(), 0);
        if (ipCount != 0 ) {
            msg = "EIP in sbw so that sbw cannot be removed ，please remove first !,ipCount:{}" + ipCount;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
        }
        Firewall firewall = firewallRepository.findFirewallByRegion(sbwEntity.getRegion());
        if (sbwEntity.getPipeId() ==null ||"".equals(sbwEntity.getPipeId())){
            sbwEntity.setIsDelete(1);
            sbwEntity.setUpdateTime(CommonUtil.getGmtDate());
            sbwRepository.saveAndFlush(sbwEntity);
            return  ActionResponse.actionSuccess();
        }
        boolean delQos = firewallService.delQos(sbwEntity.getPipeId(), firewall.getId());
        if (delQos){
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
        sbw.setUpdateTime(CommonUtil.getGmtDate());
        sbwRepository.saveAndFlush(sbw);
        return ActionResponse.actionSuccess();
    }

    @Transactional
    public ActionResponse reNewSbwEntity(String sbwId, String renewTime) {

        Sbw sbw = sbwRepository.findBySbwId(sbwId);
        if (null == sbw) {
            return ActionResponse.actionFailed("Can not find the sbw by id:{}" + sbwId, HttpStatus.SC_NOT_FOUND);
        }
        String oldTime = sbw.getDuration();
        int newTime = Integer.valueOf(renewTime) + Integer.valueOf(oldTime);
        sbw.setDuration(String.valueOf(newTime));
        if ((newTime > 0)) {
            sbw.setUpdateTime(CommonUtil.getGmtDate());
        }
        sbwRepository.saveAndFlush(sbw);
        return ActionResponse.actionSuccess();
    }

    @Transactional
    public JSONObject renameSbw(String sbwId, SbwUpdateParamWrapper wrapper) {
        JSONObject data = new JSONObject();
        String newSbwName = wrapper.getSbwUpdateParam().getSbwName();
        Sbw sbw = sbwRepository.findBySbwId(sbwId);
        if (null == sbw) {
            log.error("In rename sbw process,failed to find the sbw by id:{} ", sbwId);
            data.put(HsConstants.REASON, CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
            data.put(HsConstants.HTTP_CODE, HttpStatus.SC_NOT_FOUND);
            data.put(HsConstants.INTER_CODE, ReturnStatus.SC_NOT_FOUND);
            return data;
        }
        if (!CommonUtil.isAuthoried(sbw.getProjectId())) {
            log.error("User have no write to operate sbw:{}", sbwId);
            data.put(HsConstants.REASON, CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
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
    public MethodSbwReturn updateSbwEntity(String sbwid, SbwUpdateParamWrapper param) {

        Sbw sbwEntity = sbwRepository.findBySbwId(sbwid);
        long count = eipRepository.countBySharedBandWidthIdAndIsDelete(sbwid ,0);
        if (null == sbwEntity) {
            log.error("In update sbw width  process,failed to find the sbw by id:{} ", sbwid);
            return MethodReturnUtil.errorSbw(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_NOT_FOND_BY_ID));
        }
        if(!CommonUtil.isAuthoried(sbwEntity.getProjectId())){
            log.error("User have no write to operate sbw:{}", sbwid);
            return MethodReturnUtil.errorSbw(HttpStatus.SC_FORBIDDEN, ReturnStatus.SC_FORBIDDEN,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_FORBIDDEN));
        }
        if(param.getSbwUpdateParam().getBillType().equals(HsConstants.MONTHLY)&&param.getSbwUpdateParam().getBandwidth()< sbwEntity.getBandWidth()){
                //can’t  modify
            return MethodReturnUtil.errorSbw(HttpStatus.SC_BAD_REQUEST, ReturnStatus.SC_PARAM_ERROR,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR));
        }
        if (count ==0){
            sbwEntity.setBandWidth(param.getSbwUpdateParam().getBandwidth());
            sbwEntity.setBillType(param.getSbwUpdateParam().getBillType());
            sbwEntity.setUpdateTime(CommonUtil.getGmtDate());
            sbwEntity.setChargeMode(param.getSbwUpdateParam().getChargemode());
            sbwRepository.saveAndFlush(sbwEntity);
            return MethodReturnUtil.successSbw(sbwEntity);
        }
        Firewall firewall = firewallRepository.findFirewallByRegion(param.getSbwUpdateParam().getRegion());
        boolean updateStatus = firewallService.updateQosBandWidth(firewall.getId(),sbwEntity.getPipeId(), sbwEntity.getSbwId(), String.valueOf(param.getSbwUpdateParam().getBandwidth()));
        if (updateStatus ||CommonUtil.qosDebug) {
            sbwEntity.setBandWidth(param.getSbwUpdateParam().getBandwidth());
            sbwEntity.setBillType(param.getSbwUpdateParam().getBillType());
            sbwEntity.setUpdateTime(CommonUtil.getGmtDate());
            sbwEntity.setChargeMode(param.getSbwUpdateParam().getChargemode());
            sbwRepository.saveAndFlush(sbwEntity);
            return MethodReturnUtil.successSbw(sbwEntity);
        }else{
            return MethodReturnUtil.errorSbw(HttpStatus.SC_INTERNAL_SERVER_ERROR, ReturnStatus.SC_FIREWALL_SERVER_ERROR,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_CHANGE_BANDWIDTH_ERROR));
        }
    }

    @Transactional
    public MethodReturn addEipIntoSbw(String eipid, EipUpdateParam eipUpdateParam)  {


        String sbwId = eipUpdateParam.getSharedBandWidthId();
        Eip eipEntity = eipRepository.findByEipId(eipid);
        String pipeId ;
        if (null == eipEntity) {
            log.error("In addEipIntoSbw process,failed to find the eip by id:{} ", eipid);
            return MethodReturnUtil.error(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
        }
        if (!CommonUtil.isAuthoried(eipEntity.getProjectId())) {
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
        //3.check eip had not adding any Shared bandwidth
        if ((null != eipEntity.getSharedBandWidthId() && !"".equals(eipEntity.getSharedBandWidthId().trim())  )){
            log.error("The shared band id not null, this mean the eip had already added other SBW !", eipEntity.getSharedBandWidthId());
            return MethodReturnUtil.error(HttpStatus.SC_BAD_REQUEST, ReturnStatus.SC_PARAM_ERROR,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_SHARED_BAND_WIDTH_ID_NOT_NULL));
        }
        Sbw sbwEntiy = sbwRepository.findBySbwId(sbwId);
        if(null == sbwEntiy){
            log.error("Failed to find sbw by id:{} ", sbwId);
            return MethodReturnUtil.error(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_NOT_FOND_BY_ID));
        }
        boolean updateStatus = true ;
        if (eipEntity.getStatus().equalsIgnoreCase(HsConstants.ACTIVE)){
            log.info("FirewallId: "+eipEntity.getFirewallId()+" FloatingIp: "+eipEntity.getFloatingIp()+" ShardBandId: "+ sbwId);
            pipeId = firewallService.addFloatingIPtoQos(eipEntity.getFirewallId(), eipEntity.getFloatingIp(),sbwEntiy.getPipeId(), sbwId, eipUpdateParam.getBandWidth());
            if(null != pipeId){
                updateStatus = firewallService.delQos(eipEntity.getPipId(), eipEntity.getFirewallId());
                if(sbwEntiy.getPipeId() == null || sbwEntiy.getPipeId().isEmpty()) {
                    sbwEntiy.setPipeId(pipeId);
                }
            }else{
                updateStatus = false;
            }
        }

        if (updateStatus || CommonUtil.qosDebug) {
            eipEntity.setPipId(sbwEntiy.getPipeId());
            eipEntity.setUpdateTime(new Date());
            eipEntity.setSharedBandWidthId(sbwId);
            eipEntity.setOldBandWidth(eipEntity.getBandWidth());
            eipEntity.setChargeMode("SharedBandwidth");
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
    public ActionResponse removeEipFromSbw(String eipid, EipUpdateParam eipUpdateParam)  {
        Eip eipEntity = eipRepository.findByEipId(eipid);
        String msg ;
        String sbwId = eipUpdateParam.getSharedBandWidthId();
        Sbw sbw = sbwRepository.findBySbwId(sbwId);
        if(null == sbw){
            log.error("In removeEipFromSbw process,failed to find sbw by id:{} ", sbwId);
            return ActionResponse.actionFailed("Eip Not found.", HttpStatus.SC_NOT_FOUND);
        }
        if (null == eipEntity) {
            log.error("In removeEipFromSbw process,failed to find the eip by id:{} ", eipid);
            return ActionResponse.actionFailed("Eip Not found.", HttpStatus.SC_NOT_FOUND);
        }

        if (!CommonUtil.isAuthoried(eipEntity.getProjectId())) {
            log.error("User have no write to delete eip:{}", eipid);
            return ActionResponse.actionFailed("Forbiden.", HttpStatus.SC_FORBIDDEN);
        }
        boolean removeStatus =true;
        String newPipId = null;
        if (eipEntity.getStatus().equalsIgnoreCase(HsConstants.ACTIVE)) {
            log.info("FirewallId: "+eipEntity.getFirewallId()+" FloatingIp: "+eipEntity.getFloatingIp()+" ShardBandId: "+ sbwId);
            newPipId = firewallService.addQos(eipEntity.getFloatingIp(), eipEntity.getEipAddress()+"-"+CommonUtil.getToday(), String.valueOf(eipUpdateParam.getBandWidth()),
                    eipEntity.getFirewallId());
            if(null != newPipId) {
                removeStatus = firewallService.removeFloatingIpFromQos(eipEntity.getFirewallId(), eipEntity.getFloatingIp(), eipEntity.getPipId(), sbwId);
            }else {
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


}
