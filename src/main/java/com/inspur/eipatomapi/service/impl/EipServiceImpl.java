package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.*;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.service.EipDaoService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.inspur.eipatomapi.util.ReturnStatus.SC_OK;

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
    @Autowired
    private EipDaoService eipDaoService;

    private final static Log log = LogFactory.getLog(EipServiceImpl.class);


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
    public ResponseEntity createEip(EipAllocateParam eipConfig, String externalNetWorkId, String portId) {

        String code;
        String msg;
        try {
            Eip eipMo = eipDaoService.allocateEip(eipConfig.getRegion(), externalNetWorkId);
            if (null != eipMo) {
                NetFloatingIP floatingIP = neutronService.createFloatingIp(eipConfig.getRegion(), externalNetWorkId, portId);
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
                    eipMo.setProjectId(CommonUtil.getProjectId());
                    eipMo = eipDaoService.updateEipEntity(eipMo);

                    EipReturnBase eipInfo = new EipReturnBase();
                    BeanUtils.copyProperties(eipMo, eipInfo);
                    return new ResponseEntity<>(ReturnMsgUtil.success(eipInfo), HttpStatus.OK);

                } else {
                    eipDaoService.deleteEip(eipMo);
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
            msg = e.getCause()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }



    /**
     * 1.delete  floatingIp
     * 2.Determine if Snate and Qos is deleted
     * 3.delete eip
     *
     * @param eipIds  eip ids
     * @return       result: true/false
     */
    @Override
    @ICPServiceLog
    public ResponseEntity deleteEipList(List<String> eipIds) {
        for(String eipId: eipIds) {
            log.info("delete eip "+eipId);
            deleteEip(eipId);
        }
        return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
    }

    /**
     * delete eip
     * @param eipId eipid
     * @return return
     */
    @Override
    @ICPServiceLog
    public ResponseEntity deleteEip(String eipId) {
        String msg;
        String code;

        try {
            Eip eipEntity = findEipEntryById(eipId);
            if (null != eipEntity) {
                if ((null != eipEntity.getPipId())
                        || (null != eipEntity.getDnatId())
                        || (null != eipEntity.getSnatId())) {
                    msg = "Failed to delete eip,Eip is bind to port.";
                    code = ReturnStatus.SC_PARAM_UNKONWERROR;
                    log.warn(msg);
                } else {
                    if (neutronService.deleteFloatingIp(eipEntity.getName(), eipEntity.getFloatingIpId())) {
                        eipDaoService.deleteEip(eipEntity);
                        return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
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
            msg = e.getCause()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     *  list the eip
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    @Override
    public ResponseEntity listEips(int currentPage,int limit,boolean returnFloatingip){
        log.info("listEips  service start execute");
        try {
            JSONObject data=new JSONObject();
            JSONArray eips=new JSONArray();

            if(currentPage!=0){
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable =PageRequest.of(currentPage-1,limit,sort);
                Page<Eip> page = eipRepository.findAll(pageable);

                for(Eip eip:page.getContent()){

                    EipReturnDetail eipReturnDetail = new EipReturnDetail();
                    BeanUtils.copyProperties(eip, eipReturnDetail);
                    eipReturnDetail.setResourceset(Resourceset.builder()
                            .resource_id(eip.getInstanceId())
                            .resourcetype(eip.getInstanceType()).build());
                    eips.add(eipReturnDetail);
                }
                data.put("eips",eips);
                data.put("totalPages",page.getTotalPages());
                data.put("totalElements",page.getTotalElements());
                data.put("currentPage",currentPage);
                data.put("currentPagePer",limit);
            }else{
                List<Eip> eipList=eipRepository.findAll();

                for(Eip eip:eipList){
                    EipReturnDetail eipReturnDetail = new EipReturnDetail();
                    BeanUtils.copyProperties(eip, eipReturnDetail);
                    eipReturnDetail.setResourceset(Resourceset.builder()
                            .resource_id(eip.getInstanceId())
                            .resourcetype(eip.getInstanceType()).build());
                    eips.add(eipReturnDetail);
                }
                data.put("eips",eips);
                data.put("totalPages",1);
                data.put("totalElements",eips.size());
                data.put("currentPage",1);
                data.put("currentPagePer",eips.size());

            }
            return new ResponseEntity<>(ReturnMsgUtil.listsuccess(data), HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();

            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
        ActionResponse actionResponse = neutronService.associaInstanceWithFloatingIp(eip,serverId);
        String dnatRuleId = null;
        String snatRuleId = null;
        String pipId;
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
                    eipDaoService.updateEipEntity(eip);
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
            neutronService.disassociateInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
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
        eipDaoService.updateEipEntity(eipEntity);

        return delDnatResult && delSnatResult && delQosResult && (actionResponse.isSuccess());
    }


    /**
     * get detail of the eip
     * @param eipId  the id of the eip instance
     * @return the json result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity getEipDetail(String eipId) {

        try {
            Optional<Eip> eip = eipRepository.findById(eipId);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                EipReturnDetail eipReturnDetail = new EipReturnDetail();
                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                                .resource_id(eipEntity.getInstanceId())
                                .resourcetype(eipEntity.getInstanceType()).build());

                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "Can not find eip by id:" + eipId+"."),
                        HttpStatus.NOT_FOUND);

            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * get eip by instance id
     * @param  instanceId  the instance id
     * @return the json result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity getEipByInstanceId(String instanceId) {

        try {
            Eip eipEntity = eipRepository.findByInstanceId(instanceId);

            if (null != eipEntity) {
                EipReturnDetail eipReturnDetail = new EipReturnDetail();

                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                        .resource_id(eipEntity.getInstanceId())
                        .resourcetype(eipEntity.getInstanceType()).build());
                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "can not find instance by this id:" + instanceId+""),
                        HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * get eip by eip
     * @param  eip  the eip
     * @return the json result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity getEipByIpAddress(String eip) {

        try {
            Eip eipEntity = eipRepository.findByEipAddress(eip);

            if (null != eipEntity) {
                EipReturnDetail eipReturnDetail = new EipReturnDetail();

                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                        .resource_id(eipEntity.getInstanceId())
                        .resourcetype(eipEntity.getInstanceType()).build());
                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "can not find eip by this eip address:" + eip+""),
                        HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
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
    public ResponseEntity updateEipBandWidth(String id, EipUpdateParamWrapper param) {
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
                                eipEntity.getPipId(),eipEntity.getEipId(),
                                String.valueOf(param.getEipUpdateParam().getBandWidth()));
                        if(updateStatus || CommonUtil.isDebug){
                            log.info("before change："+eipEntity.getBandWidth());
                            eipEntity.setBandWidth(param.getEipUpdateParam().getBandWidth());
                            log.info("after  change："+eipEntity.getBandWidth());
                            eipDaoService.updateEipEntity(eipEntity);

                            EipReturnDetail eipReturnDetail = new EipReturnDetail();

                            BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                            eipReturnDetail.setResourceset(Resourceset.builder()
                                    .resource_id(eipEntity.getInstanceId())
                                    .resourcetype(eipEntity.getInstanceType()).build());
                            return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
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
            msg = e.getCause()+"";
        }

        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);

    }

    /**
     * eip bind with port
     * @param id      id
     * @param serverId  server id
     * @return        result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity eipbindPort(String id, String type,String serverId, String portId){
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
                            return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
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
                msg = "can not find eip wiht id ："+id;
            }
        } catch (Exception e) {
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getCause()+"";
        }
        log.info(code + msg);
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /**
     * un bind port
     * @param id    id
     * @return      result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity unBindPort(String id){

        String code;
        String msg;
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                String instanceType = eipEntity.getInstanceType();
                if(null != instanceType) {
                    switch (instanceType) {
                        case "1":
                            // 1：ecs
                            if (!disassociateInstanceWithEip(eipEntity)) {
                                code = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
                                msg = "Failed to disassociate  port with eip " + id;
                                log.info(msg);
                            } else {
                                EipReturnDetail eipReturnDetail = new EipReturnDetail();

                                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                                eipReturnDetail.setResourceset(Resourceset.builder()
                                        .resource_id(eipEntity.getInstanceId())
                                        .resourcetype(eipEntity.getInstanceType()).build());
                                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
                            }
                            break;
                        case "2":
                        case "3":
                        default:
                            //default ecs
                            code = ReturnStatus.SC_PARAM_ERROR;
                            msg = "no support instance type " + instanceType;
                            log.warn(msg);
                            break;
                    }
                }else{
                    code = ReturnStatus.SC_RESOURCE_ERROR;
                    msg = "Failed to get instance type.";
                }
            } else {
                code = ReturnStatus.SC_NOT_FOUND;
                msg = "can find eip wiht id ："+id;
            }
        } catch (Exception e) {
            e.printStackTrace();
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getCause()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
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

        for (int i = 0; i < 100; i++) {
            EipPool eipPoolMo = new EipPool();
            eipPoolMo.setFireWallId(id);
            eipPoolMo.setIp("13.2.3."+i);
            eipPoolMo.setState("0");
            eipPoolMo.setNum(i+1);
            //eipPoolMo.setIndex(i);
            eipPoolRepository.save(eipPoolMo);
        }

    }


    @Override
    public ResponseEntity listServer(){
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
            returnjs.put("message", "success");
            return new ResponseEntity<>(returnjs, HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
            returnjs.put("code",ReturnStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("data","{}");
            returnjs.put("message", e.getMessage()+"");
            return new ResponseEntity<>(returnjs, HttpStatus.INTERNAL_SERVER_ERROR);
        }



    }
    /**
     * get number of the eip
     * @return the json result
     */
    @Override
    @ICPServiceLog
    public ResponseEntity getEipNumber() {
        JSONObject returnjs = new JSONObject();
        try {
            List<Eip> eips = eipDaoService.findByProjectId(CommonUtil.getProjectId());
            returnjs.put("code", SC_OK);
            returnjs.put("message", "success");
            returnjs.put("number", eips.size());
            return new ResponseEntity<>(returnjs.toString(), HttpStatus.OK);
        } catch(Exception e){
            e.printStackTrace();
            returnjs.put("code", ReturnStatus.SC_NOT_FOUND);
            returnjs.put("message", "Failed");
            return new ResponseEntity<>(returnjs.toString(), HttpStatus.NOT_FOUND);
        }
    }
    
}
