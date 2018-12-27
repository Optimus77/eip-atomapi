package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.entity.sbw.ConsoleCustomization;
import com.inspur.eipatomapi.entity.sbw.Sbw;
import com.inspur.eipatomapi.entity.sbw.SbwAllocateParam;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.SbwRepository;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.HsConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.openstack4j.model.common.ActionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SbwDaoService {
    @Autowired
    private SbwRepository sbwRepository;

    @Autowired
    private EipRepository eipRepository;

    public List<Sbw> findByProjectId(String projectId){
        return sbwRepository.findByProjectIdAndIsDelete(projectId,0);
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

    /**
     * delete
     * @param sbwId
     * @return
     */
    public ActionResponse deleteEip(String sbwId){
        String msg;
        int ipCount = 0;
        Sbw entity = sbwRepository.findBySbwId(sbwId);
        if (null == entity) {
            msg= "Faild to find sbw by id:"+sbwId;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        if(!CommonUtil.isAuthoried(entity.getProjectId())){
            log.error(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), sbwId);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }
        ipCount = entity.getIpCount();
        if (ipCount !=0 ||ipCount>0){
            msg = "Elastic IP in Shared bandwidth cannot be removed ,ipCount:{}"+ipCount ;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
        }
        if(null != entity.getChargeMode() ) {
            if (!entity.getChargeMode().equalsIgnoreCase(HsConstants.SHAREDBANDWIDTH)) {
                msg = "Only Shared bandwidth is allowed for chargeMode";
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
            }
        }
        if (null != entity.getBillType()){
            if (!entity.getBillType().equalsIgnoreCase(HsConstants.HOURLYSETTLEMENT)){
                msg = "Only hourlysettlement is allowed for billType";
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_FORBIDDEN);
            }
        }
        entity.setIsDelete(1);
        entity.setUpdateTime(CommonUtil.getGmtDate());
        sbwRepository.saveAndFlush(entity);

        List<Eip> eipList = eipRepository.findBySharedBandWidthIdAndIsDelete(sbwId, 0);
        if (eipList!= null && eipList.size()>0){
            for (int i = 0; i < eipList.size(); i++) {
                Eip eip = eipList.get(i);
                eip.setSharedBandWidthId(null);
                eip.setUpdateTime(CommonUtil.getGmtDate());
                eipRepository.saveAndFlush(eip);
            }
        }
        return ActionResponse.actionSuccess();
    }
}
