package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.*;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.service.EipDaoService;
import com.inspur.eipatomapi.service.IEipService;
import com.inspur.eipatomapi.service.NeutronService;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.KecloakTokenException;
import com.inspur.eipatomapi.util.ReturnMsgUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
import org.hibernate.annotations.Check;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.Server;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
    private NeutronService neutronService;

    @Autowired
    private EipDaoService eipDaoService;

    public final static Logger log = LoggerFactory.getLogger(EipServiceImpl.class);

    /**
     * create a eip
     * @param eipConfig          config
     * @param portId             port id
     * @return                   json info of eip
     */
    @Override
    @ICPServiceLog
    public ResponseEntity createEip(EipAllocateParam eipConfig, String portId) {

        String code;
        String msg;
        try {
            Eip eipMo = eipDaoService.allocateEip(eipConfig, portId);
            if (null != eipMo) {
                EipReturnBase eipInfo = new EipReturnBase();
                BeanUtils.copyProperties(eipMo, eipInfo);
                return new ResponseEntity<>(ReturnMsgUtil.success(eipInfo), HttpStatus.OK);
            } else {
                code = ReturnStatus.SC_OPENSTACK_FIPCREATE_ERROR;
                msg = "Failed to create floating ip in external network:" + eipConfig.getRegion();
                log.error(msg);
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
            if(eipDaoService.deleteEip(eipId)){
                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
            } else {
                msg = "Failed to delete eip,eip is bind to port or fip not found.";
                code = ReturnStatus.SC_PARAM_UNKONWERROR;
                log.error(msg);
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
    @ICPServiceLog
    public ResponseEntity listEips(int currentPage,int limit,boolean returnFloatingip){
        log.info("listEips  service start execute");
        try {
            String projcectid=CommonUtil.getProjectId();
            log.info(projcectid);
            if(projcectid==null){
                return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),"get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data=new JSONObject();
            JSONArray eips=new JSONArray();
            if(currentPage!=0){
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable =PageRequest.of(currentPage-1,limit,sort);
                Page<Eip> page=eipRepository.findByProjectId(projcectid,pageable);
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
                List<Eip> eipList=eipRepository.findByProjectId(projcectid);
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
            return new ResponseEntity<>(ReturnMsgUtil.success(data), HttpStatus.OK);
        }catch(KecloakTokenException e){
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN,e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
                log.warn("Failed to find eip by instance id, instanceId:{}", instanceId);
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
                log.warn("Failed to find eip by eip, eip:{}", eip);
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
            JSONObject result = eipDaoService.updateEipEntity(id, param);
            if(!result.getBoolean("flag")){
                code = result.getString("interCode");
                int httpResponseCode=result.getInteger("httpCode");
                msg = result.getString("reason");
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));
            }else{
                EipReturnDetail eipReturnDetail = new EipReturnDetail();
                Eip eipEntity=(Eip)result.get("data");
                BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                eipReturnDetail.setResourceset(Resourceset.builder()
                        .resource_id(eipEntity.getInstanceId())
                        .resourcetype(eipEntity.getInstanceType()).build());
                return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
            }
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
            switch(type){
                case "1":
                    log.info(serverId);
                    // 1：ecs
                    JSONObject result = eipDaoService.associateInstanceWithEip(id, serverId, type, portId);
                    if(!result.getBoolean("flag")){
                        code = result.getString("interCode");
                        int httpResponseCode=result.getInteger("httpCode");
                        msg = result.getString("reason");
                        log.error(msg);
                        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));

                    }else{
                        EipReturnDetail eipReturnDetail = new EipReturnDetail();
                        Eip eipEntity=(Eip)result.get("data");
                        BeanUtils.copyProperties(eipEntity, eipReturnDetail);
                        eipReturnDetail.setResourceset(Resourceset.builder()
                                .resource_id(eipEntity.getInstanceId())
                                .resourcetype(eipEntity.getInstanceType()).build());
                        return new ResponseEntity<>(ReturnMsgUtil.success(eipReturnDetail), HttpStatus.OK);
                    }
                case "2":
                case "3":
                default:
                    code = ReturnStatus.SC_PARAM_ERROR;
                    msg = "no support type param "+type;
                    log.warn(msg);
                    break;
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
                            if (!eipDaoService.disassociateInstanceWithEip(id)) {
                                code = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
                                msg = "Failed to disassociate  port with eip " + id;
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
        log.error(msg);
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * add eip into eip pool for test
     */
    @Override
    public void addEipPool(String ip) {

        try {
            eipDaoService.addEipPool(ip);
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    @Override
    @ICPServiceLog
    public ResponseEntity listServer(){
        log.info("listServer start execute");
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

            return new ResponseEntity<>(ReturnMsgUtil.success(dataArray), HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
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
            String projectid =CommonUtil.getProjectId();
            List<Eip> eips = eipDaoService.findByProjectId(projectid);
            return new ResponseEntity<>(ReturnMsgUtil.success(eips.size()), HttpStatus.OK);
        }catch (KecloakTokenException e){
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN,e.getMessage()), HttpStatus.UNAUTHORIZED);
        }catch(Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
