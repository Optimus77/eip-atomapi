package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.Eip;
import com.inspur.eipatomapi.entity.EipAllocateParam;
import com.inspur.eipatomapi.entity.EipPool;
import com.inspur.eipatomapi.entity.EipUpdateParamWrapper;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.service.IEipService;
import com.inspur.eipatomapi.service.NeutronService;
import com.inspur.eipatomapi.service.FirewallService;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.openstack4j.model.network.NetFloatingIP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

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
                eipMo.setBanWidth(eipConfig.getBanWidth());
                eipMo.setSharedBandWidthId(eipConfig.getSharedBandWidthId());
                eipRepository.save(eipMo);

                eipInfo.put("eipid", eip.getId());
                eipInfo.put("status", eipMo.getState());
                eipInfo.put("iptype", eipMo.getLinkType());
                eipInfo.put("eip_address", eipMo.getEip());
                eipInfo.put("bandwidth", Integer.parseInt(eipMo.getBanWidth()));
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
    public String listEips(int currentPage,int limit){
        log.info("listEips  service start execute");
        JSONObject returnjs = new JSONObject();

        try {
            Sort sort = new Sort(Sort.Direction.DESC, "id");
            Pageable pageable =PageRequest.of(currentPage,limit,sort);
            Page<Eip> page = eipRepository.findAll(pageable);
            JSONObject data=new JSONObject();
            JSONArray eips=new JSONArray();
            for(Eip eip:page.getContent()){
                JSONObject eipJson=new JSONObject();
                eipJson.put("eipid",eip.getId());
                eipJson.put("status",eip.getState());
                eipJson.put("iptype",eip.getLinkType());
                eipJson.put("eip_address",eip.getEip());
                eipJson.put("private_ip_address",eip.getFixedIp());
                eipJson.put("bandwidth",eip.getBanWidth());
                eipJson.put("chargetype","null");
                eipJson.put("chargemode","null");
                eipJson.put("create at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(eip.getCreateTime()));
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
            returnjs.put("code",HttpStatus.SC_OK);
            returnjs.put("msg",null);
            return returnjs.toJSONString();
        }catch (Exception e){
            e.printStackTrace();
            returnjs.put("data",e.getMessage());
            returnjs.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
            returnjs.put("msg", e.getCause());
            return returnjs.toJSONString();
        }

    }

    /**
     * associate port with eip
     * @param eip          eip
     * @param portId       port id
     * @param instanceType instance type
     * @return             true or false
     * @throws Exception   e
     */
    private Boolean associatePortWithEip(Eip eip, String portId, String instanceType) throws Exception{
        NetFloatingIP netFloatingIP = neutronService.associatePortWithFloatingIp(eip.getFloatingIpId(),portId);
        String dnatRuleId = null;
        String snatRuleId = null;
        String pipId;
        if(null != netFloatingIP){
            dnatRuleId = firewallService.addDnat(eip.getFloatingIp(), eip.getEip(), eip.getFirewallId());
            snatRuleId = firewallService.addSnat(eip.getFloatingIp(), eip.getEip(), eip.getFirewallId());
            if((null != dnatRuleId) && (null != snatRuleId)){
                pipId = firewallService.addQos(eip.getFloatingIp(),
                        eip.getEip(),
                        eip.getBanWidth(),
                        eip.getFirewallId());
                if(null != pipId) {
                    eip.setInstanceId(portId);
                    eip.setInstanceType(instanceType);
                    eip.setDnatId(dnatRuleId);
                    eip.setSnatId(snatRuleId);
                    eip.setPipId(pipId);
                    eip.setState("1");
                    eipRepository.save(eip);
                    return true;
                } else {
                    log.warn("Failed to add qos in firewall"+eip.getFirewallId());
                }
            } else {
                log.warn("Failed to add snat and dnat in firewall"+eip.getFirewallId());

            }
        } else {
            log.warn("Failed to associate port with eip, portId:"+portId);
        }
        if(null != netFloatingIP){
            neutronService.disassociateFloatingIpFromPort(netFloatingIP.getFloatingNetworkId());
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
    private Boolean disassociatePortWithEip(Eip eipEntity) throws Exception  {
        NetFloatingIP netFloatingIP= neutronService.disassociateFloatingIpFromPort(eipEntity.getFloatingIpId());
        if(null != netFloatingIP) {
            log.warn("Failed to disassociate port with eip, floatingipid:" + eipEntity.getFloatingIpId());
        } else {
            eipEntity.setInstanceId(null);
            eipEntity.setInstanceType(null);
        }
        Boolean delDnatResult = firewallService.delDnat(eipEntity.getDnatId(), eipEntity.getFirewallId());
        if(!delDnatResult){
            log.warn("Failed to del dnat in firewall" + eipEntity.getFirewallId());
        }else{
            eipEntity.setDnatId(null);
        }
        Boolean delSnatResult = firewallService.delSnat(eipEntity.getSnatId(), eipEntity.getFirewallId());
        if(!delSnatResult) {
            log.warn("Failed to del snat in firewall" + eipEntity.getFirewallId());
        }else {
            eipEntity.setSnatId(null);
        }

        Boolean delQosResult = firewallService.delQos(eipEntity.getPipId(), eipEntity.getFirewallId());
        if(!delQosResult) {
            log.warn("Failed to del qos" + eipEntity.getPipId());
        } else {
            eipEntity.setPipId(null);
        }

        eipEntity.setState("0");
        eipRepository.save(eipEntity);

        return delDnatResult && delSnatResult && delQosResult && (null == netFloatingIP);
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

                eipInfo.put("eipid", eipEntity.getId());
                NetFloatingIP bandingFloatIp = neutronService.getFloatingIp(eipEntity.getFloatingIpId());
                if(bandingFloatIp!=null){
                    log.info(bandingFloatIp.toString());
                    eipInfo.put("status", bandingFloatIp.getStatus());
                }else{
                    eipInfo.put("status", "ERROR GET INFO");
                }
                eipInfo.put("status",eipEntity.getState());
                eipInfo.put("iptype", eipEntity.getLinkType());
                eipInfo.put("eip_address", eipEntity.getEip());
                eipInfo.put("private_ip_address", eipEntity.getFloatingIp());
                eipInfo.put("bandwidth", Integer.parseInt(eipEntity.getBanWidth()));
                eipInfo.put("chargetype", "THIS IS EMPTY");
                eipInfo.put("chargemode", "THIS IS EMPTY");
                eipInfo.put("create_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(eipEntity.getCreateTime()));
                JSONObject resourceset = new JSONObject();
                resourceset.put("resourcetype", eipEntity.getInstanceType());
                resourceset.put("resource_id", eipEntity.getInstanceId());
                eipInfo.put("resourceset", resourceset);
                eipWrapper.put("eip", eipInfo);

                returnjs.put("code", HttpStatus.SC_OK);
                returnjs.put("data",eipWrapper);
                returnjs.put("msg", "");
            } else {
                returnjs.put("code",HttpStatus.SC_NOT_FOUND);
                returnjs.put("data",null);
                returnjs.put("msg", "can not find instance use this id:" + eipId+"");
            }
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
    public String updateEipBandWidth(String id, EipUpdateParamWrapper param) {

        JSONObject returnjs = new JSONObject();
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                if(param.getEipUpdateParam().getBandWidth()!=null && param.getEipUpdateParam().getChargeType()!=null){
                    Integer.parseInt(param.getEipUpdateParam().getBandWidth());
                    String width=param.getEipUpdateParam().getBandWidth();
                    log.info(width);
                    //TODO UpdateQos
                    boolean updateStatus=firewallService.updateQosBandWidth(eipEntity.getFirewallId(),
                            eipEntity.getPipId(),eipEntity.getId(),width);
                    if(updateStatus){
                        log.info("before change："+eipEntity.getBanWidth());
                        eipEntity.setBanWidth(width);
                        log.info("after  change："+eipEntity.getBanWidth());
                        eipRepository.save(eipEntity);
                        JSONObject eipJSON = new JSONObject();
                        eipJSON.put("eipid", eipEntity.getId());
                        NetFloatingIP bandingFloatIp =neutronService.getFloatingIp(eipEntity.getFloatingIpId());
                        if(bandingFloatIp!=null){
                            log.info(bandingFloatIp.toString());
                            eipJSON.put("status", bandingFloatIp.getStatus());
                        }else{
                            eipJSON.put("status", "error:can't get it");
                        }
                        eipJSON.put("iptype", eipEntity.getLinkType());
                        eipJSON.put("eip_address", eipEntity.getEip());
                        eipJSON.put("port_id", eipEntity.getFloatingIp());
                        eipJSON.put("bandwidth", Integer.parseInt(eipEntity.getBanWidth()));
                        eipJSON.put("chargetype", "THIS IS EMPTY");
                        eipJSON.put("create_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(eipEntity.getCreateTime()));
                        returnjs.put("eip",eipJSON);

                        returnjs.put("code",HttpStatus.SC_OK);
                        returnjs.put("data",new JSONObject().put("data",returnjs));
                        returnjs.put("msg", "");


                    }else{
                        returnjs.put("code",HttpStatus.SC_INTERNAL_SERVER_ERROR);
                        returnjs.put("data",null);
                        returnjs.put("msg", "the qos set is not success,please contact the dev");
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
        return returnjs.toString();

    }

    /**
     * eip bind with port
     * @param id      id
     * @param portId  port id
     * @return        result
     */
    @Override
    @ICPServiceLog
    public String eipbindPort(String id,String portId){
        JSONObject returnjs = new JSONObject();
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                String instanceType = eipEntity.getInstanceType();
                switch(instanceType){
                    case "1":
                        // 1：ecs
                        if(!associatePortWithEip(eipEntity, portId, instanceType)){
                            log.warn("Failed to associate port with eip:%s."+ id);
                        }
                        break;
                    case "2":
                        // 2：cps
                        break;
                    case "3":
                        // 3：slb
                        break;
                    default:
                        log.warn("Unhandled instance type.");
                        break;
                }
                JSONObject eipJSON = new JSONObject();
                eipJSON.put("eipid", eipEntity.getId());
                NetFloatingIP bandingFloatIp =neutronService.getFloatingIp(eipEntity.getFloatingIpId());
                if(bandingFloatIp!=null){
                    log.info(bandingFloatIp.toString());
                    eipJSON.put("status", bandingFloatIp.getStatus());
                }else{
                    eipJSON.put("status", "ERROR GET INFO");
                }
                eipJSON.put("iptype", eipEntity.getLinkType());
                eipJSON.put("eip_address", eipEntity.getEip());
                eipJSON.put("port_id","portId" );
                eipJSON.put("bandwidth", Integer.parseInt(eipEntity.getBanWidth()));
                eipJSON.put("chargetype", "THIS IS EMPTY");
                eipJSON.put("create_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(eipEntity.getCreateTime()));
                returnjs.put("eipatomapi", eipJSON);
            } else {
                returnjs.put("error", "can not find EIP instance use this id:" + id+"");
            }
        } catch (Exception e) {
            e.printStackTrace();
            returnjs.put("error", e.getMessage()+"");
        }
        log.info(returnjs.toString());
        return returnjs.toString();
    }

    /**
     * un bind port
     * @param id    id
     * @return      result
     */
    @Override
    @ICPServiceLog
    public String unBindPort(String id){

        JSONObject returnjs = new JSONObject();
        try {
            Optional<Eip> eip = eipRepository.findById(id);
            if (eip.isPresent()) {
                Eip eipEntity = eip.get();
                String instanceType = eipEntity.getInstanceType();
                switch(instanceType){
                    case "1":
                        // 1：ecs
                        if(!disassociatePortWithEip(eipEntity)){
                            log.info("Failed to disassociate port with eip"+id);
                        }
                        break;
                    case "2":
                        // 2：cps
                        break;
                    case "3":
                        // 3：slb
                        break;
                    default:
                        //default ecs
                        log.info("Unhandled instance type.");
                        break;
                }
                JSONObject eipJSON = new JSONObject();
                eipJSON.put("eipid", eipEntity.getId());
                NetFloatingIP bandingFloatIp =neutronService.getFloatingIp(eipEntity.getFloatingIpId());
                if(bandingFloatIp!=null){
                    log.info(bandingFloatIp.toString());
                    eipJSON.put("status", bandingFloatIp.getStatus());
                }else{
                    eipJSON.put("status", "ERROR GET INFO");
                }
                eipJSON.put("iptype", eipEntity.getLinkType());
                eipJSON.put("eip_address", eipEntity.getEip());
                eipJSON.put("port_id","portI " );
                eipJSON.put("bandwidth", Integer.parseInt(eipEntity.getBanWidth()));
                eipJSON.put("chargetype", "THIS IS EMPTY");
                eipJSON.put("create_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(eipEntity.getCreateTime()));
                returnjs.put("eip", eipJSON);
            } else {
                returnjs.put("error", "can not find EIP instance use this id:" + id+"");
            }
        } catch (Exception e) {
            e.printStackTrace();
            returnjs.put("error", e.getMessage()+"");
        }

        return returnjs.toString();
    }

    /**
     * add eip into eip pool for test
     */
    @Override
    public void addEipPool() {
        for (int i = 0; i < 10; i++) {
            EipPool eipPoolMo = new EipPool();
            eipPoolMo.setFireWallId("firewall_id1");
            eipPoolMo.setIp("1.2.3."+i);
            eipPoolMo.setState("0");
            eipPoolRepository.save(eipPoolMo);
        }
    }
}
