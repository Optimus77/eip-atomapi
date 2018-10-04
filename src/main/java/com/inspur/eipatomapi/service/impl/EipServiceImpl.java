package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.*;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.service.IEipService;
import com.inspur.eipatomapi.service.NeutronService;
import com.inspur.eipatomapi.service.FirewallService;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.ReturnMsgUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.NetFloatingIP;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Auther: jiasirui
 * @Date: 2018/9/14 09:32
 * @Description:  the Eip Service Interface
 */

@Service
public class EipServiceImpl implements IEipService {

    @Autowired
    private EipRepository eipRepository;
    @Autowired
    private EipPoolRepository eipPoolRepository;
    @Autowired
    private FirewallService firewallService;
    @Autowired
    private NeutronService neutronService;
    @Autowired
    private FirewallRepository firewallRepository;


    private final static Log log = LogFactory.getLog(EipServiceImpl.class);

    /**
     * allocate eip
     * @param region     region
     * @param networkId  network id
     * @return           result
     */
    private synchronized EipPool allocateEip(String region, String networkId){

        List<EipPool> eipList = eipPoolRepository.findAll();
        for(EipPool eip: eipList) {
            if (eip != null) {
                String eipState="0";
                if (eip.getState().equals(eipState)) {
                    eipPoolRepository.delete(eip);
                    return eip;
                }
            }
        }
        log.warn("Failed to allocate eip in network："+networkId);
        return null;
    }

    /**
     * find eip by id
     * @param eipId  eip id
     * @return  eip entity
     */
    private Eip findEipEntryById(String eipId){
        Eip eipEntity = null;
        Optional<Eip> eip = eipRepository.findById(eipId);
        if (eip.isPresent()) {
            eipEntity = eip.get();
        }
        return eipEntity;
    }

