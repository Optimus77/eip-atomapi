package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.inspur.eipatomapi.entity.*;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.service.IEipService;
import com.inspur.eipatomapi.service.NeutronService;
import com.inspur.eipatomapi.service.FirewallService;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.FastjsonUtil;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.NetFloatingIP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
     * @throws Exception         e
     */
    @Override
    @ICPServiceLog
    public JSONObject createEip(EipAllocateParam eipConfig, String externalNetWorkId, String portId) throws Exception {
        //Eip eipMo;

        JSONObject eipWrapper=new JSONObject();
        JSONObject eipInfo = new JSONObject();

        EipPool eip = allocateEip(eipConfig.getRegion(), externalNetWorkId);
        if (null != eip) {
            NetFloatingIP floatingIP = neutronService.createFloatingIp(eipConfig.getRegion(),externalNetWorkId,portId);
            if(null != floatingIP) {
                Eip eipMo = new Eip();
                eipMo.setFloatingIp(floatingIP.getFloatingIpAddress());
                eipMo.setFixedIp(floatingIP.getFixedIpAddress());
                eipMo.setEip(eip.getIp());
                eipMo.setState("DOWN");
                eipMo.setLinkType(eipConfig.getIpType());
                eipMo.setFirewallId(eip.getFireWallId());
                eipMo.setFloatingIpId(floatingIP.getId());

                eipMo.setChargeType(eipConfig.getChargeType());
                eipMo.setPurchaseTime(eipConfig.getPurchaseTime());

                eipMo.setChargeMode(eipConfig.getChargeMode());
                eipMo.setBanWidth(eipConfig.getBanWidth());
                eipMo.setSharedBandWidthId(eipConfig.getSharedBandWidthId());
                eipMo.setProjectId(CommonUtil.getProjectId());
                eipRepository.save(eipMo);

                eipInfo.put("eipid", eip.getId());
                eipInfo.put("status", eipMo.getState());
                eipInfo.put("iptype", eipMo.getLinkType());
                eipInfo.put("chargetype", eipMo.getChargeType());
                eipInfo.put("chargemode", eipMo.getChargeMode());
                eipInfo.put("purchasetime", eipMo.getPurchaseTime());
                eipInfo.put("eip_address", eipMo.getEip());
                eipInfo.put("bandwidth", eipMo.getBanWidth());
                eipInfo.put("sharedbandwidth_id", eipMo.getSharedBandWidthId());
                eipInfo.put("create_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(eipMo.getCreateTime()));
                eipWrapper.put("eip", eipInfo);
                return eipWrapper;
            }else {
                log.warn("Failed to create floating ip in external network:"+externalNetWorkId);
            }
        }
        eipInfo.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        eipWrapper.put("eip", eipInfo);
        return eipWrapper;
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
    public Boolean deleteEip(String name, String eipId) throws Exception {
        Boolean result = false;
        Eip eipEntity = findEipEntryById(eipId);
        if (null != eipEntity) {
            if ((null != eipEntity.getPipId()) || (null != eipEntity.getDnatId()) || (null != eipEntity.getSnatId())) {
                log.warn("Failed to delete eip,Eip is bind to port.");
            } else {
                result = neutronService.deleteFloatingIp(eipEntity.getName(), eipEntity.getFloatingIpId());
                EipPool eipPoolMo = new EipPool();
                eipPoolMo.setFireWallId(eipEntity.getFirewallId());
                eipPoolMo.setIp(eipEntity.getEip());
                eipPoolMo.setState("0");
                eipPoolRepository.save(eipPoolMo);
                eipRepository.deleteById(eipId);
            }
        } else {
            log.warn("eipid errors");
        }
        return result;
    }

    /**
     *  list the eip
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    @Override
    public JSONObject listEips(int currentPage,int limit,boolean returnFloatingip){
        log.info("listEips  service start execute");
        JSONObject returnjs = new JSONObject();

        try {
            if(currentPage!=0){
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable =PageRequest.of(currentPage,limit,sort);
                Page<Eip> page = eipRepository.findAll(pageable);
                JSONObject data=new JSONObject();
                JSONArray eips=new JSONArray();
                for(Eip eip:page.getContent()){
                    JSONObject eipJson=new JSONObject();
                    eipReturnValueHandler(eipJson,eip,returnFloatingip);
                    eips.add(eipJson);
                }
                data.put("eips",eips);
                data.put("totalPages",page.getTotalPages());
                data.put("totalElements",page.getTotalElements());
                data.put("currentPage",currentPage);
                data.put("currentPagePer",limit);
                returnjs.put("data",data);
                returnjs.put("code",HttpStatus.SC_OK);
                returnjs.put("msg","success");
            }else{
                List<Eip> eipList=eipRepository.findAll();
                JSONObject data=new JSONObject();
                JSONArray eips=new JSONArray();
                for(Eip eip:eipList){
                    JSONObject eipJson=new JSONObject();
                    eipReturnValueHandler(eipJson,eip,returnFloatingip);
                    eips.add(eipJson);
                }
                data.put("eips",eips);
                data.put("totalPages",1);
                data.put("totalElements",eips.size());
                data.put("currentPage",1);
                data.put("currentPagePer",eips.size());
                returnjs.put("data",data);
                returnjs.put("code",HttpStatus.SC_OK);
                returnjs.put("msg","success");
            }

        }catch (Exception e){
            e.printStackTrace();
            returnjs.put("data",e.getMessage());
            returnjs.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("msg", e.getCause());

        }
        return returnjs;
    }

    /**
     * associate port with eip
     * @param eip          eip
     * @param serverId     server id
     * @param instanceType instance type
     * @return             true or false
     * @throws Exception   e
     */
    private Boolean associateInstanceWithEip(Eip eip, String serverId, String instanceType) throws Exception{
        ActionResponse actionResponse = neutronService.associaInstanceWithFloatingIp(eip.getFloatingIp(),serverId);
        String dnatRuleId = null;
        String snatRuleId = null;
        String pipId;
        if(actionResponse.isSuccess()){
            dnatRuleId = firewallService.addDnat(eip.getFloatingIp(), eip.getEip(), eip.getFirewallId());
            snatRuleId = firewallService.addSnat(eip.getFloatingIp(), eip.getEip(), eip.getFirewallId());
            if((null != dnatRuleId) && (null != snatRuleId)){
//                pipId = firewallService.addQos(eip.getFloatingIp(),
//                        eip.getEip(),
//                        String.valueOf(eip.getBanWidth()),
//                        eip.getFirewallId());
               // if(null != pipId) {
                    eip.setInstanceId(serverId);
                    eip.setInstanceType(instanceType);
                    eip.setDnatId(dnatRuleId);
                    eip.setSnatId(snatRuleId);
                    eip.setPipId("//TODO qos function is not support now");
                    eip.setState("1");
                    eipRepository.save(eip);
                    return true;
//                } else {
//                    log.warn("Failed to add qos in firewall"+eip.getFirewallId());
//                }
            } else {
                log.warn("Failed to add snat and dnat in firewall"+eip.getFirewallId());
            }
        } else {
            log.warn("Failed to associate port with eip, serverId:"+serverId);
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

        eipEntity.setState("0");
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
    public JSONObject getEipDetail(String eipId) {

        JSONObject returnjs = new JSONObject();
        try {
            Optional<Eip> eip = eipRepository.findById(eipId);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                JSONObject eipWrapper=new JSONObject();
                JSONObject eipInfo = new JSONObject();
                eipReturnValueHandler(eipInfo,eipEntity,false);
                eipWrapper.put("eip", eipInfo);
                returnjs.put("code", HttpStatus.SC_OK);
                returnjs.put("data",eipWrapper);
                returnjs.put("msg", "");
            } else {
                returnjs.put("code",HttpStatus.SC_NOT_FOUND);
                returnjs.put("data",null);
                returnjs.put("msg", "can not find instance use this id:" + eipId+"");
            }
            log.info(returnjs.toJSONString());
            return returnjs;
        } catch (Exception e) {
            e.printStackTrace();
            returnjs.put("data",e.getMessage());
            returnjs.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("msg", e.getCause());
            return returnjs;
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
    public JSONObject updateEipBandWidth(String id, EipUpdateParamWrapper param) {

        JSONObject returnjs = new JSONObject();
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                if(param.getEipUpdateParam().getChargeType()!=null){
                    if(param.getEipUpdateParam().getBandWidth()==0){
                        log.info("=====error==>>========="+param.getEipUpdateParam().getBandWidth());
                        returnjs.put("code",HttpStatus.SC_BAD_REQUEST);
                        returnjs.put("data","{}");
                        returnjs.put("msg", "bindwidth is null");
                    }else{
                        boolean updateStatus=firewallService.updateQosBandWidth(eipEntity.getFirewallId(), eipEntity.getPipId(),eipEntity.getId(),String.valueOf(param.getEipUpdateParam().getBandWidth()));
                        if(updateStatus){
                            log.info("before change："+eipEntity.getBanWidth());
                            eipEntity.setBanWidth(param.getEipUpdateParam().getBandWidth());
                            log.info("after  change："+eipEntity.getBanWidth());
                            eipRepository.save(eipEntity);
                            JSONObject eipJSON = new JSONObject();
                            eipReturnValueHandler(eipJSON,eipEntity,false);
                            returnjs.put("eip",eipJSON);
                            returnjs.put("code",HttpStatus.SC_OK);
                            returnjs.put("data",new JSONObject().put("data",returnjs));
                            returnjs.put("msg", "");
                        }else{
                            returnjs.put("code",HttpStatus.SC_INTERNAL_SERVER_ERROR);
                            returnjs.put("data",null);
                            returnjs.put("msg", "the qos set is not success,please contact the dev");
                        }
                    }
                }else{
                    returnjs.put("code",HttpStatus.SC_BAD_REQUEST);
                    returnjs.put("data",null);
                    returnjs.put("msg", "need the param bindwidth");
                }
            } else {
                returnjs.put("code",HttpStatus.SC_NOT_FOUND);
                returnjs.put("data",null);
                returnjs.put("msg", "can not find instance use this id:" +id+"");
            }
        }catch (NumberFormatException e){
            e.printStackTrace();
            returnjs.put("code",HttpStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("data",null);
            returnjs.put("msg", "BandWidth must be a Integer"+e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            returnjs.put("code",HttpStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("data",null);
            returnjs.put("error", e.getMessage()+"");
        }
        log.info(returnjs.toString());
        return returnjs;

    }

    /**
     * eip bind with port
     * @param id      id
     * @param serverId  server id
     * @return        result
     */
    @Override
    @ICPServiceLog
    public JSONObject eipBindServer(String id, String type,String serverId){
        JSONObject returnjs = new JSONObject();
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                switch(type){
                    case "1":
                        log.info(serverId);
                        // 1：ecs
                        if(!associateInstanceWithEip(eipEntity, serverId, type)){
                            log.warn("Failed to associate port with eip:%s."+ id);
                            returnjs.put("code",HttpStatus.SC_INTERNAL_SERVER_ERROR);
                            returnjs.put("data","{}");
                            returnjs.put("msg", "can't associate  port with eip"+ id);
                        }else{
                            JSONObject eipJSON = new JSONObject();
                            eipReturnValueHandler(eipJSON,eipEntity,false);
                            JSONObject eipjs=new JSONObject();
                            eipjs.put("eip",eipJSON);
                            returnjs.put("code",HttpStatus.SC_OK);
                            returnjs.put("data",eipjs);
                            returnjs.put("msg", "success");
                        }
                        break;
                    case "2":
                        // 2：cps
                        returnjs.put("code",HttpStatus.SC_ACCEPTED);
                        returnjs.put("data","{}");
                        returnjs.put("msg", "no support type param "+type);
                        break;
                    case "3":
                        // 3：slb
                        returnjs.put("code",HttpStatus.SC_ACCEPTED);
                        returnjs.put("data","{}");
                        returnjs.put("msg", "no support type param "+type);
                        break;
                    default:
                        log.info("no support type");
                        returnjs.put("code",HttpStatus.SC_ACCEPTED);
                        returnjs.put("data","{}");
                        returnjs.put("msg", "no support type param "+type);
                        break;
                }

            } else {
                returnjs.put("code",HttpStatus.SC_NOT_FOUND);
                returnjs.put("data",null);
                returnjs.put("msg", "can find eip wiht id ："+id);
            }
        } catch (Exception e) {
            e.printStackTrace();
            returnjs.put("code",HttpStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("data",null);
            returnjs.put("msg", e.getMessage()+"");
        }
        log.info(returnjs.toString());
        return returnjs;
    }
    /**
     * un bind port
     * @param id    id
     * @return      result
     */
    @Override
    @ICPServiceLog
    public JSONObject eipUnbindServer(String id){

        JSONObject returnjs = new JSONObject();
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                String instanceType = eipEntity.getInstanceType();
                switch(instanceType){
                    case "1":
                        // 1：ecs
                        if(!disassociateInstanceWithEip(eipEntity)){
                            log.info("Failed to disassociate port with eip"+id);
                            returnjs.put("code",HttpStatus.SC_INTERNAL_SERVER_ERROR);
                            returnjs.put("data","{}");
                            returnjs.put("msg", "can't associate  port with eip"+ id);
                        }else{
                            JSONObject eipJSON = new JSONObject();
                            eipReturnValueHandler(eipJSON,eipEntity,false);
                            JSONObject eipjs=new JSONObject();
                            eipjs.put("eip",eipJSON);
                            returnjs.put("code",HttpStatus.SC_OK);
                            returnjs.put("data",eipjs);
                            returnjs.put("msg", "success");
                        }
                        break;
                    case "2":
                        // 2：cps
                        returnjs.put("code",HttpStatus.SC_ACCEPTED);
                        returnjs.put("data","{}");
                        returnjs.put("msg", "no support cps ");
                        break;
                    case "3":
                        // 3：slb
                        returnjs.put("code",HttpStatus.SC_ACCEPTED);
                        returnjs.put("data","{}");
                        returnjs.put("msg", "no support slb ");
                        break;
                    default:
                        //default ecs
                        log.info("Unhandled instance type.");
                        returnjs.put("code",HttpStatus.SC_ACCEPTED);
                        returnjs.put("data","{}");
                        returnjs.put("msg", "no support instance type. ");
                        break;
                }
            } else {
                returnjs.put("code",HttpStatus.SC_NOT_FOUND);
                returnjs.put("data","{}");
                returnjs.put("msg", "can find eip wiht id ："+id);
            }
        } catch (Exception e) {
            e.printStackTrace();
            returnjs.put("code",HttpStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("data","{}");
            returnjs.put("msg", e.getMessage()+"");
        }

        return returnjs;
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
    public JSONObject listServer(){
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
            returnjs.put("code",HttpStatus.SC_OK);
            returnjs.put("data",dataArray);
            returnjs.put("msg", "success");
        }catch (Exception e){
            e.printStackTrace();
            returnjs.put("code",HttpStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("data","{}");
            returnjs.put("msg", e.getMessage()+"");
        }


        return returnjs;
    }


    private JSONObject eipReturnValueHandler(JSONObject eipJson,Eip eip,boolean containsFloatingInfo){


        eipJson.put("eipid",eip.getId());
        eipJson.put("status",eip.getState());
        eipJson.put("iptype",eip.getLinkType());
        eipJson.put("eip_address",eip.getEip());

        eipJson.put("bandwidth",eip.getBanWidth());

        eipJson.put("chargetype",eip.getChargeType());
        eipJson.put("chargemode",eip.getChargeMode());

        eipJson.put("instanceId",eip.getInstanceId());
        eipJson.put("instanceType",eip.getInstanceType());
        eipJson.put("private_ip_address",eip.getFixedIp());

        if(containsFloatingInfo){
            eipJson.put("floating_ip",eip.getFloatingIp());
            eipJson.put("floating_ipId",eip.getFloatingIpId());
        }

        eipJson.put("Sharedbandwidth_id",eip.getSharedBandWidthId());
        //eipJson.put("sharedbandwidth_id",eip.getSharedBandWidthId());

        eipJson.put("create_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(eip.getCreateTime()));

        JSONObject resourceset=new JSONObject();
        resourceset.put("resourcetype",eip.getInstanceType());
        resourceset.put("resource_id",eip.getInstanceId());
        eipJson.put("resourceset",resourceset);

        return eipJson;

    }

}
