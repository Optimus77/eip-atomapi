package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.*;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.service.impl.EipServiceImpl;
import com.inspur.eipatomapi.util.CommonUtil;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.NetFloatingIP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

@Service
public class EipDaoService {
    @Autowired
    private EipPoolRepository eipPoolRepository;

    @Autowired
    private FirewallRepository firewallRepository;

    @Autowired
    private EipRepository eipRepository;

    @Autowired
    private FirewallService firewallService;

    @Autowired
    private NeutronService neutronService;

    private final static Log log = LogFactory.getLog(EipServiceImpl.class);

    /**
     * allocate eip
     *
     * @param eipConfig    eipconfig
     * @param networkId network id
     * @return result
     */
    @Transactional
    public Eip allocateEip(EipAllocateParam eipConfig, String networkId, String portId) throws Exception{

        EipPool eip = getOneEipFromPool();
        if(null != eip){
            if (eip.getState().equals("0")) {
                Eip eipMo = new Eip();
                eipMo.setEipAddress(eip.getIp());
                eipMo.setStatus("DOWN");
                eipMo.setFirewallId(eip.getFireWallId());

                NetFloatingIP floatingIP = neutronService.createFloatingIp(eipConfig.getRegion(), networkId, portId);
                if (null != floatingIP) {
                    eipMo.setFloatingIp(floatingIP.getFloatingIpAddress());
                    eipMo.setPrivateIpAddress(floatingIP.getFixedIpAddress());
                    eipMo.setFloatingIpId(floatingIP.getId());
                    eipMo.setIpType(eipConfig.getIptype());
                    eipMo.setChargeType(eipConfig.getChargetype());
                    eipMo.setChargeMode(eipConfig.getChargemode());
                    eipMo.setPurchaseTime(eipConfig.getPurchasetime());
                    eipMo.setBandWidth(eipConfig.getBandwidth());
                    eipMo.setSharedBandWidthId(eipConfig.getSharedBandWidthId());
                    String tenantid = CommonUtil.getOsClientV3Util().getToken().getProject().getId();
                    log.info("get tenantid from clientv3"+tenantid);
                    log.info("get tenantid from token"+CommonUtil.getProjectId());
                    eipMo.setProjectId(tenantid);

                    eipPoolRepository.delete(eip);
                    eipMo = eipRepository.save(eipMo);
                    return eipMo;
                }
            }
        }

        log.warn("Failed to allocate eip in network：" + networkId);
        return null;
    }

    @Transactional
    public boolean deleteEip(String  eipid) throws Exception {

        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            return false;
        }

