package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.MethodReturn;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.entity.eipv6.EipPoolV6;
import com.inspur.eipatomapi.entity.eipv6.EipV6;
import com.inspur.eipatomapi.entity.eipv6.NatPtV6;
import com.inspur.eipatomapi.repository.*;
import com.inspur.eipatomapi.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.NetFloatingIP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
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
    private EipV6Repository eipV6Repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EipV6DaoService eipV6DaoService;

    @Autowired
    private NatPtService natPtService;

    @Autowired
    private EipPoolV6Repository eipPoolV6Repository;


    /**
     * allocate eip
     *
     * @param eipConfig eipconfig
     * @return result
     */
    @Transactional
    public Eip allocateEip(EipAllocateParam eipConfig, EipPool eip, String portId) throws KeycloakTokenException {


        if (!eip.getState().equals("0")) {
            log.error("Fatal Error! eip state is not free, state:{}.", eip.getState());
            eipPoolRepository.saveAndFlush(eip);
            return null;
        }

        EipPool eipPoolCheck = eipPoolRepository.findByIp(eip.getIp());
        if (eipPoolCheck != null) {
            log.error("==================================================================================");
            log.error("Fatal Error! get a duplicate eip from eip pool, eip_address:{}.", eip.getIp());
            log.error("===================================================================================");
            eipPoolRepository.deleteById(eipPoolCheck.getId());
            eipPoolRepository.flush();
        }

        Eip eipEntity = eipRepository.findByEipAddressAndIsDelete(eip.getIp(), 0);
        if (null != eipEntity) {
            log.error("Fatal Error! get a duplicate eip from eip pool, eip_address:{} eipId:{}.",
                    eipEntity.getEipAddress(), eipEntity.getEipId());
            return null;
        }

        Eip eipMo = new Eip();
        eipMo.setEipAddress(eip.getIp());
        eipMo.setStatus(HsConstants.DOWN);
        eipMo.setFirewallId(eip.getFireWallId());

        eipMo.setIpType(eipConfig.getIptype());
        eipMo.setBillType(eipConfig.getBillType());
        eipMo.setChargeMode(eipConfig.getChargemode());
        eipMo.setDuration(eipConfig.getDuration());
        eipMo.setBandWidth(eipConfig.getBandwidth());
        eipMo.setRegion(eipConfig.getRegion());
        eipMo.setSharedBandWidthId(eipConfig.getSharedBandWidthId());
        String userId = CommonUtil.getUserId();
        log.debug("get tenantid:{} from clientv3", userId);
        eipMo.setUserId(userId);
        eipMo.setProjectId(CommonUtil.getProjectName());
        eipMo.setIsDelete(0);

        eipMo.setCreateTime(CommonUtil.getGmtDate());
        eipRepository.saveAndFlush(eipMo);
        log.info("User:{} success allocate eip:{}", userId, eipMo.getEipId());
        return eipMo;
    }


    @Transactional
    public ActionResponse deleteEip(String eipid) throws KeycloakTokenException {
        String msg;
        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            msg = "Faild to find eip by id:" + eipid;
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_FOUND);
        }

        if (!CommonUtil.isAuthoried(eipEntity.getUserId())) {
            log.error(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), eipid);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }

        if ((null != eipEntity.getPipId())
                || (null != eipEntity.getDnatId())
                || (null != eipEntity.getSnatId())) {
            msg = "Failed to delete eip,please unbind eip first." + eipEntity.toString();
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        if (null != eipEntity.getFloatingIpId() && !neutronService.deleteFloatingIp(eipEntity.getRegion(),
                eipEntity.getFloatingIpId(),
                eipEntity.getInstanceId())) {
            msg = "Failed to delete floating ip, floatingIpId:" + eipEntity.getFloatingIpId();
            log.error(msg);
        }
        String eipAddress = eipEntity.getEipAddress();
        EipV6 eipV6 = eipV6Repository.findByIpv4AndUserIdAndIsDelete(eipAddress, eipEntity.getUserId(), 0);
        if (eipV6 != null) {
            eipV6.setIsDelete(1);
            eipV6.setUpdateTime(CommonUtil.getGmtDate());
            eipV6Repository.saveAndFlush(eipV6);
            EipPoolV6 eipV6Pool = eipPoolV6Repository.findByIp(eipV6.getIpv6());
            if (null != eipV6Pool) {
                log.error("******************************************************************************");
                log.error("Fatal error, eipV6 has already exist in eipV6 pool. can not add to eipV6 pool.{}",
                        eipV6.getIpv6());
                log.error("******************************************************************************");
            } else {
                EipPoolV6 eipPoolV6Mo = new EipPoolV6();
                eipPoolV6Mo.setFireWallId(eipV6.getFirewallId());
                eipPoolV6Mo.setIp(eipV6.getIpv6());
                eipPoolV6Mo.setState("0");
                eipPoolV6Repository.saveAndFlush(eipPoolV6Mo);
                log.info("Success delete eipV6:{}", eipV6.getIpv6());
            }
        }
        eipEntity.setIsDelete(1);
        eipEntity.setUpdateTime(CommonUtil.getGmtDate());
        eipEntity.setEipV6Id(null);
        eipRepository.saveAndFlush(eipEntity);
        if(eipEntity.getStatus().equals(HsConstants.ERROE)){
            return ActionResponse.actionSuccess();
        }
        EipPool eipPool = eipPoolRepository.findByIp(eipEntity.getEipAddress());
        if (null != eipPool) {
            log.error("******************************************************************************");
            log.error("Fatal error, eip has already exist in eip pool. can not add to eip pool.{}",
                    eipEntity.getEipAddress());
            log.error("******************************************************************************");
        } else {
            EipPool eipPoolMo = new EipPool();
            eipPoolMo.setFireWallId(eipEntity.getFirewallId());
            eipPoolMo.setIp(eipEntity.getEipAddress());
            eipPoolMo.setState("0");
            eipPoolRepository.saveAndFlush(eipPoolMo);
            log.info("Success delete eip:{}", eipEntity.getEipAddress());
        }

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
        if(StringUtils.isNotBlank(eipEntity.getEipV6Id())){
            return ActionResponse.actionFailed(CodeInfo.EIP_BIND_EIPV6_ERROR, HttpStatus.SC_NOT_FOUND);
        }
        if(!CommonUtil.isAuthoried(eipEntity.getUserId())){
            log.error(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), eipid);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }
        eipEntity.setStatus(HsConstants.STOP);

        MethodReturn fireWallReturn = firewallService.delNatAndQos(eipEntity);
        if(fireWallReturn.getHttpCode() == HttpStatus.SC_OK) {
            eipEntity.setUpdateTime(CommonUtil.getGmtDate());
            eipRepository.saveAndFlush(eipEntity);
            return ActionResponse.actionSuccess();
        }else{
            return ActionResponse.actionFailed(fireWallReturn.getMessage(), fireWallReturn.getHttpCode());
        }
    }
    /**
     * associate port with eip
     * @param eipid          eip
     * @param serverId     server id
     * @param instanceType instance type
     * @return             true or false
     * @throws KeycloakTokenException   e
     */
    @Transactional
    public MethodReturn associateInstanceWithEip(String eipid, String serverId, String instanceType, String portId) throws KeycloakTokenException {
        NetFloatingIP floatingIP ;
        String returnStat;
        String returnMsg ;
        Eip eip = eipRepository.findByEipId(eipid);
        if (null == eip) {
            log.error("In associate process, failed to find the eip by id:{} ", eipid);
            return MethodReturnUtil.error(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
        }
        if (!eip.getUserId().equals(CommonUtil.getUserId())) {
            log.error(CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDEN_WITH_ID), eipid);
            return MethodReturnUtil.error(HttpStatus.SC_FORBIDDEN, ReturnStatus.SC_FORBIDDEN,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
        }

        if (!(HsConstants.DOWN.equals(eip.getStatus())) || (null != eip.getDnatId()) || (null != eip.getSnatId()) ) {
            return MethodReturnUtil.error(HttpStatus.SC_BAD_REQUEST, ReturnStatus.EIP_BIND_HAS_BAND,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_HAS_BAND));
        }
        if (serverId == null || portId == null) {
            return MethodReturnUtil.error(HttpStatus.SC_BAD_REQUEST, ReturnStatus.SC_PARAM_ERROR,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_PARA_SERVERID_ERROR));
        }

        try {
            String networkId = getExtNetId(eip.getRegion());
            if (null == networkId) {
                log.error("Failed to get external net in region:{}. ", eip.getRegion());
                return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, ReturnStatus.SC_OPENSTACK_FIP_UNAVAILABLE,
                        CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_OPENSTACK_ERROR));
            }
            floatingIP = neutronService.createAndAssociateWithFip(eip.getRegion(), networkId, portId, eip, serverId);
            if (null == floatingIP) {
                log.error("Fatal Error! Can not get floating when bind ip in network:{}, region:{}, portId:{}.",
                        networkId, eip.getRegion(), portId);
                return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, ReturnStatus.SC_OPENSTACK_FIP_UNAVAILABLE,
                        CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_OPENSTACK_ERROR));
            }
            eip.setFloatingIp(floatingIP.getFloatingIpAddress());
            eip.setFloatingIpId(floatingIP.getId());

            MethodReturn  fireWallReturn = firewallService.addNatAndQos(eip, eip.getFloatingIp(), eip.getEipAddress(),
                                                    eip.getBandWidth(), eip.getFirewallId());
            returnMsg = fireWallReturn.getMessage();
            returnStat = fireWallReturn.getInnerCode();
            if(fireWallReturn.getHttpCode() == HttpStatus.SC_OK){
                String eipAddress = eip.getEipAddress();
                EipV6 eipV6 = eipV6Repository.findByIpv4AndUserIdAndIsDelete(eipAddress, eip.getUserId(), 0);
                if (eipV6 != null) {
                    NatPtV6 natPtV6 = natPtService.addNatPt(eipV6.getIpv6(), eip.getFloatingIp(), eipV6.getFirewallId());
                    if (natPtV6 == null) {
                        firewallService.delNatAndQos(eip);
                        neutronService.disassociateAndDeleteFloatingIp(floatingIP.getFloatingIpAddress(),
                                floatingIP.getId(), serverId, eip.getRegion());
                        eip.setFloatingIp(null);
                        eip.setFloatingIpId(null);
                        eipRepository.saveAndFlush(eip);
                        return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, returnStat, returnMsg);
                    }
                    eipV6.setFloatingIp(eip.getFloatingIp());
                    eipV6.setDnatptId(natPtV6.getNewDnatPtId());
                    eipV6.setSnatptId(natPtV6.getNewSnatPtId());
                    eipV6.setUpdateTime(CommonUtil.getGmtDate());
                    eipV6Repository.saveAndFlush(eipV6);
                    log.info("Bind eipv6 with instance successfully. eip:{}");

                }
                eip.setInstanceId(serverId);
                eip.setInstanceType(instanceType);
                eip.setPortId(portId);
                eip.setStatus(HsConstants.ACTIVE);
                eip.setUpdateTime(CommonUtil.getGmtDate());
                eipRepository.saveAndFlush(eip);
                log.info("Bind eip with instance successfully. eip:{}, instance:{}, portId:{}",
                        eip.getEipAddress(), eip.getInstanceId(), eip.getPortId());
                return MethodReturnUtil.success(eip);
            }else{
                neutronService.disassociateAndDeleteFloatingIp(floatingIP.getFloatingIpAddress(),
                        floatingIP.getId(), serverId, eip.getRegion());
                eip.setFloatingIp(null);
                eip.setFloatingIpId(null);
                eipRepository.saveAndFlush(eip);
            }
        } catch (Exception e) {
            log.error("band server exception", e);
            returnStat = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
            returnMsg = e.getMessage();
        }
        return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, returnStat, returnMsg);
    }


    /**
     * disassociate port with eip
     *
     * @param eipid eip id
     * @return reuslt, true or false
     * @throws KeycloakTokenException e
     */
    @Transactional
    public ActionResponse disassociateInstanceWithEip(String eipid) throws Exception {

        String msg = null;
        Eip eipEntity = eipRepository.findByEipId(eipid);
        String status= HsConstants.DOWN;
        if(null == eipEntity){
            log.error("disassociateInstanceWithEip In disassociate process,failed to find the eip by id:{} ",eipid);
            return ActionResponse.actionFailed("Not found.", HttpStatus.SC_NOT_FOUND);
        }
        if(!CommonUtil.isAuthoried(eipEntity.getUserId())){
            log.error("User have no write to delete eip:{}", eipid);
            return ActionResponse.actionFailed(HsConstants.FORBIDEN, HttpStatus.SC_FORBIDDEN);
        }

        if(eipEntity.getStatus().equals(HsConstants.DOWN)){
            msg = "Error status when disassociate eip:"+eipEntity.toString();
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_ACCEPTABLE);
        }

        MethodReturn fireWallReturn =  firewallService.delNatAndQos(eipEntity);
        if(fireWallReturn.getHttpCode() != HttpStatus.SC_OK) {
            msg += fireWallReturn.getMessage();
            status = HsConstants.ERROE;
        }

        if(null != eipEntity.getFloatingIp() && null != eipEntity.getInstanceId()) {
            ActionResponse actionResponse = neutronService.disassociateAndDeleteFloatingIp(eipEntity.getFloatingIp(),
                    eipEntity.getFloatingIpId(),
                    eipEntity.getInstanceId(), eipEntity.getRegion());
            if (!actionResponse.isSuccess()) {
                msg = "Failed to disassociate port with fip:"+eipEntity.toString();
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        }

        String eipAddress = eipEntity.getEipAddress();
        EipV6 eipV6 = eipV6Repository.findByIpv4AndUserIdAndIsDelete(eipAddress, eipEntity.getUserId(), 0);
        if (eipV6 != null) {
            Boolean flag = natPtService.delNatPt(eipV6.getSnatptId(), eipV6.getDnatptId(), eipV6.getFirewallId());
            if (!flag) {
                neutronService.associaInstanceWithFloatingIp(eipEntity, eipEntity.getInstanceId(), eipEntity.getPortId());
                firewallService.addNatAndQos(eipEntity, eipEntity.getFloatingIp(),
                        eipEntity.getEipAddress(), eipEntity.getBandWidth(), eipEntity.getFirewallId());
                msg = "Failed to disassociate  with natPt:" + eipV6.getSnatptId() + "--" + eipV6.getDnatptId();
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            } else {
                eipV6.setSnatptId(null);
                eipV6.setDnatptId(null);
                eipV6.setFloatingIp(null);
                eipV6.setUpdateTime(CommonUtil.getGmtDate());
                eipV6Repository.saveAndFlush(eipV6);
            }
        }
        eipEntity.setInstanceId(null);
        eipEntity.setInstanceType(null);
        eipEntity.setPrivateIpAddress(null);
        eipEntity.setPortId(null);
        eipEntity.setFloatingIp(null);
        eipEntity.setFloatingIpId(null);

        eipEntity.setStatus(status);
        eipEntity.setUpdateTime(CommonUtil.getGmtDate());
        eipRepository.saveAndFlush(eipEntity);

        if(null != msg ) {
            return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }else {
            return ActionResponse.actionSuccess();
        }
    }

    @Transactional
    public MethodReturn updateEipEntity(String eipid, EipUpdateParamWrapper param) {

        Eip eipEntity = eipRepository.findByEipId(eipid);
        if (null == eipEntity) {
            log.error("updateEipEntity In disassociate process,failed to find the eip by id:{} ", eipid);
            return MethodReturnUtil.error(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_NOT_FOND));
        }
        if(StringUtils.isNotBlank(eipEntity.getEipV6Id())){
            log.error("EIP is already bound to eipv6");
            return MethodReturnUtil.error(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_EIPV6_ERROR));
        }
        if(!CommonUtil.isAuthoried(eipEntity.getUserId())){
            log.error("User have no write to operate eip:{}", eipid);
            return MethodReturnUtil.error(HttpStatus.SC_FORBIDDEN, ReturnStatus.SC_FORBIDDEN,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
        }
        if(param.getEipUpdateParam().getBillType().equals(HsConstants.MONTHLY)&&param.getEipUpdateParam().getBandWidth()<eipEntity.getBandWidth()){
            //can’t sub
                return MethodReturnUtil.error(HttpStatus.SC_BAD_REQUEST, ReturnStatus.SC_PARAM_ERROR,
                        CodeInfo.getCodeMessage(CodeInfo.EIP_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR));
        }
        boolean updateStatus;
        if(null == eipEntity.getPipId() || eipEntity.getPipId().isEmpty()){
            updateStatus = true;
        }else{
            updateStatus = firewallService.updateQosBandWidth(eipEntity.getFirewallId(), eipEntity.getPipId(), eipEntity.getEipId(), String.valueOf(param.getEipUpdateParam().getBandWidth()));
        }

        if (updateStatus ||CommonUtil.qosDebug) {
            eipEntity.setOldBandWidth(eipEntity.getBandWidth());
            eipEntity.setBandWidth(param.getEipUpdateParam().getBandWidth());
            eipEntity.setBillType(param.getEipUpdateParam().getBillType());
            eipEntity.setUpdateTime(CommonUtil.getGmtDate());
            eipRepository.saveAndFlush(eipEntity);
            return MethodReturnUtil.success(eipEntity);
        }else{
            return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, ReturnStatus.SC_FIREWALL_SERVER_ERROR,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_CHANGE_BANDWIDTH_ERROR));
        }

    }

    @Transactional
    public ActionResponse reNewEipEntity(String eipId, String addTime)  {

        Eip eipEntity = eipRepository.findByEipId(eipId);
        if (null == eipEntity) {
            return ActionResponse.actionFailed("Can not find the eip by id:{}"+eipId, HttpStatus.SC_NOT_FOUND);
        }
        if((null ==eipEntity.getSnatId()) && (null == eipEntity.getDnatId()) && (null != eipEntity.getFloatingIp())){
            MethodReturn fireWallReturn =  firewallService.addNatAndQos(eipEntity, eipEntity.getFloatingIp(),
                    eipEntity.getEipAddress(), eipEntity.getBandWidth(), eipEntity.getFirewallId() );
            if(fireWallReturn.getHttpCode() == HttpStatus.SC_OK){
                log.info("renew eip entity add nat and qos,{}.  ", eipEntity);
                eipEntity.setStatus(HsConstants.ACTIVE);
                eipEntity.setUpdateTime(CommonUtil.getGmtDate());
                eipRepository.saveAndFlush(eipEntity);
            }else{
                log.error("renew eip error {}", fireWallReturn.getMessage());
            }
        }
        return ActionResponse.actionSuccess();
    }

    public List<Eip> findByUserId(String projectId){
        return eipRepository.findByUserIdAndIsDelete(projectId,0);
    }

    public  Eip findByEipAddress(String eipAddr) throws KeycloakTokenException {
        return eipRepository.findByEipAddressAndUserIdAndIsDelete(eipAddr, CommonUtil.getUserId(), 0);
    }

    public Eip findByInstanceId(String instanceId) {
        return eipRepository.findByInstanceIdAndIsDelete(instanceId,0);
    }

    public Eip getEipById(String id){

        Eip eipEntity = null;
        Optional<Eip> eip = eipRepository.findById(id);
        if (eip.isPresent()) {
            eipEntity = eip.get();
        }

        return eipEntity;
    }

    public long getInstanceNum(String userId){

        String sql ="select count(1) as num from eip where user_id='"+userId+"'"+ "and is_delete=0";

        Map<String, Object> map=jdbcTemplate.queryForMap(sql);
        long num =(long)map.get("num");
        log.debug("{}, result:{}",sql, num);


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

        String sql ="select eip_address, count(*) as num from eip group by eip_address having num>1";


        Map<String, Object> map=jdbcTemplate.queryForMap(sql);

        log.info("{}", map);

        return map;

    }

    public Map<String, Object> getDuplicateEipFromPool(){

        String sql ="select ip, count(*) as num from eip_pool group by ip having num>1";

        Map<String, Object> map=jdbcTemplate.queryForMap(sql);

        log.info("{}, result:{}",sql, map);

        return map;

    }


    /**
     * associate port with eip
     * @param eipId          eip
     * @param InstanceId     slb id
     * @param ipAddr    ip
     * @return             true or false
     * @throws KeycloakTokenException   e
     */
    @Transactional
    public MethodReturn cpsOrSlbBindEip(String eipId, String InstanceId, String ipAddr, String type) throws Exception {

        Eip eip = eipRepository.findByEipId(eipId);
        String eipIp = eip.getEipAddress();
        if (!eip.getUserId().equals( CommonUtil.getUserId())) {
            log.error("User have no write to operate eip:{}", eipId);
            return MethodReturnUtil.error(HttpStatus.SC_FORBIDDEN, ReturnStatus.SC_FORBIDDEN,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_FORBIDDEN));
        }

        if (!("DOWN".equals(eip.getStatus())) || (null != eip.getDnatId())
                || (null != eip.getSnatId()) || (null != eip.getPipId())) {
            return MethodReturnUtil.error(HttpStatus.SC_BAD_REQUEST, ReturnStatus.EIP_BIND_HAS_BAND,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_HAS_BAND));
        }
        if (InstanceId == null) {
            return MethodReturnUtil.error(HttpStatus.SC_NOT_FOUND, ReturnStatus.SC_NOT_FOUND,
                    CodeInfo.getCodeMessage(CodeInfo.SLB_BIND_NOT_FOND));
        }
        MethodReturn  fireWallReturn = firewallService.addNatAndQos(eip, ipAddr, eipIp, eip.getBandWidth(), eip.getFirewallId());
        if(fireWallReturn.getHttpCode() == HttpStatus.SC_OK) {
            EipV6 eipV6 = eipV6Repository.findByIpv4AndUserIdAndIsDelete(eipIp, eip.getUserId(), 0);
            if (eipV6 != null) {
                NatPtV6 natPtV6 = natPtService.addNatPt(eipV6.getIpv6(), eip.getFloatingIp(), eipV6.getFirewallId());
                if (natPtV6 != null) {
                    eipV6.setFloatingIp(ipAddr);
                    eipV6.setDnatptId(natPtV6.getNewDnatPtId());
                    eipV6.setSnatptId(natPtV6.getNewSnatPtId());
                    eipV6.setUpdateTime(CommonUtil.getGmtDate());
                    eipV6Repository.saveAndFlush(eipV6);
                } else {
                    firewallService.delNatAndQos(eip);
                    return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, ReturnStatus.SC_FIREWALL_NATPT_UNAVAILABLE,
                            CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_EIPV6_ERROR));
                }
            }
            eip.setInstanceId(InstanceId);
            eip.setInstanceType(type);
            eip.setStatus(HsConstants.ACTIVE);
            eip.setPrivateIpAddress(ipAddr);
            eip.setFloatingIp(ipAddr);
            eip.setUpdateTime(CommonUtil.getGmtDate());
            eipRepository.saveAndFlush(eip);
        }else{
            return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, ReturnStatus.SC_FIREWALL_SERVER_ERROR,
                    CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_ERROR));
        }
        return MethodReturnUtil.success(eip);
    }


    /**
     * associate port with eip
     * @param InstanceId          slbid
     * @return             true or false
     * @throws KeycloakTokenException   e
     */
    public ActionResponse unCpcOrSlbBindEip(String InstanceId) throws Exception {

        String msg ;
        Eip eipEntity = eipRepository.findByInstanceIdAndIsDelete(InstanceId, 0);

        if(null == eipEntity){
            log.error("unCpcOrSlbBindEip In disassociate process,failed to find the eip by id:{} ", InstanceId);
            return ActionResponse.actionFailed("Not found.", HttpStatus.SC_NOT_FOUND);
        }
        if(!eipEntity.getUserId().equals(CommonUtil.getUserId())){
            log.error("User have no write to delete eip:{}", InstanceId);
            return ActionResponse.actionFailed("Forbiden.", HttpStatus.SC_FORBIDDEN);
        }

        if(!(eipEntity.getStatus().equals("ACTIVE")) || (null == eipEntity.getSnatId())
                || (null == eipEntity.getDnatId()) ){
            msg = "Error status when disassociate eip , InstanceId: " + InstanceId + " status : " + eipEntity.getStatus() +
                    " snatId : "+eipEntity.getSnatId()+" dnatId : "+eipEntity.getDnatId();
            log.error(msg);
            return ActionResponse.actionFailed(msg, HttpStatus.SC_NOT_ACCEPTABLE);
        }

        MethodReturn fireWallReturn = firewallService.delNatAndQos(eipEntity);

        String eipAddress = eipEntity.getEipAddress();
        EipV6 eipV6 = eipV6Repository.findByIpv4AndUserIdAndIsDelete(eipAddress, eipEntity.getUserId(), 0);
        if (eipV6 != null) {
            Boolean flag = natPtService.delNatPt(eipV6.getSnatptId(), eipV6.getDnatptId(), eipV6.getFirewallId());
            if (!flag) {
                firewallService.addNatAndQos(eipEntity, eipEntity.getFloatingIp(), eipEntity.getEipAddress(), eipEntity.getBandWidth(), eipEntity.getFirewallId());
                msg = "Failed to disassociate  with natPt:" + eipV6.getSnatptId() + eipV6.getDnatptId();
                log.error(msg);
                return ActionResponse.actionFailed(msg, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            } else {
                eipV6.setSnatptId(null);
                eipV6.setDnatptId(null);
                eipV6.setFloatingIp(null);
                eipV6.setUpdateTime(CommonUtil.getGmtDate());
                eipV6Repository.saveAndFlush(eipV6);
            }
        }
        eipEntity.setInstanceId(null);
        eipEntity.setPrivateIpAddress(null);
        eipEntity.setInstanceType(null);
        eipEntity.setFloatingIp(null);

        if(fireWallReturn.getHttpCode() != HttpStatus.SC_OK) {
            eipEntity.setStatus(HsConstants.ERROE);
        }else {
            eipEntity.setStatus(HsConstants.DOWN);
        }
        eipEntity.setUpdateTime(CommonUtil.getGmtDate());
        eipRepository.saveAndFlush(eipEntity);
        if(fireWallReturn.getHttpCode() != HttpStatus.SC_OK) {
            return ActionResponse.actionFailed(fireWallReturn.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }else {
            return ActionResponse.actionSuccess();
        }
    }

    public Eip get(String instanceId) {
        return eipRepository.findByInstanceIdAndIsDelete(instanceId,0);
    }

    public int statisEipCountBySbw(String sbwId, int isDelete){
        return (int)eipRepository.countBySharedBandWidthIdAndIsDelete(sbwId, 0);
    }


}