    /**
     * create a eip
     * @param eipConfig          config
     * @param externalNetWorkId  external network id
     * @param portId             port id
     * @return                   json info of eip
     */
    @Override
    @ICPServiceLog
    public ReturnMsg createEip(EipAllocateParam eipConfig, String externalNetWorkId, String portId) {

        String code;
        String msg;
        try {
            EipPool eip = allocateEip(eipConfig.getRegion(), externalNetWorkId);
            EipReturnBase eipInfo = new EipReturnBase();
            if (null != eip) {
                NetFloatingIP floatingIP = neutronService.createFloatingIp(eipConfig.getRegion(), externalNetWorkId, portId);
                if (null != floatingIP) {
                    Eip eipMo = new Eip();
                    eipMo.setFloating_ip(floatingIP.getFloatingIpAddress());
                    eipMo.setPrivate_ip_address(floatingIP.getFixedIpAddress());
                    eipMo.setFloating_ipId(floatingIP.getId());
                    eipMo.setEip_address(eip.getIp());
                    eipMo.setStatus("DOWN");
                    eipMo.setIptype(eipConfig.getIpType());
                    eipMo.setFirewallId(eip.getFireWallId());
                    eipMo.setChargetype(eipConfig.getChargeType());
                    eipMo.setChargemode(eipConfig.getChargeMode());
                    eipMo.setPurchasetime(eipConfig.getPurchaseTime());
                    eipMo.setBanwidth(eipConfig.getBanWidth());
                    eipMo.setSharedBandWidth_id(eipConfig.getSharedBandWidthId());
                    eipMo.setProjectId(CommonUtil.getProjectId());
                    eipMo = eipRepository.save(eipMo);

                    BeanUtils.copyProperties(eipMo, eipInfo);
                    return ReturnMsgUtil.success(eipInfo);
                } else {
                    code = ReturnStatus.SC_OPENSTACK_FIPCREATE_ERROR;
                    msg = "Failed to create floating ip in external network:" + externalNetWorkId;
                    log.warn(msg);
                }
            } else {
                msg = "Not enough eip in external network:" + externalNetWorkId;
                code = ReturnStatus.SC_RESOURCE_NOTENOUGH;
                log.warn(msg);
            }
        }catch (Exception e){
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        return ReturnMsgUtil.error(code, msg);
    }



    /**
     * 1.delete  floatingIp
     * 2.Determine if Snate and Qos is deleted
     * 3.delete eip
     *
     * @param name  name
     * @param eipId  eip ip
     * @return       result: true/false
     */
    @Override
    @ICPServiceLog
    public ReturnMsg deleteEip(String name, String eipId) {
        String msg;
        String code;
        try {
            Eip eipEntity = findEipEntryById(eipId);
            if (null != eipEntity) {
                if ((null != eipEntity.getPipId()) || (null != eipEntity.getDnatId()) || (null != eipEntity.getSnatId())) {
                    msg = "Failed to delete eip,Eip is bind to port.";
                    code = ReturnStatus.SC_PARAM_UNKONWERROR;
                    log.warn(msg);
                } else {
                    if (neutronService.deleteFloatingIp(eipEntity.getName(), eipEntity.getFloating_ipId())) {
                        EipPool eipPoolMo = new EipPool();
                        eipPoolMo.setFireWallId(eipEntity.getFirewallId());
                        eipPoolMo.setIp(eipEntity.getEip_address());
                        eipPoolMo.setState("0");
                        eipPoolRepository.save(eipPoolMo);
                        eipRepository.deleteById(eipId);
                        return ReturnMsgUtil.success();
                    } else {
                        msg = "Failed to delete floating ip.";
                        code = ReturnStatus.SC_OPENSTACK_FIP_UNAVAILABLE;
                        log.warn(msg);
                    }
                }
            } else {
                msg = "Eip not found.";
                code = ReturnStatus.SC_NOT_FOUND;
                log.warn(msg);
            }
        }catch (Exception e){
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        return ReturnMsgUtil.error(code, msg);
    }

    /**
     *  list the eip
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    @Override
    public String listEips(int currentPage,int limit,boolean returnFloatingip){
        log.info("listEips  service start execute");
        JSONObject returnjs = new JSONObject();

        try {
            Sort sort = new Sort(Sort.Direction.DESC, "createtime");
            Pageable pageable =PageRequest.of(currentPage,limit,sort);
            Page<Eip> page = eipRepository.findAll(pageable);
            JSONObject data=new JSONObject();
            JSONArray eips=new JSONArray();
            for(Eip eip:page.getContent()){
                JSONObject eipJson=new JSONObject();
                if(returnFloatingip){
                    eipJson.put("floating_ip",eip.getFloating_ip());
                    eipJson.put("floating_ipId",eip.getFloating_ipId());
                }
                eipJson.put("eipid",eip.getEipid());
                eipJson.put("status",eip.getStatus());
                eipJson.put("iptype",eip.getIptype());
                eipJson.put("eip_address",eip.getEip_address());
                eipJson.put("private_ip_address",eip.getPrivate_ip_address());
                eipJson.put("bandwidth",eip.getBanwidth());
                eipJson.put("chargetype",eip.getChargetype());
                eipJson.put("chargemode",eip.getChargemode());
                eipJson.put("create_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(eip.getCreatetime()));
                eipJson.put("sharedbandwidth_id",eip.getSharedBandWidth_id());
                JSONObject resourceset=new JSONObject();
                resourceset.put("resourcetype",eip.getInstanceType());
                resourceset.put("resource_id",eip.getInstanceId());
                eipJson.put("resourceset",resourceset);
                eips.add(eipJson);
            }
            data.put("eips",eips);
            data.put("totalPages",page.getTotalPages());
            data.put("totalElements",page.getTotalElements());
            data.put("currentPage",currentPage);
            data.put("currentPagePer",limit);
            returnjs.put("data",data);
            returnjs.put("code",ReturnStatus.SC_OK);
            returnjs.put("msg","success");
        }catch (Exception e){
            e.printStackTrace();
            returnjs.put("data",e.getMessage());
            returnjs.put("code", ReturnStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("msg", e.getCause());

        }
        return returnjs.toJSONString();
    }

    /**
     * associate port with eip
     * @param eip          eip
     * @param serverId     server id
     * @param instanceType instance type
     * @return             true or false
     * @throws Exception   e
     */
    private Boolean associateInstanceWithEip(Eip eip, String serverId, String instanceType, String portId)
            throws Exception{
        ActionResponse actionResponse = neutronService.associaInstanceWithFloatingIp(eip.getFloating_ip(),serverId);
        String dnatRuleId = null;
        String snatRuleId = null;
        String pipId;
        if(actionResponse.isSuccess()){
            dnatRuleId = firewallService.addDnat(eip.getFloating_ip(), eip.getEip_address(), eip.getFirewallId());
            snatRuleId = firewallService.addSnat(eip.getFloating_ip(), eip.getEip_address(), eip.getFirewallId());
            if((null != dnatRuleId) && (null != snatRuleId)){
                pipId = firewallService.addQos(eip.getFloating_ip(),
                        eip.getEip_address(),
                        String.valueOf(eip.getBanwidth()),
                        eip.getFirewallId());
                if(null != pipId) {
//                if(null == pipId) {//Todo:debug,do delete is when finish debug
                    eip.setInstanceId(serverId);
                    eip.setInstanceType(instanceType);
                    eip.setDnatId(dnatRuleId);
                    eip.setSnatId(snatRuleId);
                    eip.setPipId(pipId);
                    eip.setPortid(portId);
                    eip.setStatus("ACTIVE");
                    eipRepository.save(eip);
                    return true;
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
            neutronService.disassociateInstanceWithFloatingIp(eip.getFloating_ip(),serverId);
        }
        if(null != snatRuleId){
            firewallService.delSnat(snatRuleId, eip.getFirewallId());
        }
        if(null != dnatRuleId){
            firewallService.delDnat(dnatRuleId, eip.getFirewallId());
        }

        return false;
    }

    /**
     * disassociate port with eip
     * @param eipEntity    eip entity
     * @return             reuslt, true or false
     * @throws Exception   e
     */
    private Boolean disassociateInstanceWithEip(Eip eipEntity) throws Exception  {
        ActionResponse actionResponse= neutronService.disassociateInstanceWithFloatingIp(eipEntity.getFloating_ip(),
                                                                                         eipEntity.getInstanceId());
        if(actionResponse.isSuccess()) {
            eipEntity.setInstanceId(null);
            eipEntity.setInstanceType(null);
        } else {
            log.warn("Failed to disassociate port with eip, floatingipid:" + eipEntity.getFloating_ipId());
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


    /**
     * get detail of the eip
     * @param eipId  the id of the eip instance
     * @return the json result
     */
    @Override
    @ICPServiceLog
    public ReturnMsg getEipDetail(String eipId) {

        try {
            Optional<Eip> eip = eipRepository.findById(eipId);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                EipReturnDetail eipReturnDetail = new EipReturnDetail();
                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                                .resource_id(eipEntity.getInstanceId())
                                .resourcetype(eipEntity.getInstanceType()).build());

                return ReturnMsgUtil.success(eipReturnDetail);
            } else {
                return ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "Can not find instance by id:" + eipId+".");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage());
        }

    }

    /**
     * get eip by instance id
     * @param  instanceId  the instance id
     * @return the json result
     */
    @Override
    @ICPServiceLog
    public ReturnMsg getEipByInstanceId(String instanceId) {

        try {
            Eip eipEntity = eipRepository.findByInstanceId(instanceId);

            if (null != eipEntity) {
                EipReturnDetail eipReturnDetail = new EipReturnDetail();

                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                        .resource_id(eipEntity.getInstanceId())
                        .resourcetype(eipEntity.getInstanceType()).build());
                return ReturnMsgUtil.success(eipReturnDetail);
            } else {
                return ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "can not find instance by this id:" + instanceId+"") ;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()) ;
        }
    }
    /**
     * update eip band width
     * @param id    id
     * @param param param
     * @return      result
     */
    @Override
    @ICPServiceLog
    public ReturnMsg updateEipBandWidth(String id, EipUpdateParamWrapper param) {
        String code;
        String msg;
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                if(param.getEipUpdateParam().getChargeType()!=null){
                    if(param.getEipUpdateParam().getBandWidth()==0){
                        log.info("=====error==>>========="+param.getEipUpdateParam().getBandWidth());
                        code = ReturnStatus.SC_PARAM_ERROR;
                        msg = "Bindwidth can not be null";
                    }else{
                        boolean updateStatus=firewallService.updateQosBandWidth(eipEntity.getFirewallId(),
                                eipEntity.getPipId(),eipEntity.getEipid(),
                                String.valueOf(param.getEipUpdateParam().getBandWidth()));
                        if(updateStatus){
                            log.info("before change："+eipEntity.getBanwidth());
                            eipEntity.setBanwidth(param.getEipUpdateParam().getBandWidth());
                            log.info("after  change："+eipEntity.getBanwidth());
                            eipRepository.save(eipEntity);

                            EipReturnDetail eipReturnDetail = new EipReturnDetail();

                            BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                            eipReturnDetail.setResourceset(Resourceset.builder()
                                    .resource_id(eipEntity.getInstanceId())
                                    .resourcetype(eipEntity.getInstanceType()).build());
                            return ReturnMsgUtil.success(eipReturnDetail);
                        }else{
                            code= ReturnStatus.SC_FIREWALL_SERVER_ERROR;
                            msg = "the qos set is not success,please contact the dev";
                        }
                    }
                }else{
                    code = ReturnStatus.SC_PARAM_ERROR;
                    msg=  "need the param bindwidth";
                }
            } else {
                code = ReturnStatus.SC_NOT_FOUND;
                msg = "can not find instance use this id:" +id+"";
            }
        }catch (NumberFormatException e){
            e.printStackTrace();
            code = ReturnStatus.SC_PARAM_ERROR;
            msg = "BandWidth must be a Integer"+e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }

