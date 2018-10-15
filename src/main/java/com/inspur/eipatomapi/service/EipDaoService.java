package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.*;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import org.apache.http.HttpStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.NetFloatingIP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public final static Logger log = LoggerFactory.getLogger(EipDaoService.class);
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
                    log.debug("get tenantid:{} from clientv3",tenantid);
                    log.debug("get tenantid from token:{}",CommonUtil.getProjectId());
                    eipMo.setProjectId(tenantid);

                    eipPoolRepository.delete(eip);
                    eipMo = eipRepository.save(eipMo);
                    return eipMo;
                }
            }
        }

        log.error("Failed to allocate eip in network：{}, ",networkId);
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
            log.error("Failed to delete eip,eipId:{},pipId:{}, dnatId:{}, snatid:{}.",
                    eipEntity.getEipId(),eipEntity.getPipId(), eipEntity.getDnatId(),eipEntity.getSnatId());
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
            log.error("Failed to delete floating ip, floatingIpId:{}.",eipEntity.getFloatingIpId());
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
    public JSONObject associateInstanceWithEip(String eipid, String serverId, String instanceType, String portId)
            throws Exception{


        JSONObject data=new JSONObject();
        Eip eip = eipRepository.findByEipId(eipid);
        if(null == eip){
            log.error("In associate process, failed to find the eip by id:{} ",eipid);
            data.put("flag",false);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
            data.put("httpCode", HttpStatus.SC_NOT_FOUND);
            data.put("interCode", ReturnStatus.SC_NOT_FOUND);
            data.put("data",null);
            return data;
        }
        if(!(eip.getStatus().equals("DOWN")) || (null != eip.getDnatId())
                || (null != eip.getSnatId()) || (null != eip.getPipId())){
            data.put("flag",false);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_HAS_BAND));
            data.put("httpCode", HttpStatus.SC_BAD_REQUEST);
            data.put("interCode", ReturnStatus.EIP_BIND_HAS_BAND);
            data.put("data",null);
            return data;
        }
        if(serverId==null){
            data.put("flag",false);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_PARA_SERVERID_ERROR));
            data.put("httpCode", HttpStatus.SC_BAD_REQUEST);
            data.put("interCode", ReturnStatus.SC_PARAM_ERROR);
            data.put("data",null);
            return data;
        }
        ActionResponse actionResponse;
        try{
            actionResponse = neutronService.associaInstanceWithFloatingIp(eip,serverId);
        }catch (Exception e){
            log.error("==========openstack associaInstanceWithFloatingIp error=====serverId :"+serverId);
            log.error("==========openstack associaInstanceWithFloatingIp error=====eip :"+eip.getFloatingIp());
            e.printStackTrace();
            data.put("flag",false);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_OPENSTACK_ERROR));
            data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            data.put("interCode", ReturnStatus.SC_OPENSTACK_SERVER_ERROR);
            data.put("data",null);
            return data;
        }
        String pipId;
        String dnatRuleId = null;
        String snatRuleId = null;
        if(actionResponse.isSuccess()){
            log.info("start dnat oprate ");
            try{
                dnatRuleId = firewallService.addDnat(eip.getFloatingIp(), eip.getEipAddress(), eip.getFirewallId());
                log.info("dnatRuleId:  "+dnatRuleId);
            }catch(Exception e){
                e.printStackTrace();
                log.error("addDnat======filewall addDnat error==========");
                log.info("addDnat====start  disassociateInstanceWithFloatingIp  ");
                neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
                log.info("addDnat===end  disassociateInstanceWithFloatingIp  ");
                data.put("flag",false);
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_DNAT_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_DNAT_UNAVAILABLE);
                data.put("data",null);
                return data;
            }

            log.info("start snat oprate ");
            try{
                snatRuleId = firewallService.addSnat(eip.getFloatingIp(), eip.getEipAddress(), eip.getFirewallId());
                log.info("snatRuleId:  "+snatRuleId);
            }catch(Exception e){
                e.printStackTrace();
                log.error("addSnat======filewall addSnat error==========");
                log.info("addSnat====start  disassociateInstanceWithFloatingIp  ");
                neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
                log.info("addSnat===end  disassociateInstanceWithFloatingIp  ");
                log.info("addSnat====start del dnatRuleId ");
                firewallService.delDnat(dnatRuleId, eip.getFirewallId());
                log.info("====end del dnatRuleId ");
                firewallService.delSnat(snatRuleId, eip.getFirewallId());

                data.put("flag",false);
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_SNAT_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_SNAT_UNAVAILABLE);
                data.put("data",null);
                return data;
            }

            log.info("start qos oprate ");
            try{
                pipId = firewallService.addQos(eip.getFloatingIp(), eip.getEipAddress(), String.valueOf(eip.getBandWidth()), eip.getFirewallId());
                if(null != pipId || CommonUtil.isDebug) {
                    eip.setInstanceId(serverId);
                    eip.setInstanceType(instanceType);
                    eip.setDnatId(dnatRuleId);
                    eip.setSnatId(snatRuleId);
                    eip.setPipId(pipId);
                    eip.setPortId(portId);
                    eip.setStatus("ACTIVE");
                    eipRepository.save(eip);
                    data.put("flag",true);
                    data.put("reason","success");
                    data.put("httpCode", HttpStatus.SC_OK);
                    data.put("interCode", ReturnStatus.SC_OK);
                    data.put("data",eip);
                    return data;

                } else {
                    log.error("Failed to add qos in firewall,eipId:{}, fip:{}, firewallId:{} .", eip.getEipId(), eip.getFloatingIp(), eip.getFirewallId());
                    data.put("flag",false);
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_QOS_ERROR));
                    data.put("httpCode", HttpStatus.SC_OK);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_QOS_UNAVAILABLE);
                    data.put("data",null);
                    return data;
                }
            } catch (Exception e){
                e.printStackTrace();
                log.error("qos is error");
                log.info("addQos====start  disassociateInstanceWithFloatingIp  ");
                neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
                log.info("addQos===end  disassociateInstanceWithFloatingIp  ");
                log.info("addQos====start del dnatRuleId ");
                firewallService.delDnat(dnatRuleId, eip.getFirewallId());
                log.info("====end del dnatRuleId ");
                log.info("addQos====start del snatRuleId ");
                firewallService.delSnat(snatRuleId, eip.getFirewallId());
                log.info("====end del snatRuleId ");
                data.put("flag",false);
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_QOS_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_QOS_UNAVAILABLE);
                data.put("data",null);
                return data;
            }

        } else {
            log.error("Failed to associate port:{} with eip:{}, serverId:{} ", portId, eipid, serverId);
            data.put("flag",false);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_OPENSTACK_ASSOCIA_FAIL));
            data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            data.put("interCode", ReturnStatus.SC_OPENSTACK_SERVER_ERROR);
            data.put("data",null);
            return data;
        }


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
            log.error("In disassociate process,failed to find the eip by id:{} ",eipid);
            return false;
        }
        if(!(eipEntity.getStatus().equals("ACTIVE")) || (null == eipEntity.getSnatId())
                || (null == eipEntity.getDnatId()) || null == eipEntity.getFloatingIp()){
            log.error("Error status when disassociate eip,eipId:{}, status:{}, snatId:{}, dnatId:{}, fipId:{} ", eipid, eipEntity.getStatus(), eipEntity.getSnatId(), eipEntity.getDnatId());
        }
        boolean delFip = false;
        if(null != eipEntity.getFloatingIp() && null != eipEntity.getInstanceId()) {
            ActionResponse actionResponse = neutronService.disassociateInstanceWithFloatingIp(eipEntity.getFloatingIp(),
                    eipEntity.getInstanceId());
            if (actionResponse.isSuccess()) {
                eipEntity.setInstanceId(null);
                eipEntity.setInstanceType(null);
                eipEntity.setPrivateIpAddress(null);
                delFip = true;
            }else {
                log.error("Failed to disassociate port with fip,eipId:{},  floatingip:{}, instanceId:{}",
                        eipEntity.getEipId(), eipEntity.getFloatingIp(), eipEntity.getInstanceId());
            }
        }

        Boolean delDnatResult = firewallService.delDnat(eipEntity.getDnatId(), eipEntity.getFirewallId());
        if (delDnatResult) {
            eipEntity.setDnatId(null);
        } else {
            log.error("Failed to del dnat in firewall,eipId:{}, dnatId:{}",
                    eipEntity.getEipId(), eipEntity.getDnatId());
        }

        Boolean delSnatResult = firewallService.delSnat(eipEntity.getSnatId(), eipEntity.getFirewallId());
        if (delSnatResult) {
            eipEntity.setSnatId(null);
        } else {
            log.error("Failed to del snat in firewall, eipId:{},snatId:{}",
                    eipEntity.getEipId(), eipEntity.getSnatId());
        }

        Boolean delQosResult = firewallService.delQos(eipEntity.getPipId(), eipEntity.getFirewallId());
        if(delQosResult) {
            eipEntity.setPipId(null);
        } else {
            log.error("Failed to del qos, eipId:{},pipId:{}",eipEntity.getEipId(), eipEntity.getPipId());
        }

        eipEntity.setStatus("DOWN");
        eipRepository.save(eipEntity);

        return delDnatResult && delSnatResult && delQosResult && delFip;
    }

    @Transactional
    public Eip updateEipEntity(String eipid, EipUpdateParamWrapper param) {

        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            log.error("In disassociate process,failed to find the eip by id:{} ", eipid);
            return null;
        }

        boolean updateStatus = firewallService.updateQosBandWidth(eipEntity.getFirewallId(),
                eipEntity.getPipId(), eipEntity.getEipId(),
                String.valueOf(param.getEipUpdateParam().getBandWidth()));
        if (updateStatus || CommonUtil.isDebug) {
            log.debug("before change：" + eipEntity.getBandWidth());
            eipEntity.setBandWidth(param.getEipUpdateParam().getBandWidth());
            log.debug("after  change：" + eipEntity.getBandWidth());

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
