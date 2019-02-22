package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.entity.eipv6.EipPoolV6;
import com.inspur.eipatomapi.entity.eipv6.EipV6;
import com.inspur.eipatomapi.entity.eipv6.EipV6AllocateParam;
import com.inspur.eipatomapi.repository.*;
import com.inspur.eipatomapi.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.openstack4j.model.common.ActionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class EipV6DaoService {


    @Autowired
    private EipPoolV6Repository eipPoolV6Repository;

    @Autowired
    private EipV6Repository eipV6Repository;

    @Autowired
    private EipRepository eipRepository;

    @Autowired
    private FireWallCommondService fireWallCommondService;



    /**
     * allocate eipv6
     *
     * @param eipConfig    eipconfig
     * @return result
     */
    @Transactional
    public EipV6 allocateEipV6(EipV6AllocateParam eipConfig, EipPoolV6 eipPoolv6) throws KeycloakTokenException {


        if (!eipPoolv6.getState().equals("0")) {
            log.error("Fatal Error! eipv6 state is not free, state:{}.", eipPoolv6.getState());
            eipPoolV6Repository.saveAndFlush(eipPoolv6);
            return null;
        }
        EipPoolV6 eipPoolV6Check  = eipPoolV6Repository.findByIp(eipPoolv6.getIp());
        if(eipPoolV6Check != null){
            log.error("==================================================================================");
            log.error("Fatal Error! get a duplicate eipv6 from eip pool v6, eip_v6_address:{}.", eipPoolv6.getIp());
            log.error("===================================================================================");
            eipPoolV6Repository.deleteById(eipPoolV6Check.getId());
            eipPoolV6Repository.flush();
        }
        EipV6 eipV6Entity = eipV6Repository.findByIpv6AndIsDelete(eipPoolv6.getIp(), 0);
        if(null != eipV6Entity){
            log.error("Fatal Error! get a duplicate eipv6 from eip pool v6, eip_v6_address:{} eipv6Id:{}.",
                    eipV6Entity.getIpv6(), eipV6Entity.getEipV6Id());
            return null;
        }

        Eip eip = eipRepository.findByEipId(eipConfig.getEipId());
        if(null == eip){
            log.error("Faild to find eip by id:"+eipConfig.getEipId());
            return null;
        }
        EipV6 eipMo = new EipV6();
        String ipv6 = eipPoolv6.getIp();
        String ipv4 = eip.getEipAddress();
        if(eip.getFloatingIp() != null && eip.getFloatingIp()!=""){
            eipMo.setFloatingIp(eip.getFloatingIp());
            String newSnatptId = fireWallCommondService.execCustomCommand("configure\r"
                    + "ip vrouter trust-vr\r"
                    + "dnatrule from ipv6-any to " + ipv6
                    + " service any trans-to "  + ipv4 +"\r"
                    +"end");
            log.info("create ret:{}",newSnatptId);
            if(newSnatptId != null){
                String newDnatptId = fireWallCommondService.execCustomCommand("configure\r"
                        + "ip vrouter trust-vr\r"
                        + "snatrule from ipv6-any to "  + ipv6
                        + " service any trans-to "  + ipv4
                        + " mode dynamicport" +"\r"
                        +"end");
                eipMo.setSnatptId(newSnatptId);
                eipMo.setDnatptId(newDnatptId);

            }
        }

        eipMo.setIpv6(eipPoolv6.getIp());
        eipMo.setStatus(HsConstants.DOWN);
        eipMo.setFirewallId(eipPoolv6.getFireWallId());

        eipMo.setRegion(eip.getRegion());
        eipMo.setIpv4(eip.getEipAddress());
        String userId = CommonUtil.getUserId();
        log.debug("get tenantid:{} from clientv3", userId);
        eipMo.setProjectId(userId);
        eipMo.setIsDelete(0);

        eipMo.setCreateTime(CommonUtil.getGmtDate());
        eipV6Repository.saveAndFlush(eipMo);

        log.info("User:{} success allocate eipv6:{}",userId, eipMo.getEipV6Id());
        return eipMo;
    }

    @Transactional(isolation= Isolation.SERIALIZABLE)
    public synchronized EipPoolV6 getOneEipFromPoolV6(){
        EipPoolV6 eipAddress =  eipPoolV6Repository.getEipV6ByRandom();
       if(null != eipAddress) {
            eipPoolV6Repository.deleteById(eipAddress.getId());
           eipPoolV6Repository.flush();
       }
        return eipAddress;
    }


    public List<EipV6> findEipV6ByProjectId(String projectId){
        return eipV6Repository.findByProjectIdAndIsDelete(projectId,0);
    }


    @Transactional
    public ActionResponse deleteEipV6(String  eipv6id) throws KeycloakTokenException {
        String msg;
        EipV6 eipV6Entity = eipV6Repository.findByEipV6Id(eipv6id);
        if (null == eipV6Entity) {
            msg= "Faild to find eipV6 by id:"+eipv6id;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        if(!CommonUtil.isAuthoried(eipV6Entity.getProjectId())){
            log.error(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), eipv6id);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }

        String dnatptId = eipV6Entity.getDnatptId();
        String snatptId = eipV6Entity.getSnatptId();
        String[] split = dnatptId.split("=");
        String[] splitt = snatptId.split("=");
        dnatptId=split[1];
        snatptId=splitt[1];
        if(dnatptId != null && snatptId !=null){
            String disconnectNat = fireWallCommondService.execCustomCommand("configure\r"
                    + "ip vrouter trust-vr\r"
                    + "no snatrule id "+snatptId +"\r"
                    + "no dnatrule id "+dnatptId +"\r"
                    +"end");
            log.info("disconnectNat:"+disconnectNat);
            if(disconnectNat == null){
                eipV6Entity.setDnatptId(null);
                eipV6Entity.setSnatptId(null);
                eipV6Entity.setFloatingIp(null);
            }else{
                log.error("Deletion mapping failed while deleting ipv6 ,dnatptid {},snatptid {}",
                        eipV6Entity.getDnatptId(),eipV6Entity.getSnatptId());
            }
        }
        eipV6Entity.setIsDelete(1);
        eipV6Entity.setUpdateTime(CommonUtil.getGmtDate());
        eipV6Repository.saveAndFlush(eipV6Entity);
        EipPoolV6 eipV6Pool = eipPoolV6Repository.findByIp(eipV6Entity.getIpv6());
        if(null != eipV6Pool){
            log.error("******************************************************************************");
            log.error("Fatal error, eipV6 has already exist in eipV6 pool. can not add to eipV6 pool.{}",
                    eipV6Entity.getIpv6());
            log.error("******************************************************************************");
        }else {
            EipPoolV6 eipPoolV6Mo = new EipPoolV6();
            eipPoolV6Mo.setFireWallId(eipV6Entity.getFirewallId());
            eipPoolV6Mo.setIp(eipV6Entity.getIpv6());
            eipPoolV6Mo.setState("0");
            eipPoolV6Repository.saveAndFlush(eipPoolV6Mo);
            log.info("Success delete eipV6:{}",eipV6Entity.getIpv6());
        }
        return ActionResponse.actionSuccess();
    }


    public EipV6 getEipV6ById(String id){

        EipV6 eipV6Entity = null;
        Optional<EipV6> eipV6 = eipV6Repository.findById(id);
        if (eipV6.isPresent()) {
            eipV6Entity = eipV6.get();
        }

        return eipV6Entity;
    }

    @Transactional(isolation= Isolation.SERIALIZABLE)
    public EipV6 updateIp(String newIpv4 ,EipV6 eipV6){

        eipV6.setIpv4(newIpv4);
        eipV6.setUpdateTime(CommonUtil.getGmtDate());
        eipV6Repository.saveAndFlush(eipV6);
        return eipV6;
    }

}
