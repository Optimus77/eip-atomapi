package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.entity.sbw.ConsoleCustomization;
import com.inspur.eipatomapi.entity.sbw.Sbw;
import com.inspur.eipatomapi.entity.sbw.SbwAllocateParam;
import com.inspur.eipatomapi.repository.SbwRepository;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.HsConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SbwDaoService {
    @Autowired
    private SbwRepository sbwRepository;

    public List<Sbw> findByProjectId(String projectId){
        return sbwRepository.findByProjectId(projectId);
    }

    @Transactional
    public Sbw allocateSbw(ConsoleCustomization sbwConfig) throws Exception {

        Sbw sbwMo = new Sbw();
        sbwMo.setRegion(sbwConfig.getRegion());
        sbwMo.setSharedbandwidthName(sbwConfig.getSharedbandwidthname());
        sbwMo.setBandWidth(sbwConfig.getBandwidth());
        sbwMo.setBillType(sbwConfig.getBillType());
        sbwMo.setChargeMode(sbwConfig.getChargemode());
        sbwMo.setDuration(sbwConfig.getDuration());
        sbwMo.setBandWidth(1);
        sbwMo.setRegion(sbwConfig.getRegion());
        String userId = CommonUtil.getUserId();
        sbwMo.setProjectId(userId);
        sbwMo.setIsDelete(0);
        sbwMo.setCreateTime(CommonUtil.getGmtDate());
        sbwRepository.saveAndFlush(sbwMo);
        log.info("User:{} success allocate sbw:{} ,sbw:{}", userId, sbwMo.getSbwId(), sbwMo.toString());
        return sbwMo;
    }

    public Sbw getSbwById(String id){

        Sbw sbwEntity = null;
        Optional<Sbw> sbw = sbwRepository.findById(id);
        if (sbw.isPresent()) {
            sbwEntity = sbw.get();
        }

        return sbwEntity;
    }
}
