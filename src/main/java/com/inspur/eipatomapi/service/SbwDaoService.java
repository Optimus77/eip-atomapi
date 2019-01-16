package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.MethodSbwReturn;
import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.entity.fw.Firewall;
import com.inspur.eipatomapi.entity.sbw.Sbw;
import com.inspur.eipatomapi.entity.sbw.SbwAllocateParam;
import com.inspur.eipatomapi.entity.sbw.SbwUpdateParamWrapper;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.repository.SbwRepository;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.HsConstants;
import com.inspur.eipatomapi.util.MethodReturnUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
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

    @Autowired
    private QosService qosService;

    public List<Sbw> findByProjectId(String projectId) {
        return sbwRepository.findByProjectIdAndIsDelete(projectId, 0);
    }

    @Transactional
    public Sbw allocateSbw(SbwAllocateParam sbwConfig) throws Exception{
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
            log.error(CodeInfo.getCodeMessage(CodeInfo.SBW_FORBIDEN_WITH_ID), sbwId);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }
        ipCount = entity.getIpCount();
        if (ipCount != 0 || ipCount > 0) {
            msg = "EIP in sbw so that sbw cannot be removed ,ipCount:{}" + ipCount;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
        }
        if (null != entity.getChargeMode()) {
            if (!entity.getChargeMode().equalsIgnoreCase(HsConstants.SHAREDBANDWIDTH)) {
                msg = "Only Sharedbandwidth is allowed for chargeMode";
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
//        if (null != entity.getPipeId()) {
//            msg = "Failed to delete eip,please unbind sbw first." + entity.toString();
//            log.error(msg);
//            return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
//        }
        entity.setIsDelete(1);
        entity.setUpdateTime(CommonUtil.getGmtDate());
        // todo delete the qos
        String region = entity.getRegion();


        sbwRepository.saveAndFlush(entity);

        return ActionResponse.actionSuccess();
    }

    public long getSbwNum(String projectId) {

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
        String newSbwName = wrapper.getSbwUpdateParam().getSbwName();
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
        if (sbw.getIpCount() != 0 && sbw.getIpCount()> 0) {
        //update qos
            Firewall firewall = firewallRepository.findFirewallByRegion(sbw.getRegion());
            if(firewall ==null){
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.SBW_CHANGE_BANDWIDTH_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_SERVER_ERROR);
               return data;
            }
            boolean updateResource = firewallService.updateQosBandWidth(firewall.getId(), sbw.getPipeId(), newSbwName, String.valueOf(sbw.getBandWidth()));
            if (updateResource ||CommonUtil.qosDebug) {
                sbw.setSharedbandwidthName(newSbwName);
                sbw.setUpdateTime(CommonUtil.getGmtDate());
                sbwRepository.saveAndFlush(sbw);
                data.put("reason","");
                data.put("httpCode", HttpStatus.SC_OK);
                data.put("interCode", ReturnStatus.SC_OK);
                data.put("data",sbw);
                return data;
            }else{
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.SBW_CHANGE_BANDWIDTH_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_SERVER_ERROR);
                return data;
            }
        }
        sbw.setSharedbandwidthName(newSbwName);
        sbw.setUpdateTime(CommonUtil.getGmtDate());
        sbwRepository.saveAndFlush(sbw);
        data.put("reason", "");
        data.put("httpCode", HttpStatus.SC_OK);
        data.put("interCode", ReturnStatus.SC_OK);
        data.put("data", sbw);
        return data;
    }

    @Transactional
    public JSONObject updateSbwEntity(String sbwid, SbwUpdateParamWrapper param) {

        JSONObject data=new JSONObject();
        Sbw sbwEntity = sbwRepository.findBySbwId(sbwid);
        if (null == sbwEntity) {
            log.error("In disassociate process,failed to find the sbw by id:{} ", sbwid);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
            data.put("httpCode", HttpStatus.SC_NOT_FOUND);
            data.put("interCode", ReturnStatus.SC_NOT_FOUND);
            return data;
        }
        if(!CommonUtil.isAuthoried(sbwEntity.getProjectId())){
            log.error("User have no write to operate sbw:{}", sbwid);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
            data.put("httpCode", HttpStatus.SC_FORBIDDEN);
            data.put("interCode", ReturnStatus.SC_FORBIDDEN);
            return data;
        }
        if(param.getSbwUpdateParam().getBillType().equals(HsConstants.MONTHLY)){
            //can’t sub
            if(param.getSbwUpdateParam().getBandwidth()<sbwEntity.getBandWidth()){
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR));
                data.put("httpCode", HttpStatus.SC_BAD_REQUEST);
                data.put("interCode", ReturnStatus.SC_PARAM_ERROR);
                return data;
            }
        }

        Firewall firewall = firewallRepository.findFirewallByRegion(param.getSbwUpdateParam().getRegion());
        boolean updateStatus = firewallService.updateQosBandWidth(firewall.getId(),sbwEntity.getPipeId(), sbwEntity.getSbwId(), String.valueOf(param.getSbwUpdateParam().getBandwidth()));
        if (updateStatus ||CommonUtil.qosDebug) {
            sbwEntity.setBandWidth(param.getSbwUpdateParam().getBandwidth());
            sbwEntity.setBillType(param.getSbwUpdateParam().getBillType());
            sbwEntity.setUpdateTime(CommonUtil.getGmtDate());
            sbwRepository.saveAndFlush(sbwEntity);
            data.put("reason","");
            data.put("httpCode", HttpStatus.SC_OK);
            data.put("interCode", ReturnStatus.SC_OK);
            data.put("data",sbwEntity);
            return data;
        }else{
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_CHANGE_BANDWIDTH_ERROR));
            data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            data.put("interCode", ReturnStatus.SC_FIREWALL_SERVER_ERROR);
            return data;
        }
    }

    /**
     *
     * add eip to SBW
     * @param sbwId
     * @param eipId
     * @param region
     * @return
     */
    @Transactional
    public MethodSbwReturn addEipToSbw(String sbwId, List<String> eipId, String region)throws Exception{
        Sbw sbwEntity = sbwRepository.findBySbwId(sbwId);
        if (null == sbwEntity) {
            log.error("In addEipToSbw process, failed to find the sbw by id:{} ", sbwId);
            return MethodReturnUtil.errorSbw(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_NOT_FOND_BY_ID));
        }
        if (!sbwEntity.getProjectId().equals(CommonUtil.getUserId())) {
            log.error(CodeInfo.getCodeMessage(CodeInfo.SBW_FORBIDEN_WITH_ID), sbwId);
            return MethodReturnUtil.error(HttpStatus.SC_FORBIDDEN, ReturnStatus.SC_FORBIDDEN,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_FORBIDDEN));
        }
        if(eipId!=null&&eipId.size()>0) {
            for (int count = 0;count<eipId.size();count++) {
                String id = eipId.get(count);
                Eip eipEntity = eipRepository.findByEipId(id);
                String  pipeid= firewallService.addQos(eipEntity.getFloatingIp(), eipEntity.getEipAddress(), String.valueOf(eipEntity.getBandWidth()), eipEntity.getFirewallId());


                int eipbandWidth = eipEntity.getBandWidth();
                eipEntity.setOldBandWidth(eipbandWidth);
                eipRepository.saveAndFlush(eipEntity);
                int sbwBandWidth= sbwEntity.getBandWidth();
                sbwEntity.setIpCount(count);
                sbwEntity.setPipeId(pipeid);
                eipEntity.setBandWidth(sbwBandWidth);
                eipEntity.setChargeMode("SharedBandwidth");
                eipEntity.setSharedBandWidthId(sbwId);
                eipRepository.saveAndFlush(eipEntity);
                if (pipeid!=null){
                    log.info("addQos sucess floatingIp:{},eipAddress:{},bandWidth:{},fireWallId:{}",eipEntity.getFloatingIp(),eipEntity.getEipAddress(),String.valueOf(eipEntity.getBandWidth()),eipEntity.getFirewallId());
                    firewallService.delQos(eipEntity.getPipId(),eipEntity.getFirewallId());
                }else {
                    log.error("addQos failed pipeid:{}",pipeid);
                }




            }

        }
        // todo add Eip toSbw
        // find eip and checked
        //create the qos and get the qos pipe id ,then remeber the pipe id
        //update the eip and sbw database (sbw table need to add the origin bandwidth field)
        //get the floatip and insert to the qos
        //add net and qos to firewall
        //If the above steps execute successfully,then remove the origin qos and update eip table
        return MethodReturnUtil.success();
    }

    /**
     * remove eip from sbw is same as addToSbw
     * @param sbwId
     * @param eipId
     * @param region
     * @return
     * @throws Exception
     */
    @Transactional
    public MethodSbwReturn removeEipFromSbw(String sbwId, List<String> eipId, String region)throws Exception{
        String returnStat = "";
        String returnMsg = "";
        Sbw sbw = sbwRepository.findBySbwId(sbwId);
        if (null == sbw) {
            log.error("In addEipToSbw process, failed to find the sbw by id:{} ", sbwId);
            return MethodReturnUtil.errorSbw(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_NOT_FOND_BY_ID));
        }
        if (!sbw.getProjectId().equals(CommonUtil.getUserId())) {
            log.error(CodeInfo.getCodeMessage(CodeInfo.SBW_FORBIDEN_WITH_ID), sbwId);
            return MethodReturnUtil.error(HttpStatus.SC_FORBIDDEN, ReturnStatus.SC_FORBIDDEN,
                    CodeInfo.getCodeMessage(CodeInfo.SBW_FORBIDDEN));
        }
        // todo remove Eip toSbw
        for (String eipid:eipId) {
            Eip eipEntity = eipRepository.findByEipId(eipid);
            if (eipEntity == null) {
                log.error("In removeEipToSbw process, failed to find the eipEntity by eipid:{} ", eipid);
                return MethodReturnUtil.errorSbw(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                        CodeInfo.getCodeMessage(CodeInfo.SBW_NOT_FOND_BY_ID));
            }
            String firewallId = eipEntity.getFirewallId();
            String floatingIp = eipEntity.getFloatingIp();
            String bandWidth = eipEntity.getBandWidth()+"";
            int oldBandWidth = eipEntity.getOldBandWidth();
            String  pipeId=sbw.getPipeId();
            if (pipeId==null||pipeId==""){
                log.error("In removeEipToSbw process, failed to find sbw from the pipId:{} ", pipeId);
                return MethodReturnUtil.errorSbw(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                        CodeInfo.getCodeMessage(CodeInfo.SBW_NOT_FOND_BY_ID));
            }
            boolean delQosResult = firewallService.delQos(pipeId, firewallId);
            if(delQosResult) {
                eipEntity.setPipId(null);
            } else {
                returnMsg = "Failed to del Qos, eipId:"+eipEntity.getEipId()+"pipId:"+eipEntity.getPipId()+"";
                log.error(returnMsg);
            }
            String pipid = firewallService.addQos(floatingIp, eipid, bandWidth, firewallId);
            if(pipid==null ){
                log.error("In removeEipToSbw process, failed to find the pipId:{} ", pipeId);
                return MethodReturnUtil.errorSbw(HttpStatus.SC_INTERNAL_SERVER_ERROR, ReturnStatus.SC_FIREWALL_QOS_UNAVAILABLE,
                        CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_QOS_ERROR));
            }
            eipEntity.setPipId(pipid);
            eipEntity.setSharedBandWidthId(null);
            eipEntity.setBandWidth(oldBandWidth);
            eipEntity.setUpdateTime(new Date());
            eipRepository.saveAndFlush(eipEntity);
        }
        sbw.setPipeId(null);
        sbw.setChargeMode("BandWidth");
        sbwRepository.saveAndFlush(sbw);
        return MethodReturnUtil.success();
    }

}
