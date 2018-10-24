package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.*;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.ExtNetRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.EIPChargeType;
import com.inspur.eipatomapi.util.EntityUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import org.apache.http.HttpStatus;
import org.hibernate.query.Query;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.NetFloatingIP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@Service
public class EipDaoService {
    @Autowired
    private EipPoolRepository eipPoolRepository;

    @Autowired
    private FirewallRepository firewallRepository;

    @Autowired
    private ExtNetRepository extNetRepository;

    @Autowired
    private EipRepository eipRepository;

    @Autowired
    private FirewallService firewallService;

    @Autowired
    private NeutronService neutronService;


    @Autowired
    private JdbcTemplate jdbcTemplate;

    public final static Logger log = LoggerFactory.getLogger(EipDaoService.class);
    /**
     * allocate eip
     *
     * @param eipConfig    eipconfig
     * @return result
     */
    @Transactional
    public Eip allocateEip(EipAllocateParam eipConfig, String portId) throws Exception{

        EipPool eip = getOneEipFromPool();

        if(null != eip){
            if (eip.getState().equals("0")) {
                Eip eipMo = new Eip();
                eipMo.setEipAddress(eip.getIp());
                eipMo.setStatus("DOWN");
                eipMo.setFirewallId(eip.getFireWallId());
                String networkId =  getExtNetId(eipConfig.getRegion());
                if(null != networkId) {
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
                        log.debug("get tenantid:{} from clientv3", tenantid);
                        log.debug("get tenantid from token:{}", CommonUtil.getProjectId());
                        eipMo.setProjectId(tenantid);

                        eipPoolRepository.delete(eip);
                        eipMo = eipRepository.save(eipMo);
                        return eipMo;
                    }
                }else {
                    log.error("Failed to get external net in region:{}. ", eipConfig.getRegion());
                }
            }
        }else{
            log.error("Failed to allocate eip in eip pool.");
        }


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
            try{
                log.info("======start dnat oprate ");
                dnatRuleId = firewallService.addDnat(eip.getFloatingIp(), eip.getEipAddress(), eip.getFirewallId());
                log.info("dnatRuleId:  "+dnatRuleId);
                if(dnatRuleId==null){
                    neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
                    data.put("flag",false);
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_DNAT_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_DNAT_UNAVAILABLE);
                    data.put("data",null);
                    return data;
                }
                log.info("======start snat oprate ");
                snatRuleId = firewallService.addSnat(eip.getFloatingIp(), eip.getEipAddress(), eip.getFirewallId());
                log.info("snatRuleId:  "+snatRuleId);
                if(snatRuleId==null){
                    neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
                    if(dnatRuleId!=null){
                        firewallService.delDnat(dnatRuleId, eip.getFirewallId());
                    }
                    data.put("flag",false);
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_SNAT_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_SNAT_UNAVAILABLE);
                    data.put("data",null);
                    return data;
                }

                log.info("=======start qos oprate ");
                pipId = firewallService.addQos(eip.getFloatingIp(), eip.getEipAddress(), String.valueOf(eip.getBandWidth()), eip.getFirewallId());
                log.info("qos:  "+pipId);
                if(pipId==null){
                    neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
                    if(dnatRuleId!=null){
                        firewallService.delDnat(dnatRuleId, eip.getFirewallId());
                    }
                    if(snatRuleId!=null){
                        firewallService.delSnat(snatRuleId, eip.getFirewallId());
                    }
                    data.put("flag",false);
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_QOS_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_QOS_UNAVAILABLE);
                    data.put("data",null);
                    return data;
                }
                if(dnatRuleId!=null&&snatRuleId!=null&&pipId!=null){
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
                }else{
                    neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
                    if(dnatRuleId!=null){
                        firewallService.delDnat(dnatRuleId, eip.getFirewallId());
                    }
                    if(snatRuleId!=null){
                        firewallService.delSnat(snatRuleId, eip.getFirewallId());
                    }
                    if(pipId!=null){
                        firewallService.delQos(pipId,eip.getFirewallId());
                    }
                    data.put("flag",false);
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_UNAVAILABLE);
                    data.put("data",null);
                    return data;
                }
            }catch (Exception e){
                log.error("band server firewall error");
                e.printStackTrace();
                data.put("flag",false);
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_UNAVAILABLE);
                data.put("data",null);
                return data;
            }

        } else {
            log.error("Failed to associate port:{} with eip:{}, serverId:{} ", portId, eipid, serverId);
            data.put("flag",false);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_OPENSTACK_ASSOCIA_FAIL)+actionResponse.getFault());
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
            return false;
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
    public JSONObject updateEipEntity(String eipid, EipUpdateParamWrapper param) {

        JSONObject data=new JSONObject();
        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            log.error("In disassociate process,failed to find the eip by id:{} ", eipid);
            data.put("flag",false);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
            data.put("httpCode", HttpStatus.SC_NOT_FOUND);
            data.put("interCode", ReturnStatus.SC_NOT_FOUND);
            data.put("data",null);
            return data;
        }
        if(param.getEipUpdateParam().getChargeType().equals(EIPChargeType.EIP_CHARGETYPE_PREPAID)){
            //can’t sub
            if(param.getEipUpdateParam().getBandWidth()<eipEntity.getBandWidth()){
                data.put("flag",false);
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR));
                data.put("httpCode", HttpStatus.SC_BAD_REQUEST);
                data.put("interCode", ReturnStatus.SC_PARAM_ERROR);
                data.put("data",null);
                return data;
            }
        }
        boolean updateStatus = firewallService.updateQosBandWidth(eipEntity.getFirewallId(), eipEntity.getPipId(), eipEntity.getEipId(), String.valueOf(param.getEipUpdateParam().getBandWidth()));
        if (updateStatus) {
            eipEntity.setBandWidth(param.getEipUpdateParam().getBandWidth());
            eipEntity.setChargeType(param.getEipUpdateParam().getChargeType());
            eipRepository.save(eipEntity);
            data.put("flag",true);
            data.put("reason","");
            data.put("httpCode", HttpStatus.SC_OK);
            data.put("interCode", ReturnStatus.SC_OK);
            data.put("data",eipEntity);
            return data;
        }else{
            data.put("flag",false);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_CHANGE_BANDWIDTH_ERROR));
            data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            data.put("interCode", ReturnStatus.SC_FIREWALL_SERVER_ERROR);
            data.put("data",null);
            return data;
        }

    }

    public List<Eip> findByProjectId(String projectId){
        return eipRepository.findByProjectId(projectId);
    }

    public long getInstanceNum(String projectId){

        //TODO  get table name and colum name by entityUtil
        String sql ="select count(1) as num from eip where project_id='"+projectId+"'";
        log.info(sql);
        Map<String, Object> map=jdbcTemplate.queryForMap(sql);
        log.info("{}",map);
        long num =(long)map.get("num");
        log.info("get count use jdbc {}",num);

        //demo do't use this type, default value must be set value;
        Eip eip  = new Eip();
        eip.setCreateTime(null);
        eip.setStatus(null);
        eip.setIpVersion("IPv4");
        eip.setChargeType("");
        eip.setProjectId(projectId);
        Example<Eip> example = Example.of(eip);
        long count=eipRepository.count(example);
        log.info("get count use jdbc {}",count);

        return num;


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


    private String getExtNetId(String region){
        List<ExtNet> extNets = extNetRepository.findByRegion(region);
        String extNetId = null;
        for(ExtNet extNet: extNets){
            if(null != extNet.getNetId()){
                extNetId = extNet.getNetId();
            }
        }
        return extNetId;
    }
}