        return ReturnMsgUtil.error(code, msg);

    }

    /**
     * eip bind with port
     * @param id      id
     * @param serverId  server id
     * @return        result
     */
    @Override
    @ICPServiceLog
    public ReturnMsg eipbindPort(String id, String type,String serverId, String portId){
        String code;
        String msg;
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                switch(type){
                    case "1":
                        log.info(serverId);
                        // 1：ecs
                        if(!associateInstanceWithEip(eipEntity, serverId, type, portId)){
                            code = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
                            msg = "Failed to associate  port with eip "+ id;
                            log.warn(msg);
                        }else{
                            EipReturnDetail eipReturnDetail = new EipReturnDetail();

                            BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                            eipReturnDetail.setResourceset(Resourceset.builder()
                                    .resource_id(eipEntity.getInstanceId())
                                    .resourcetype(eipEntity.getInstanceType()).build());
                            return ReturnMsgUtil.success(eipReturnDetail);
                        }
                        break;
                    case "2":
                    case "3":
                    default:
                        code = ReturnStatus.SC_PARAM_ERROR;
                        msg = "no support type param "+type;
                        log.info("no support type");
                        break;
                }

            } else {
                code = ReturnStatus.SC_NOT_FOUND;
                msg = "can find eip wiht id ："+id;
            }
        } catch (Exception e) {
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        log.info(code + msg);
        return ReturnMsgUtil.error(code,msg);
    }
    /**
     * un bind port
     * @param id    id
     * @return      result
     */
    @Override
    @ICPServiceLog
    public ReturnMsg unBindPort(String id){

        String code;
        String msg;
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                String instanceType = eipEntity.getInstanceType();
                switch(instanceType){
                    case "1":
                        // 1：ecs
                        if(!disassociateInstanceWithEip(eipEntity)){
                            code = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
                            msg = "Failed to disassociate  port with eip "+ id;
                            log.info(msg);
                        }else{
                            EipReturnDetail eipReturnDetail = new EipReturnDetail();

                            BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                            eipReturnDetail.setResourceset(Resourceset.builder()
                                    .resource_id(eipEntity.getInstanceId())
                                    .resourcetype(eipEntity.getInstanceType()).build());
                            return ReturnMsgUtil.success(eipReturnDetail);
                        }
                        break;
                    case "2":
                    case "3":
                    default:
                        //default ecs
                        code = ReturnStatus.SC_PARAM_ERROR;
                        msg = "no support instance type "+instanceType;
                        log.warn(msg);
                        break;
                }
            } else {
                code = ReturnStatus.SC_NOT_FOUND;
                msg = "can find eip wiht id ："+id;
            }
        } catch (Exception e) {
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }

        return ReturnMsgUtil.error(code, msg);
    }


    /**
     * add eip into eip pool for test
     */
    @Override
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

        for (int i = 0; i < 10; i++) {
            EipPool eipPoolMo = new EipPool();
            eipPoolMo.setFireWallId(id);
            eipPoolMo.setIp("1.2.3."+i);
            eipPoolMo.setState("0");
            eipPoolRepository.save(eipPoolMo);
        }

    }


    @Override
    public String listServer(){
        log.info("listServer start execute");
        JSONObject returnjs = new JSONObject();
        try {
            List<Server> serverList= (List<Server>) neutronService.listServer();
            JSONArray dataArray=new JSONArray();
            for(Server server:serverList){

                boolean bindFloatingIpFlag=true;
                Addresses addresses =server.getAddresses();
                Map<String, List<? extends Address>>  novaAddresses= addresses.getAddresses();
                Set<String> keySet =novaAddresses.keySet();
                for (String netname:keySet) {
                    List<? extends Address> address=novaAddresses.get(netname);
                    for(Address addr:address){
                        log.debug(server.getId()+server.getName()+"   "+addr.getType());
                        if (addr.getType().equals("floating")){
                            log.debug("===get this =======");
                            bindFloatingIpFlag=false;
                            break;
                        }
                    }

                }
                if(bindFloatingIpFlag){
                    JSONObject data=new JSONObject();
                    data.put("id",server.getId());
                    data.put("name",server.getName());
                    dataArray.add(data);
                }
            }
            returnjs.put("code",ReturnStatus.SC_OK);
            returnjs.put("data",dataArray);
            returnjs.put("msg", "success");
        }catch (Exception e){
            e.printStackTrace();
            returnjs.put("code",ReturnStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("data","{}");
            returnjs.put("msg", e.getMessage()+"");
        }


        return returnjs.toJSONString();
    }

}
