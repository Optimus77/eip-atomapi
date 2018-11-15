package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.entity.fw.Firewall;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.ExtNetRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.util.*;

import org.apache.http.HttpStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.NetFloatingIP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        if(null == eip) {
            log.error("Failed to allocate eip in eip pool.");
            return null;
        }
        String networkId =  getExtNetId(eipConfig.getRegion());
        if(null == networkId) {
            log.error("Failed to get external net in region:{}. ", eipConfig.getRegion());
            return null;
        }
        Eip eipEntity = eipRepository.findByEipAddress(eip.getIp());
        if(null != eipEntity){
            log.error("Fatal Error! get a duplicate eip from eip pool, eip:{}.",
                    eipEntity.getEipAddress(), eipEntity.getEipId());
            return null;
        }
        if (!eip.getState().equals("0")) {
            log.error("Fatal Error! eip state is not free, state:{}.", eip.getState());
            return null;
        }

        NetFloatingIP floatingIP = neutronService.createFloatingIp(eipConfig.getRegion(), networkId, portId);
        if (null == floatingIP) {
            log.error("Fatal Error! Can not get floating ip in network:{}, region:{}, portId:{}.",
                    networkId, eipConfig.getRegion(), portId);
            return null;
        }
        Eip eipMo = new Eip();
        eipMo.setEipAddress(eip.getIp());
        eipMo.setStatus("DOWN");
        eipMo.setFirewallId(eip.getFireWallId());

        eipMo.setFloatingIp(floatingIP.getFloatingIpAddress());
        eipMo.setPrivateIpAddress(floatingIP.getFixedIpAddress());
        eipMo.setFloatingIpId(floatingIP.getId());
        eipMo.setIpType(eipConfig.getIptype());
        eipMo.setBillType(eipConfig.getBillType());
        eipMo.setChargeMode(eipConfig.getChargemode());
        eipMo.setDuration(eipConfig.getDuration());
        eipMo.setBandWidth(eipConfig.getBandwidth());
        eipMo.setRegion(eipConfig.getRegion());
        eipMo.setSharedBandWidthId(eipConfig.getSharedBandWidthId());
        String userId = CommonUtil.getUserId();
        log.debug("get tenantid:{} from clientv3", userId);
        log.debug("get tenantid from token:{}", CommonUtil.getProjectId());
        eipMo.setProjectId(userId);

        eipPoolRepository.delete(eip);
        eipMo = eipRepository.save(eipMo);
        return eipMo;
    }


    @Transactional
    public ActionResponse deleteEip(String  eipid) throws Exception {
        String msg;
        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            msg= "Faild to find eip by id:%s"+eipid;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        if(!eipEntity.getProjectId().equals(CommonUtil.getUserId())){
            log.error("User have no write to delete eip:{}", eipid);
            return ActionResponse.actionFailed("Forbiden.", HttpStatus.SC_FORBIDDEN);
        }

        if ((null != eipEntity.getPipId())
                || (null != eipEntity.getDnatId())
                || (null != eipEntity.getSnatId())) {
            msg = "Failed to delete eip,status error.eipId:"+eipEntity.getEipId()+"pipId:"+eipEntity.getPipId()+
                    "dnatId:"+ eipEntity.getDnatId()+"snatid:"+eipEntity.getSnatId()+"";
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
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
            return ActionResponse.actionSuccess();
        } else {
            msg = "Failed to delete floating ip, floatingIpId:"+eipEntity.getFloatingIpId();
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public ActionResponse softDownEip(String  eipid) throws Exception {
        String msg;
        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            msg= "Faild to find eip by id:"+eipid+" ";
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        if(!eipEntity.getProjectId().equals(CommonUtil.getUserId())){
            log.error("User have no write to delete eip:{}", eipid);
            return ActionResponse.actionFailed("Forbiden.", HttpStatus.SC_FORBIDDEN);
        }

        if (null == eipEntity.getSnatId()) {
            msg = "Failed to softDown eip,status error.eipId:"+eipEntity.getEipId()+"pipId:"+eipEntity.getPipId()+
                    "dnatId:"+ eipEntity.getDnatId()+"snatid:"+eipEntity.getSnatId()+" ";
            log.error(msg);
            return ActionResponse.actionSuccess();
        }
        if(firewallService.delSnat(eipEntity.getSnatId(), eipEntity.getFirewallId())){
            eipEntity.setStatus("DOWN");
            eipEntity.setSnatId(null);
            eipRepository.save(eipEntity);
            return ActionResponse.actionSuccess();
        } else {
            msg = "Failed to soft down eip in firewall, eipId:"+eipEntity.getEipId()+"snatId:"+eipEntity.getSnatId()+" ";
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
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
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
            data.put("httpCode", HttpStatus.SC_NOT_FOUND);
            data.put("interCode", ReturnStatus.SC_NOT_FOUND);
            return data;
        }
        if(!eip.getProjectId().equals(CommonUtil.getUserId())){
            log.error("User have no write to operate eip:{}", eipid);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
            data.put("httpCode", HttpStatus.SC_FORBIDDEN);
            data.put("interCode", ReturnStatus.SC_FORBIDDEN);
            return data;
        }

        if(!(eip.getStatus().equals("DOWN")) || (null != eip.getDnatId())
                || (null != eip.getSnatId()) || (null != eip.getPipId())){
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_HAS_BAND));
            data.put("httpCode", HttpStatus.SC_BAD_REQUEST);
            data.put("interCode", ReturnStatus.EIP_BIND_HAS_BAND);
            return data;
        }
        if(serverId==null){
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_PARA_SERVERID_ERROR));
            data.put("httpCode", HttpStatus.SC_BAD_REQUEST);
            data.put("interCode", ReturnStatus.SC_PARAM_ERROR);
            return data;
        }
        if(eip.getFloatingIpId() == null && eip.getFloatingIp() == null) {
            String networkId =  getExtNetId(eip.getRegion());
            if(null == networkId) {
                log.error("Failed to get external net in region:{}. ", eip.getRegion());
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_OPENSTACK_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_OPENSTACK_FIP_UNAVAILABLE);
                return data;
            }
            NetFloatingIP floatingIP = neutronService.createFloatingIp(eip.getRegion(), networkId, portId);
            if (null == floatingIP) {
                log.error("Fatal Error! Can not get floating when bind ip in network:{}, region:{}, portId:{}.",
                        networkId, eip.getRegion(), portId);
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_OPENSTACK_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_OPENSTACK_FIP_UNAVAILABLE);
                return data;
            }
            eip.setFloatingIpId(floatingIP.getId());
            eip.setFloatingIp(floatingIP.getFloatingIpAddress());
            eipRepository.save(eip);
        }
        ActionResponse actionResponse;
        try{
            actionResponse = neutronService.associaInstanceWithFloatingIp(eip,serverId);
        }catch (Exception e){
            log.error("==========openstack associaInstanceWithFloatingIp error=====serverId :{}",serverId);
            log.error("==========openstack associaInstanceWithFloatingIp error=====eip :{}",eip.getFloatingIp());

            log.error("Exception in associateInstanceWithEip",e);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_OPENSTACK_ERROR));
            data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            data.put("interCode", ReturnStatus.SC_OPENSTACK_SERVER_ERROR);
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
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_DNAT_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_DNAT_UNAVAILABLE);
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
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_SNAT_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_SNAT_UNAVAILABLE);
                    return data;
                }

                pipId = firewallService.addQos(eip.getFloatingIp(), eip.getEipAddress(), String.valueOf(eip.getBandWidth()), eip.getFirewallId());
                if(pipId==null){
                    neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
                    if(dnatRuleId!=null){
                        firewallService.delDnat(dnatRuleId, eip.getFirewallId());
                    }
                    if(snatRuleId!=null){
                        firewallService.delSnat(snatRuleId, eip.getFirewallId());
                    }
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_QOS_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_QOS_UNAVAILABLE);
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
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_UNAVAILABLE);
                    return data;
                }
            }catch (Exception e){
                log.error("band server firewall exception",e);

                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_UNAVAILABLE);
                return data;
            }

        } else {
            log.error("Failed to associate port:{} with eip:{}, serverId:{} ", portId, eipid, serverId);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_OPENSTACK_ASSOCIA_FAIL)+actionResponse.getFault());
            data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            data.put("interCode", ReturnStatus.SC_OPENSTACK_SERVER_ERROR);
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
    public ActionResponse disassociateInstanceWithEip(String eipid) throws Exception  {

        String msg = null;
        Eip eipEntity = eipRepository.findByEipId(eipid);
        if(null == eipEntity){
            log.error("In disassociate process,failed to find the eip by id:{} ",eipid);
            return ActionResponse.actionFailed("Not found.", HttpStatus.SC_NOT_FOUND);
        }
        if(!eipEntity.getProjectId().equals(CommonUtil.getUserId())){
            log.error("User have no write to delete eip:{}", eipid);
            return ActionResponse.actionFailed("Forbiden.", HttpStatus.SC_FORBIDDEN);
        }

        if(!(eipEntity.getStatus().equals("ACTIVE")) || (null == eipEntity.getSnatId())
                || (null == eipEntity.getDnatId()) || null == eipEntity.getFloatingIp()){
            msg = "Error status when disassociate eip,eipId:"+eipid+ "status:"+eipEntity.getStatus()+
                    "snatId:"+eipEntity.getSnatId()+"dnatId:"+eipEntity.getDnatId()+"fipId:"+eipEntity.getFloatingIp()+"";
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_ACCEPTABLE);
        }

        if(null != eipEntity.getFloatingIp() && null != eipEntity.getInstanceId()) {
            ActionResponse actionResponse = neutronService.disassociateInstanceWithFloatingIp(eipEntity.getFloatingIp(),
                    eipEntity.getInstanceId());
            if (actionResponse.isSuccess()) {
                eipEntity.setInstanceId(null);
                eipEntity.setInstanceType(null);
                eipEntity.setPrivateIpAddress(null);
            }else {
                msg = "Failed to disassociate port with fip,eipId:"+eipEntity.getEipId()+
                        "floatingip:"+eipEntity.getFloatingIp()+ "instanceId:"+eipEntity.getInstanceId()+"";
                log.error(msg);
            }
        }

        Boolean delDnatResult = firewallService.delDnat(eipEntity.getDnatId(), eipEntity.getFirewallId());
        if (delDnatResult) {
            eipEntity.setDnatId(null);
        } else {
            msg = "Failed to del dnat in firewall,eipId:"+eipEntity.getEipId()+"dnatId:"+eipEntity.getDnatId()+"";
            log.error(msg);
        }

        Boolean delSnatResult = firewallService.delSnat(eipEntity.getSnatId(), eipEntity.getFirewallId());
        if (delSnatResult) {
            eipEntity.setSnatId(null);
        } else {
            msg = "Failed to del snat in firewall, eipId:"+eipEntity.getEipId()+"snatId:"+eipEntity.getSnatId()+"";
            log.error(msg);
        }

        Boolean delQosResult = firewallService.delQos(eipEntity.getPipId(), eipEntity.getFirewallId());
        if(delQosResult) {
            eipEntity.setPipId(null);
        } else {
            msg = "Failed to del qos, eipId:"+eipEntity.getEipId()+"pipId:"+eipEntity.getPipId()+"";
            log.error(msg);
        }

        eipEntity.setStatus("DOWN");
        eipRepository.save(eipEntity);
        if(null != msg) {
            return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }else {
            return ActionResponse.actionSuccess();
        }
    }

    @Transactional
    public JSONObject updateEipEntity(String eipid, EipUpdateParamWrapper param) throws KeycloakTokenException {

        JSONObject data=new JSONObject();
        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            log.error("In disassociate process,failed to find the eip by id:{} ", eipid);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
            data.put("httpCode", HttpStatus.SC_NOT_FOUND);
            data.put("interCode", ReturnStatus.SC_NOT_FOUND);
            return data;
        }
        if(!eipEntity.getProjectId().equals(CommonUtil.getUserId())){
            log.error("User have no write to operate eip:{}", eipid);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
            data.put("httpCode", HttpStatus.SC_FORBIDDEN);
            data.put("interCode", ReturnStatus.SC_FORBIDDEN);
            return data;
        }
        if(param.getEipUpdateParam().getBillType().equals(HsConstants.MONTHLY)){
            //can’t sub
            if(param.getEipUpdateParam().getBandWidth()<eipEntity.getBandWidth()){
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR));
                data.put("httpCode", HttpStatus.SC_BAD_REQUEST);
                data.put("interCode", ReturnStatus.SC_PARAM_ERROR);
                return data;
            }
        }
        boolean updateStatus = firewallService.updateQosBandWidth(eipEntity.getFirewallId(), eipEntity.getPipId(), eipEntity.getEipId(), String.valueOf(param.getEipUpdateParam().getBandWidth()));
        if (updateStatus) {
            eipEntity.setBandWidth(param.getEipUpdateParam().getBandWidth());
            eipEntity.setBillType(param.getEipUpdateParam().getBillType());
            eipRepository.save(eipEntity);
            data.put("reason","");
            data.put("httpCode", HttpStatus.SC_OK);
            data.put("interCode", ReturnStatus.SC_OK);
            data.put("data",eipEntity);
            return data;
        }else{
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_CHANGE_BANDWIDTH_ERROR));
            data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            data.put("interCode", ReturnStatus.SC_FIREWALL_SERVER_ERROR);
            return data;
        }

    }

    @Transactional
    public ActionResponse reNewEipEntity(String eipId, String addTime)  {

        Eip eipEntity = eipRepository.findByEipId(eipId);
        if (null != eipEntity) {
            String oldTime = eipEntity.getDuration();
            int newTime = Integer.valueOf(addTime) + Integer.valueOf(oldTime);
            eipEntity.setDuration(String.valueOf(newTime));
            eipRepository.save(eipEntity);
            return ActionResponse.actionSuccess();
        }
        return ActionResponse.actionFailed("Can not find the eip by id:{}"+eipId, HttpStatus.SC_NOT_FOUND);
    }

    public List<Eip> findByProjectId(String projectId){
        return eipRepository.findByProjectId(projectId);
    }

    public  Eip findByEipAddress(String eipAddr){
        return eipRepository.findByEipAddress(eipAddr);
    }

    public Eip findByInstanceId(String instanceId) {
        return eipRepository.findByInstanceId(instanceId);
    }

    public Eip getEipById(String id){

        Eip eipEntity = null;
        Optional<Eip> eip = eipRepository.findById(id);
        if (eip.isPresent()) {
            eipEntity = eip.get();
        }

        return eipEntity;
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
        eip.setBillType("");
        eip.setProjectId(projectId);
        Example<Eip> example = Example.of(eip);
        long count=eipRepository.count(example);
        log.info("get count use jdbc {}",count);

        return num;


    }

    public void addEipPool(String ip, String eip) {

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

        for (int i = 0; i < 200; i++) {
            EipPool eipPoolMo = new EipPool();
            eipPoolMo.setFireWallId(id);
            eipPoolMo.setIp(eip+i);
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
