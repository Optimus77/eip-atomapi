package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.entity.sbw.ConsoleCustomization;
import com.inspur.eipatomapi.entity.sbw.Sbw;
import com.inspur.eipatomapi.entity.sbw.SbwAllocateParam;
import com.inspur.eipatomapi.entity.sbw.SbwUpdateParamWrapper;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.SbwRepository;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.HsConstants;
import com.inspur.eipatomapi.util.ReturnStatus;
import com.sun.org.apache.bcel.internal.generic.I2F;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.openstack4j.model.common.ActionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class SbwDaoService {
    @Autowired
    private SbwRepository sbwRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EipRepository eipRepository;

    public List<Sbw> findByProjectId(String projectId) {
        return sbwRepository.findByProjectIdAndIsDelete(projectId, 0);
    }

    @Transactional
    public Sbw allocateSbw(SbwAllocateParam sbwConfig) throws Exception {
        ConsoleCustomization customization = sbwConfig.getConsoleCustomization();

        Sbw sbwMo = new Sbw();
        sbwMo.setRegion(customization.getRegion());
        sbwMo.setSharedbandwidthName(customization.getSharedbandwidthname());
        sbwMo.setBandWidth(customization.getBandwidth());
        sbwMo.setBillType(customization.getBillType());
        sbwMo.setChargeMode(customization.getChargemode());
        sbwMo.setDuration(customization.getDuration());
        sbwMo.setDurationUnit(sbwConfig.getDurationUnit());
        sbwMo.setBandWidth(customization.getBandwidth());
        sbwMo.setRegion(customization.getRegion());
        String userId = CommonUtil.getUserId();
        sbwMo.setProjectId(userId);
        sbwMo.setIsDelete(0);
        sbwMo.setCreateTime(CommonUtil.getGmtDate());
        Sbw sbw = sbwRepository.saveAndFlush(sbwMo);
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
     * @param sbwId
     * @return
     */
    public ActionResponse deleteSbw(String sbwId) {
        String msg;
        int ipCount = 0;
        Sbw entity = sbwRepository.findBySbwId(sbwId);
        if (null == entity) {
            msg = "Faild to find sbw by id:" + sbwId;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        if (!CommonUtil.isAuthoried(entity.getProjectId())) {
            log.error(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), sbwId);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }
        ipCount = entity.getIpCount();
        if (ipCount != 0 || ipCount > 0) {
            msg = "Elastic IP in Shared bandwidth cannot be removed ,ipCount:{}" + ipCount;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
        }
        if (null != entity.getChargeMode()) {
            if (!entity.getChargeMode().equalsIgnoreCase(HsConstants.SHAREDBANDWIDTH)) {
                msg = "Only Shared bandwidth is allowed for chargeMode";
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
            }
        }
        if (null != entity.getBillType()) {
            if (!entity.getBillType().equalsIgnoreCase(HsConstants.HOURLYSETTLEMENT)) {
                msg = "Only hourlysettlement is allowed for billType";
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
            }
        }
        if (null != entity.getPipeId()) {
            msg = "Failed to delete eip,please unbind sbw first." + entity.toString();
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        entity.setIsDelete(1);
        entity.setUpdateTime(CommonUtil.getGmtDate());
        sbwRepository.saveAndFlush(entity);
        //delete the qos

        return ActionResponse.actionSuccess();
    }

    public long getSbwNum(String projectId) {

        //TODO  get table name and colum name by entityUtil
        String sql = "select count(1) as num from eip where project_id='" + projectId + "'";

        Map<String, Object> map = jdbcTemplate.queryForMap(sql);
        long num = (long) map.get("num");
        log.debug("{}, result:{}", sql, num);

        return num;
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
        sbw.setStatus(HsConstants.DOWN);
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
            sbw.setStatus(HsConstants.ACTIVE);
            sbw.setUpdateTime(CommonUtil.getGmtDate());
        }
        sbwRepository.saveAndFlush(sbw);
        return ActionResponse.actionSuccess();
    }

    @Transactional
    public JSONObject renameSbw(String sbwId, SbwUpdateParamWrapper wrapper) {
        JSONObject data = new JSONObject();
        Sbw sbw = sbwRepository.findBySbwId(sbwId);
        if (null == sbw) {
            log.error("In disassociate process,failed to find the sbw by id:{} ", sbwId);
            data.put("reason", CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
            data.put("httpCode", HttpStatus.SC_NOT_FOUND);
            data.put("interCode", ReturnStatus.SC_NOT_FOUND);
            return data;
        }
        if (!CommonUtil.isAuthoried(sbw.getProjectId())) {
            log.error("User have no write to operate sbw:{}", sbwId);
            data.put("reason", CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
            data.put("httpCode", HttpStatus.SC_FORBIDDEN);
            data.put("interCode", ReturnStatus.SC_FORBIDDEN);
            return data;
        }
        //Distinguish between EIP binding and IP unbinding

        sbw.setSharedbandwidthName(wrapper.getSbwUpdateParam().getSbwName());
        if (sbw.getIpCount() != 0) {
        //update qos
            Eip eip = eipRepository.findBySharedBandWidthIdAndIsDelete(sbwId, 0).get(0);
        }
        sbwRepository.saveAndFlush(sbw);
        data.put("reason", "");
        data.put("httpCode", HttpStatus.SC_OK);
        data.put("interCode", ReturnStatus.SC_OK);
        data.put("data", sbw);
        return data;
    }
}
