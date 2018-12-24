package com.inspur.eipatomapi.service;

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

@Slf4j
@Service
public class SbwDaoService {
    @Autowired
    private SbwRepository sbwRepository;

    public List<Sbw> findByProjectId(String projectId){
        return sbwRepository.findByProjectIdAndIsDelete(projectId,0);
    }

    @Transactional
    public Sbw allocateSbw(SbwAllocateParam sbwConfig) throws Exception{

        Sbw sbwMo = new Sbw();
        sbwMo.setRegion(sbwConfig.getRegion());
        sbwMo.setSharedbandwidthname(sbwConfig.getSharedbandwidthname());

        sbwMo.setBillType(sbwConfig.getBillType());
        sbwMo.setChargeMode(sbwConfig.getChargemode());
        sbwMo.setDuration(sbwConfig.getDuration());
        sbwMo.setBandWidth(sbwConfig.getBandwidth());
        sbwMo.setRegion(sbwConfig.getRegion());
        String userId = CommonUtil.getUserId();
        log.debug("get tenantid:{} from clientv3", userId);
        //log.debug("get tenantid from token:{}", CommonUtil.getProjectId(eipConfig.getRegion()));
        sbwMo.setProjectId(userId);
        sbwMo.setIsDelete(0);

        sbwMo.setCreateTime(CommonUtil.getGmtDate());
        sbwRepository.saveAndFlush(sbwMo);
        log.info("User:{} success allocate eip:{}",userId, sbwMo.getSbwId());
        return sbwMo;
    }
}
