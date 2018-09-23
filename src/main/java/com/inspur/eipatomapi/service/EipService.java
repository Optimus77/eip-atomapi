package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.Eip;
import com.inspur.eipatomapi.entity.EipAllocateParam;
import com.inspur.eipatomapi.entity.EipUpdateParamWrapper;
import com.inspur.eipatomapi.entity.EipPool;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
import org.apache.http.HttpStatus;
import org.openstack4j.model.network.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * @Auther: jiasirui
 * @Date: 2018/9/14 09:32
 * @Description:  the Eip Service Interface
 */

@Service
public class EipService {

    @Autowired
    private EipRepository eipRepository;
    @Autowired
    private EipPoolRepository eipPoolRepository;
    @Autowired
    private FirewallService firewallService;
    @Autowired
    private NeutronService neutronService;


    private final static Logger log = Logger.getLogger(EipService.class.getName());


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
        log.warning("Failed to allocate eipatomapi in network"+networkId);
        return null;
    }


    private Eip findEipEntryById(String eipId){
        Eip eipEntity = null;
        Optional<Eip> eip = eipRepository.findById(eipId);
        if (eip.isPresent()) {
            eipEntity = eip.get();
        }
        return eipEntity;
    }

    public JSONObject createEip(EipAllocateParam eipConfig, String externalNetWorkId, String portId) throws Exception {
        Eip eipMo = null;

        JSONObject eipWrapper=new JSONObject();
        JSONObject eipInfo = new JSONObject();

        EipPool eip = allocateEip(eipConfig.getRegion(), externalNetWorkId);
        if (null != eip) {
            NetFloatingIP floatingIP = neutronService.createFloatingIp(eipConfig.getRegion(),externalNetWorkId,portId);
            if(null != floatingIP) {
                eipMo = new Eip();
                eipMo.setFloatingIp(floatingIP.getFloatingIpAddress());
                eipMo.setFixedIp(floatingIP.getFixedIpAddress());
                eipMo.setEip(eip.getIp());
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
                eipWrapper.put("eipatomapi", eipInfo);
                return eipWrapper;
            }else {
                log.warning("Failed to create floating ip in external network:"+externalNetWorkId);
            }
        }
        eipInfo.put("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        eipWrapper.put("eipatomapi", eipInfo);
        return eipWrapper;
    }


    public Boolean deleteEip(String name, String eipId){
        Boolean result = false;
        try {
            Eip eipEntity = findEipEntryById(eipId);
            if (null != eipEntity) {
                result = neutronService.deleteFloatingIp(eipEntity.getName(), eipEntity.getFloatingIpId());
                if((null != eipEntity.getPipId()) || (null != eipEntity.getDnatId()) || (null!=eipEntity.getSnatId())){
                    log.warning("Failed to delete eipatomapi,eipatomapi is bind to port.");
                    return false;
                }
                EipPool eipPoolMo = new EipPool();
                eipPoolMo.setFireWallId(eipEntity.getFirewallId());
                eipPoolMo.setIp(eipEntity.getEip());
                eipPoolMo.setState("0");
                eipPoolRepository.save(eipPoolMo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     *  list the eipatomapi
     * @param vpcId
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return
     */
    public String listEips(String vpcId,int currentPage,int limit){
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

    private Boolean associatePortWithEip(Eip eip, String portId, String instanceType) throws Exception{
        if(null != neutronService.associatePortWithFloatingIp(eip.getFloatingIpId(),portId)){
            String dnatRuleId = firewallService.addDnat(eip.getFloatingIp(), eip.getEip(), eip.getFirewallId());
            String snatRuleId = firewallService.addSnat(eip.getFloatingIp(), eip.getEip(), eip.getFirewallId());
            if((null != dnatRuleId) && (null != snatRuleId)){
                String pipId = firewallService.addQos(eip.getFloatingIp(),
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
                    log.warning("Failed to add qos in firewall"+eip.getFirewallId());
                }
            } else {
                log.warning("Failed to add snat and dnat in firewall"+eip.getFirewallId());
            }
        } else {
            log.warning("Failed to associate port with eipatomapi, portId:"+portId);
        }
        return false;
    }

    private Boolean disassociatePortWithEip(Eip eipEntity) throws Exception  {
        if(null != neutronService.disassociateFloatingIpFromPort(eipEntity.getFloatingIpId())){
            Boolean result1 = firewallService.delDnat(eipEntity.getDnatId(), eipEntity.getFirewallId());
            Boolean result2 = firewallService.delSnat(eipEntity.getDnatId(), eipEntity.getFirewallId());
            if(result1 && result2) {
                if(firewallService.delQos(eipEntity.getPipId(), eipEntity.getFirewallId())){
                    eipEntity.setInstanceId(null);
                    eipEntity.setInstanceType(null);
                    eipEntity.setDnatId(null);
                    eipEntity.setSnatId(null);
                    eipEntity.setPipId(null);
                    eipEntity.setState("0");
                    eipRepository.save(eipEntity);
                    return true;
                } else {
                    log.warning("Failed to del qos"+eipEntity.getPipId());
                }
            } else {
                log.warning("Failed to del snat and dnat in firewall"+eipEntity.getFirewallId());
            }
        } else {
            log.warning("Failed to disassociate port with eipatomapi, floatingipid:"+eipEntity.getFloatingIpId());
        }
        return false;
    }


    /**
     * get detail of the eipatomapi
     * @param eipId  the id of the eipatomapi instance
     * @return the json result
     */
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
                eipWrapper.put("eipatomapi", eipInfo);

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
     * bandWidth
     * @param
     * @return
     */
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
                        returnjs.put("eipatomapi",eipJSON);

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
     *
     * @param id
     * @param portId
     * @return
     */

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
                            log.warning("Failed to associate port with eipatomapi:%s."+ id);
                        }
                        break;
                    case "2":
                        // 2：cps
                        break;
                    case "3":
                        // 3：slb
                        break;
                    default:
                        log.warning("Unhandled instance type.");
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
     *
     * @param id
     * @return
     */
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
                            log.warning("Failed to disassociate port with eipatomapi"+id);
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
                        log.warning("Unhandled instance type.");
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
                returnjs.put("eipatomapi", eipJSON);
            } else {
                returnjs.put("error", "can not find EIP instance use this id:" + id+"");
            }
        } catch (Exception e) {
            e.printStackTrace();
            returnjs.put("error", e.getMessage()+"");
        }

        return returnjs.toString();
    }

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
