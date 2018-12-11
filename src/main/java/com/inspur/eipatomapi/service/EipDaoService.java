package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.ExtNetRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.util.*;

import org.apache.http.HttpStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.NetFloatingIP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
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
    public  Eip allocateEip(EipAllocateParam eipConfig, EipPool eip, String portId) throws Exception{


        if (!eip.getState().equals("0")) {
            log.error("Fatal Error! eip state is not free, state:{}.", eip.getState());
            eipPoolRepository.saveAndFlush(eip);
            return null;
        }

        String networkId =  getExtNetId(eipConfig.getRegion());
        if(null == networkId) {
            log.error("Failed to get external net in region:{}. ", eipConfig.getRegion());
            eipPoolRepository.saveAndFlush(eip);
            return null;
        }

        EipPool eipPoolCheck  = eipPoolRepository.findByIp(eip.getIp());
        if(eipPoolCheck != null){
            log.error("==================================================================================");
            log.error("Fatal Error! get a duplicate eip from eip pool, eip_address:{}.", eip.getIp());
            log.error("===================================================================================");
            return null;
        }

        Eip eipEntity = eipRepository.findByEipAddress(eip.getIp());
        if(null != eipEntity){
            log.error("Fatal Error! get a duplicate eip from eip pool, eip_address:{} eipId:{}.",
                    eipEntity.getEipAddress(), eipEntity.getEipId());
            return null;
        }

//        NetFloatingIP floatingIP = neutronService.createFloatingIp(eipConfig.getRegion(), networkId, portId);
//        if (null == floatingIP) {
//            log.error("Fatal Error! Can not get floating ip in network:{}, region:{}, portId:{}.",
//                    networkId, eipConfig.getRegion(), portId);
//            eipPoolRepository.saveAndFlush(eip);
//            return null;
//        }
        Eip eipMo = new Eip();
        eipMo.setEipAddress(eip.getIp());
        eipMo.setStatus(HsConstants.DOWN);
        eipMo.setFirewallId(eip.getFireWallId());

//        eipMo.setFloatingIp(floatingIP.getFloatingIpAddress());
//        eipMo.setPrivateIpAddress(floatingIP.getFixedIpAddress());
//        eipMo.setFloatingIpId(floatingIP.getId());
        eipMo.setIpType(eipConfig.getIptype());
        eipMo.setBillType(eipConfig.getBillType());
        eipMo.setChargeMode(eipConfig.getChargemode());
        eipMo.setDuration(eipConfig.getDuration());
        eipMo.setBandWidth(eipConfig.getBandwidth());
        eipMo.setRegion(eipConfig.getRegion());
        eipMo.setSharedBandWidthId(eipConfig.getSharedBandWidthId());
        String userId = CommonUtil.getUserId();
        log.debug("get tenantid:{} from clientv3", userId);
        //log.debug("get tenantid from token:{}", CommonUtil.getProjectId(eipConfig.getRegion()));
        eipMo.setProjectId(userId);

        eipRepository.saveAndFlush(eipMo);
        log.info("User:{} success allocate eip:{}",userId, eipMo.toString());
        return eipMo;
    }


    @Transactional
    public ActionResponse deleteEip(String  eipid) throws Exception {
        String msg;
        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            msg= "Faild to find eip by id:"+eipid;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        if(!CommonUtil.isAuthoried(eipEntity.getProjectId())){
            log.error(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), eipid);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }

        if ((null != eipEntity.getPipId())
                || (null != eipEntity.getDnatId())
                || (null != eipEntity.getSnatId())) {
            msg = "Failed to delete eip,please unbind eip first."+eipEntity.toString();
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        if(null != eipEntity.getFloatingIpId()) {
            if(!neutronService.deleteFloatingIp(eipEntity.getRegion(),
                    eipEntity.getFloatingIpId(),
                    eipEntity.getInstanceId())){
                msg = "Failed to delete floating ip, floatingIpId:"+eipEntity.getFloatingIpId();
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        eipRepository.deleteById(eipEntity.getEipId());
        EipPool eipPool = eipPoolRepository.findByIp(eipEntity.getEipAddress());
        if(null != eipPool){
            log.error("******************************************************************************");
            log.error("Fatal error, eip has already exist in eip pool. can not add to eip pool.{}",
                    eipEntity.getEipAddress());
            log.error("******************************************************************************");
        }else {
            EipPool eipPoolMo = new EipPool();
            eipPoolMo.setFireWallId(eipEntity.getFirewallId());
            eipPoolMo.setIp(eipEntity.getEipAddress());
            eipPoolMo.setState("0");
            eipPoolRepository.saveAndFlush(eipPoolMo);
        }
        log.info("Success delete eip:{}",eipEntity.getEipAddress());
        return ActionResponse.actionSuccess();
    }

    @Transactional
    public ActionResponse softDownEip(String  eipid) {
        String msg;
        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            msg= "Faild to find eip by id:"+eipid+" ";
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }
        if(!CommonUtil.isAuthoried(eipEntity.getProjectId())){
            log.error(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), eipid);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }
        eipEntity.setStatus(HsConstants.DOWN);
        if (null != eipEntity.getSnatId()) {
            if (firewallService.delSnat(eipEntity.getSnatId(), eipEntity.getFirewallId())) {
                eipEntity.setSnatId(null);
            } else {
                msg = "Failed to delete snat when softDown eip, eip:" + eipEntity.toString();
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }
        eipEntity.setUpdateTime(new Date());
        eipRepository.saveAndFlush(eipEntity);
        return ActionResponse.actionSuccess();
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
            log.error(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), eipid);
            data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
            data.put("httpCode", HttpStatus.SC_FORBIDDEN);
            data.put("interCode", ReturnStatus.SC_FORBIDDEN);
            return data;
        }

        if(!(HsConstants.DOWN.equals(eip.getStatus())) || (null != eip.getDnatId())
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
        }
        ActionResponse actionResponse;
        try{
            actionResponse = neutronService.associaInstanceWithFloatingIp(eip,serverId);
        }catch (Exception e){
            log.error("==========openstack associaInstanceWithFloatingIp error========");
            log.error("==========openstack associaInstanceWithFloatingIp error=====serverId :{},eip :{}",
                   serverId, eip.toString());

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
                    neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId, eip.getRegion());
                    neutronService.deleteFloatingIp(eip.getRegion(), eip.getFloatingIpId(), eip.getInstanceId());
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_DNAT_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_DNAT_UNAVAILABLE);
                    return data;
                }
                log.info("======start snat oprate ");
                snatRuleId = firewallService.addSnat(eip.getFloatingIp(), eip.getEipAddress(), eip.getFirewallId());
                log.info("snatRuleId:  "+snatRuleId);
                if(snatRuleId==null){
                    neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId, eip.getRegion());
                    firewallService.delDnat(dnatRuleId, eip.getFirewallId());
                    neutronService.deleteFloatingIp(eip.getRegion(), eip.getFloatingIpId(),eip.getInstanceId());
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_SNAT_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_SNAT_UNAVAILABLE);
                    return data;
                }

                pipId = firewallService.addQos(eip.getFloatingIp(), eip.getEipAddress(), String.valueOf(eip.getBandWidth()), eip.getFirewallId());
                if(pipId==null && !CommonUtil.qosDebug){
                    neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId, eip.getRegion());
                    firewallService.delDnat(dnatRuleId, eip.getFirewallId());
                    firewallService.delSnat(snatRuleId, eip.getFirewallId());
                    neutronService.deleteFloatingIp(eip.getRegion(), eip.getFloatingIpId(), eip.getInstanceId());
                    data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_QOS_ERROR));
                    data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    data.put("interCode", ReturnStatus.SC_FIREWALL_QOS_UNAVAILABLE);
                    return data;
                }
                eip.setInstanceId(serverId);
                eip.setInstanceType(instanceType);
                eip.setDnatId(dnatRuleId);
                eip.setSnatId(snatRuleId);
                eip.setPipId(pipId);
                eip.setPortId(portId);
                eip.setStatus(HsConstants.ACTIVE);
                eip.setUpdateTime(new Date());
                eipRepository.saveAndFlush(eip);
                data.put("reason",HsConstants.SUCCESS);
                data.put("httpCode", HttpStatus.SC_OK);
                data.put("interCode", ReturnStatus.SC_OK);
                data.put("data",eip);
                return data;
            }catch (Exception e){
                log.error("band server firewall exception",e);
                neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId, eip.getRegion());
                neutronService.deleteFloatingIp(eip.getRegion(), eip.getFloatingIpId(), eip.getInstanceId());
                data.put("reason",CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_UNAVAILABLE);
                return data;
            }

        } else {
            log.error("Failed to associate port:{} with eip:{}, serverId:{} ", portId, eipid, serverId);
            neutronService.deleteFloatingIp(eip.getRegion(), eip.getFloatingIpId(), eip.getInstanceId());
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
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }

        if(!(eipEntity.getStatus().equals(HsConstants.ACTIVE)) || (null == eipEntity.getSnatId())
                || (null == eipEntity.getDnatId()) || null == eipEntity.getFloatingIp()){
            msg = "Error status when disassociate eip:"+eipEntity.toString();
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_ACCEPTABLE);
        }

        if(null != eipEntity.getFloatingIp() && null != eipEntity.getInstanceId()) {
            ActionResponse actionResponse = neutronService.disassociateInstanceWithFloatingIp(eipEntity.getFloatingIp(),
                    eipEntity.getInstanceId(), eipEntity.getRegion());
            if (actionResponse.isSuccess()) {
                eipEntity.setInstanceId(null);
                eipEntity.setInstanceType(null);
                eipEntity.setPrivateIpAddress(null);
                eipEntity.setPortId(null);
                if(neutronService.deleteFloatingIp(eipEntity.getRegion(),
                                                   eipEntity.getEipId(),
                                                   eipEntity.getInstanceId())){
                    eipEntity.setFloatingIp(null);
                    eipEntity.setFloatingIpId(null);
                }
            }else {
                msg = "Failed to disassociate port with fip:"+eipEntity.toString();
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

        eipEntity.setStatus(HsConstants.DOWN);
        eipEntity.setUpdateTime(new Date());
        eipRepository.saveAndFlush(eipEntity);
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
        if(!CommonUtil.isAuthoried(eipEntity.getProjectId())){
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
        if (updateStatus ||CommonUtil.qosDebug) {
            eipEntity.setBandWidth(param.getEipUpdateParam().getBandWidth());
            eipEntity.setBillType(param.getEipUpdateParam().getBillType());
            eipEntity.setUpdateTime(new Date());
            eipRepository.saveAndFlush(eipEntity);
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
        if (null == eipEntity) {
            return ActionResponse.actionFailed("Can not find the eip by id:{}"+eipId, HttpStatus.SC_NOT_FOUND);
        }
        String oldTime = eipEntity.getDuration();
        int newTime = Integer.valueOf(addTime) + Integer.valueOf(oldTime);
        eipEntity.setDuration(String.valueOf(newTime));
        if((newTime > 0) && (null ==eipEntity.getSnatId()) && (null != eipEntity.getDnatId())){
            String snatRuleId = firewallService.addSnat(eipEntity.getFloatingIp(), eipEntity.getEipAddress(),
                    eipEntity.getFirewallId());
            eipEntity.setSnatId(snatRuleId);
            log.info("renew eip entity add snat, id:{}.  ",snatRuleId);
            eipEntity.setStatus(HsConstants.ACTIVE);
        }

        eipRepository.saveAndFlush(eipEntity);
        return ActionResponse.actionSuccess();
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

        Map<String, Object> map=jdbcTemplate.queryForMap(sql);
        long num =(long)map.get("num");
        log.info("{}, result:{}",sql, num);

        return num;

    }

    @Transactional(isolation= Isolation.SERIALIZABLE)
    public synchronized EipPool getOneEipFromPool(){
        EipPool eipAddress =  eipPoolRepository.getEipByRandom();
        if(null != eipAddress) {
            eipPoolRepository.deleteById(eipAddress.getId());
            eipPoolRepository.flush();
        }
        return eipAddress;
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


    public Map<String, Object> getDuplicateEip(){

        //TODO  get table name and colum name by entityUtil
        String sql ="select eip_address, count(*) as num from eip group by eip_address having num>1";


        Map<String, Object> map=jdbcTemplate.queryForMap(sql);

        log.info("{}", map);

        return map;

    }

    public Map<String, Object> getDuplicateEipFromPool(){

        //TODO  get table name and colum name by entityUtil
        String sql ="select ip, count(*) as num from eip_pool group by ip having num>1";

        Map<String, Object> map=jdbcTemplate.queryForMap(sql);

        log.info("{}, result:{}",sql, map);

        return map;

    }

    /**
     * associate port with eip
     * @param eipId          eip
     * @param slbId     slb id
     * @param ipAddr    ip
     * @return             true or false
     * @throws Exception   e
     */
    @Transactional
    public JSONObject associateSlbWithEip(String eipId, String slbId, String ipAddr)
            throws Exception {

        JSONObject data = new JSONObject();
        Eip eip = eipRepository.findByEipId(eipId);
        String eipIp = eip.getEipAddress();
        if (null == eip) {
            log.error("In associate process, failed to find the eip by id:{} ", eipId);
            data.put("reason", CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
            data.put("httpCode", HttpStatus.SC_NOT_FOUND);
            data.put("interCode", ReturnStatus.SC_NOT_FOUND);
            return data;
        }
        if (!eip.getProjectId().equals(CommonUtil.getUserId())) {
            log.error("User have no write to operate eip:{}", eipId);
            data.put("reason", CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
            data.put("httpCode", HttpStatus.SC_FORBIDDEN);
            data.put("interCode", ReturnStatus.SC_FORBIDDEN);
            return data;
        }

        if (!("DOWN".equals(eip.getStatus())) || (null != eip.getDnatId())
                || (null != eip.getSnatId()) || (null != eip.getPipId())) {
            data.put("reason", CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_HAS_BAND));
            data.put("httpCode", HttpStatus.SC_BAD_REQUEST);
            data.put("interCode", ReturnStatus.EIP_BIND_HAS_BAND);
            return data;
        }
        if (slbId == null) {
            data.put("reason", CodeInfo.getCodeMessage(CodeInfo.SLB_BIND_NOT_FOND));
            data.put("httpCode", HttpStatus.SC_NOT_FOUND);
            data.put("interCode", ReturnStatus.SC_NOT_FOUND);
            return data;
        }

        String pipId;
        String dnatRuleId = null;
        String snatRuleId = null;
        try {
            log.info("======start dnat oprate ");
            dnatRuleId = firewallService.addDnat(ipAddr, eipIp, eip.getFirewallId());
            log.info("dnatRuleId:  " + dnatRuleId);
            if (dnatRuleId == null) {
                data.put("reason", CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_DNAT_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_DNAT_UNAVAILABLE);
                return data;
            }
            log.info("======start snat oprate ");
            snatRuleId = firewallService.addSnat(ipAddr, eipIp, eip.getFirewallId());
            log.info("snatRuleId:  " + snatRuleId);
            if (snatRuleId == null) {
                firewallService.delDnat(dnatRuleId, eip.getFirewallId());
                data.put("reason", CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_SNAT_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_SNAT_UNAVAILABLE);
                return data;
            }

            pipId = firewallService.addQos(ipAddr, eip.getEipAddress(), String.valueOf(eip.getBandWidth()), eip.getFirewallId());
            if(pipId==null ){
                firewallService.delDnat(dnatRuleId, eip.getFirewallId());
                firewallService.delSnat(snatRuleId, eip.getFirewallId());

                data.put("reason", CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_QOS_ERROR));
                data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                data.put("interCode", ReturnStatus.SC_FIREWALL_QOS_UNAVAILABLE);
                return data;
            }

            eip.setDnatId(dnatRuleId);
            eip.setSnatId(snatRuleId);
            eip.setPipId(pipId);
            eip.setStatus("ACTIVE");
            eip.setInstanceType("3");
            eip.setInstanceId(slbId);
            eip.setPrivateIpAddress(ipAddr);
            eipRepository.saveAndFlush(eip);
            data.put("reason", "success");
            data.put("httpCode", HttpStatus.SC_OK);
            data.put("interCode", ReturnStatus.SC_OK);
            data.put("data", eip);
            return data;

        } catch (Exception e) {
            log.error("band server firewall exception", e);
            data.put("reason", CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_ERROR));
            data.put("httpCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            data.put("interCode", ReturnStatus.SC_FIREWALL_UNAVAILABLE);
            return data;
        }
    }


    /**
     * associate port with eip
     * @param slbId          slbid
     * @return             true or false
     * @throws Exception   e
     */
    @Transactional
    public ActionResponse disassociateSlbWithEip(String slbId) throws Exception  {

        String msg = null;
        Eip eipEntity = eipRepository.findByInstanceId(slbId);

        if(null == eipEntity){
            log.error("In disassociate process,failed to find the eip by id:{} ",slbId);
            return ActionResponse.actionFailed("Not found.", HttpStatus.SC_NOT_FOUND);
        }
        if(!eipEntity.getProjectId().equals(CommonUtil.getUserId())){
            log.error("User have no write to delete eip:{}", slbId);
            return ActionResponse.actionFailed("Forbiden.", HttpStatus.SC_FORBIDDEN);
        }

        if(!(eipEntity.getStatus().equals("ACTIVE")) || (null == eipEntity.getSnatId())
                || (null == eipEntity.getDnatId()) ){
            msg = "Error status when disassociate eip , slbId: "+slbId+ " status : "+eipEntity.getStatus()+
                    " snatId : "+eipEntity.getSnatId()+" dnatId : "+eipEntity.getDnatId();
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_ACCEPTABLE);
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
        eipEntity.setInstanceId(null);
        eipEntity.setPrivateIpAddress(null);
        eipEntity.setInstanceType(null);


        eipEntity.setStatus("DOWN");
        eipRepository.saveAndFlush(eipEntity);
        if(null != msg) {
            return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }else {
            return ActionResponse.actionSuccess();
        }
    }


}