        if ((null != eipEntity.getPipId())
                || (null != eipEntity.getDnatId())
                || (null != eipEntity.getSnatId())) {
            log.warn("Failed to delete eip,Eip is bind to port."+eipEntity.getPipId()+eipEntity.getDnatId()+eipEntity.getSnatId());
            return false;
        }
        boolean delFipResult = true;
        if(null != eipEntity.getFloatingIpId()) {
            delFipResult = neutronService.deleteFloatingIp(eipEntity.getName(), eipEntity.getFloatingIpId());
        }
        if(delFipResult) {
            EipPool eipPoolMo = new EipPool();
            eipPoolMo.setFireWallId(eipEntity.getFirewallId());
            eipPoolMo.setIp(eipEntity.getEipAddress());
            eipPoolMo.setState("0");

            eipRepository.deleteById(eipEntity.getEipId());

            eipPoolRepository.save(eipPoolMo);
            return true;
        } else {
            log.warn("Failed to delete floating ip ." + eipEntity.getFloatingIpId());
            return false;
        }
    }

    /**
     * associate port with eip
     * @param eipid          eip
     * @param serverId     server id
     * @param instanceType instance type
     * @return             true or false
     * @throws Exception   e
     */
    @Transactional
    public Eip associateInstanceWithEip(String eipid, String serverId, String instanceType, String portId)
            throws Exception{
        String dnatRuleId = null;
        String snatRuleId = null;
        String pipId;

        Eip eip = eipRepository.findByEipId(eipid);
        if(null == eip){
            log.warn("In associate process, failed to find the eip by id "+eipid);
            return null;
        }
        if(!(eip.getStatus().equals("DOWN")) || (null != eip.getDnatId())
                || (null != eip.getSnatId()) || (null != eip.getPipId())){
//            log.warn("In associate process, eip status is not down id:{}, status:{}, pipId:{}, snatId:{}, DnstId:{}",
//                    eipid,eip.getStatus(), eip.getPipId(),eip.getSnatId(),eip.getDnatId());
            return null;
        }
        ActionResponse actionResponse = neutronService.associaInstanceWithFloatingIp(eip,serverId);
        if(actionResponse.isSuccess()){
            dnatRuleId = firewallService.addDnat(eip.getFloatingIp(), eip.getEipAddress(), eip.getFirewallId());
            snatRuleId = firewallService.addSnat(eip.getFloatingIp(), eip.getEipAddress(), eip.getFirewallId());
            if((null != dnatRuleId) && (null != snatRuleId)){
                pipId = firewallService.addQos(eip.getFloatingIp(),
                        eip.getEipAddress(),
                        String.valueOf(eip.getBandWidth()),
                        eip.getFirewallId());
                if(null != pipId || CommonUtil.isDebug) {
                    eip.setInstanceId(serverId);
                    eip.setInstanceType(instanceType);
                    eip.setDnatId(dnatRuleId);
                    eip.setSnatId(snatRuleId);
                    eip.setPipId(pipId);
                    eip.setPortId(portId);
                    eip.setStatus("ACTIVE");
                    return eipRepository.save(eip);
                } else {
                    log.warn("Failed to add qos in firewall "+eip.getFirewallId());
                }
            } else {
                log.warn("Failed to add snat and dnat in firewall "+eip.getFirewallId());

            }
        } else {
            log.warn("Failed to associate port with eip, serverId "+serverId);
        }
        if(actionResponse.isSuccess() ){
            neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
        }
        if(null != snatRuleId){
            firewallService.delSnat(snatRuleId, eip.getFirewallId());
        }
        if(null != dnatRuleId){
            firewallService.delDnat(dnatRuleId, eip.getFirewallId());
        }

        return null;
    }


    /**
     * disassociate port with eip
     * @param eipid    eip id
     * @return             reuslt, true or false
     * @throws Exception   e
     */
    @Transactional
    public Boolean disassociateInstanceWithEip(String eipid) throws Exception  {

        Eip eipEntity = eipRepository.findByEipId(eipid);
        if(null == eipEntity){
            log.warn("In disassociate process,failed to find the eip by id "+eipid);
            return false;
        }
        if(!(eipEntity.getStatus().equals("ACTIVE")) || (null == eipEntity.getSnatId())
                || (null == eipEntity.getDnatId()) ){
            return false;
        }

        ActionResponse actionResponse= neutronService.disassociateInstanceWithFloatingIp(eipEntity.getFloatingIp(),
                eipEntity.getInstanceId());
        if(actionResponse.isSuccess()) {
            eipEntity.setInstanceId(null);
            eipEntity.setInstanceType(null);
            eipEntity.setPrivateIpAddress(null);
        } else {
            log.warn("Failed to disassociate port with eip, floatingipid:" + eipEntity.getFloatingIpId());
        }
        Boolean delDnatResult = firewallService.delDnat(eipEntity.getDnatId(), eipEntity.getFirewallId());
        if(delDnatResult){
            eipEntity.setDnatId(null);
        }else{
            log.warn("Failed to del dnat in firewall" + eipEntity.getFirewallId());
        }
        Boolean delSnatResult = firewallService.delSnat(eipEntity.getSnatId(), eipEntity.getFirewallId());
        if(delSnatResult) {
            eipEntity.setSnatId(null);
        }else {
            log.warn("Failed to del snat in firewall" + eipEntity.getFirewallId());
        }

        Boolean delQosResult = firewallService.delQos(eipEntity.getPipId(), eipEntity.getFirewallId());
        if(delQosResult) {
            eipEntity.setPipId(null);
        } else {
            log.warn("Failed to del qos" + eipEntity.getPipId());
        }

        eipEntity.setStatus("DOWN");
        eipRepository.save(eipEntity);

        return delDnatResult && delSnatResult && delQosResult && (actionResponse.isSuccess());
    }

    @Transactional
    public Eip updateEipEntity(String eipid, EipUpdateParamWrapper param) {

        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            log.warn("In disassociate process,failed to find the eip by id " + eipid);
            return null;
        }

        boolean updateStatus = firewallService.updateQosBandWidth(eipEntity.getFirewallId(),
                eipEntity.getPipId(), eipEntity.getEipId(),
                String.valueOf(param.getEipUpdateParam().getBandWidth()));
        if (updateStatus || CommonUtil.isDebug) {
            log.info("before change：" + eipEntity.getBandWidth());
            eipEntity.setBandWidth(param.getEipUpdateParam().getBandWidth());
            log.info("after  change：" + eipEntity.getBandWidth());

            return eipRepository.save(eipEntity);
        }
        return null;
    }

    public List<Eip> findByProjectId(String projectId){
        return eipRepository.findByProjectId(projectId);
    }

    @Transactional
    public void addEipPool(String ip) {

        String id = "firewall_id1";
        Firewall firewall = new Firewall();
        firewall.setIp(ip);
        firewall.setPort("443");
        firewall.setUser("hillstone");
        firewall.setPasswd("hillstone");
        firewall.setParam1("eth0/0/0");
        firewall.setParam2("eth0/0/1");
        firewall.setParam3("eth0/0/2");
        firewallRepository.save(firewall);
        List<Firewall> firewalls = firewallRepository.findAll();
        for(Firewall fw : firewalls){
            id = fw.getId();
        }

        for (int i = 0; i < 100; i++) {
            EipPool eipPoolMo = new EipPool();
            eipPoolMo.setFireWallId(id);
            eipPoolMo.setIp("13.2.3."+i);
            eipPoolMo.setState("0");
            eipPoolRepository.save(eipPoolMo);
        }
    }

    private synchronized EipPool getOneEipFromPool(){
        return eipPoolRepository.getEipByRandom();
    }
}
