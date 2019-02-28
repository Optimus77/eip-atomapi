package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.entity.eipv6.*;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.EipV6Repository;
import com.inspur.eipatomapi.service.*;
import com.inspur.eipatomapi.util.*;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.model.common.ActionResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EipV6ServiceImpl implements IEipV6Service {


    @Autowired
    private EipV6DaoService eipV6DaoService;

    @Autowired
    private EipV6Repository eipV6Repository;

    @Autowired
    private EipRepository eipRepository;

    @Autowired
    private FireWallCommondService fireWallCommondService;

    @Autowired
    private NatPtService natPtService;



    /**
     * create a eipV6
     * @param eipConfig          config
     * @return                   json info of eip
     */
    public ResponseEntity atomCreateEipV6(EipV6AllocateParam eipConfig) {

        String code;
        String msg;
        try {
            EipPoolV6 eipV6 = eipV6DaoService.getOneEipFromPoolV6();
            if(null == eipV6) {
                msg = "Failed, no eipv6 in eip pool v6.";
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_RESOURCE_NOTENOUGH, msg),
                        HttpStatus.FAILED_DEPENDENCY);
            }

            EipV6 eipMo = eipV6DaoService.allocateEipV6(eipConfig, eipV6);
            if (null != eipMo) {
                EipV6ReturnBase eipInfo = new EipV6ReturnBase();
                BeanUtils.copyProperties(eipMo, eipInfo);
                log.info("Atom create a eipv6 success:{}", eipMo);
                return new ResponseEntity<>(ReturnMsgUtil.success(eipInfo), HttpStatus.OK);
            } else {
                code = ReturnStatus.SC_OPENSTACK_FIPCREATE_ERROR;
                msg = "Failed to create eipv6 " ;
                log.error(msg);
            }

        }catch (Exception e){
            log.error("Exception in atomCreateEipV6", e.getMessage());
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     *   the eipV6
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    @Override
    public ResponseEntity listEipV6s(int currentPage,int limit, String status){

        try {
            String projcectid=CommonUtil.getUserId();
            log.debug("listEipV6s  of user, userId:{}", projcectid);
            if(projcectid==null){
                return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data=new JSONObject();
            JSONArray eipv6s=new JSONArray();
            if(currentPage!=0){
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable =PageRequest.of(currentPage-1,limit,sort);
                Page<EipV6> page=eipV6Repository.findByProjectIdAndIsDelete(projcectid, 0, pageable);
                for(EipV6 eipV6:page.getContent()){
                    String eipV4Address = eipV6.getIpv4();
                    String projectId = eipV6.getProjectId();
                    if (eipV4Address == null || eipV4Address.equals("")) {
                        log.error("Failed to obtain eipv4 in eipv6",eipV4Address);
                        return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),"Failed to obtain eipv4 in eipv6"), HttpStatus.BAD_REQUEST);
                    }else{
                        Eip eip = eipRepository.findByEipAddressAndProjectIdAndIsDelete(eipV4Address, projectId,0);
                        if((null != status) && (!eipV6.getStatus().trim().equalsIgnoreCase(status))){
                            continue;
                        }
                        EipV6ReturnDetail eipV6ReturnDetail = new EipV6ReturnDetail();
                        BeanUtils.copyProperties(eipV6, eipV6ReturnDetail);
                        eipV6ReturnDetail.setEipBandWidth(eip.getBandWidth());
                        eipV6ReturnDetail.setEipChargeType(eip.getBillType());
                        eipV6ReturnDetail.setEipId(eip.getEipId());
                        eipv6s.add(eipV6ReturnDetail);
                    }

                }
                data.put("totalCount",page.getTotalElements());
                data.put("currentPage",currentPage);
                data.put("limit",limit);
                data.put("totalPages",page.getTotalPages());
                data.put("dataList",eipv6s);
            }else{
                List<EipV6> eipV6List=eipV6DaoService.findEipV6ByProjectId(projcectid);
                for(EipV6 eipV6:eipV6List){
                    String eipV4Address = eipV6.getIpv4();
                    String projectId = eipV6.getProjectId();
                    if (eipV4Address == null || eipV4Address.equals("")) {
                        log.error("Failed to obtain eipv4 in eipv6",eipV4Address);
                        return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),"Failed to obtain eipv4 in eipv6"), HttpStatus.BAD_REQUEST);
                    }else{
                        Eip eip = eipRepository.findByEipAddressAndProjectIdAndIsDelete(eipV4Address, projectId,0);
                        if((null != status) && (!eipV6.getStatus().trim().equalsIgnoreCase(status))){
                            continue;
                        }
                        EipV6ReturnDetail eipV6ReturnDetail = new EipV6ReturnDetail();
                        BeanUtils.copyProperties(eipV6, eipV6ReturnDetail);
                        eipV6ReturnDetail.setEipBandWidth(eip.getBandWidth());
                        eipV6ReturnDetail.setEipChargeType(eip.getBillType());
                        eipV6ReturnDetail.setEipId(eip.getEipId());
                        eipv6s.add(eipV6ReturnDetail);
                    }

                }
                data.put("dataList",eipv6s);
                data.put("totalPages",1);
                data.put("totalCount",eipv6s.size());
                data.put("currentPage",1);
                data.put("limit",eipv6s.size());
            }
            return new ResponseEntity<>(data, HttpStatus.OK);
        }catch(KeycloakTokenException e){
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN,e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e){
            log.error("Exception in listEipv6s", e);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * delete eipV6
     * @param eipV6Id eipV6id
     * @return return
     */
    public ResponseEntity atomDeleteEipV6(String eipV6Id) {
        String msg;
        String code;

        try {
            ActionResponse actionResponse =  eipV6DaoService.deleteEipV6(eipV6Id);
            if (actionResponse.isSuccess()){
                log.info("Atom delete eipV6 successfully, eipV6Id:{}", eipV6Id);
                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
            }else {
                msg = actionResponse.getFault();
                code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
                log.info("Atom delete eipV6 failed,{}", msg);
            }
        }catch (Exception e){
            log.error("Exception in atomDeleteEipV6", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * get detail of the eipv6
     * @param eipV6Id  the id of the eipv6 instance
     * @return the json result
     */
    @Override
    public ResponseEntity getEipV6Detail(String eipV6Id) {

        try {
            EipV6 eipV6Entity = eipV6DaoService.getEipV6ById(eipV6Id);
            if (null != eipV6Entity) {
                String eipV4Address = eipV6Entity.getIpv4();
                String projectId = eipV6Entity.getProjectId();
                if (eipV4Address == null || eipV4Address.equals("")) {
                    log.error("Failed to obtain eipv4 in eipv6",eipV4Address);
                    return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),"Failed to obtain eipv4 in eipv6"), HttpStatus.BAD_REQUEST);
                }else {
                    Eip eip = eipRepository.findByEipAddressAndProjectIdAndIsDelete(eipV4Address, projectId, 0);

                    EipV6ReturnDetail eipV6ReturnDetail = new EipV6ReturnDetail();
                    BeanUtils.copyProperties(eipV6Entity, eipV6ReturnDetail);
                    eipV6ReturnDetail.setEipBandWidth(eip.getBandWidth());
                    eipV6ReturnDetail.setEipChargeType(eip.getBillType());
                    eipV6ReturnDetail.setEipId(eip.getEipId());

                    return new ResponseEntity<>(ReturnMsgUtil.success(eipV6ReturnDetail), HttpStatus.OK);
                }
            } else {
                return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "Can not find eipV6 by id:" + eipV6Id+"."),
                        HttpStatus.NOT_FOUND);

            }
        } catch (Exception e) {
            log.error("Exception in getEipV6Detail", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }



    /**
     * eipV6 bind with port
     * @param eipV6Id      eipV6Id
     * @return        result
     */
    @Override
    public ResponseEntity eipV6bindPort(String eipV6Id,String ipv4){
        String code=null;
        String msg=null;
        String newSnatptId=null;
        String newDnatptId = null;
        String ipv6;
        String snatptId = null;
        String dnatptId = null;
        EipV6 eipV6 = eipV6Repository.findByEipV6Id(eipV6Id);
        if (null == eipV6) {
            log.error("Failed to get eipv6 based on eipV6Id, eipv6Id:{}.", eipV6Id);
            return null;
        }
        ipv6 = eipV6.getIpv6();
        String projectId = eipV6.getProjectId();
        try {
            Eip eip = eipRepository.findByEipAddressAndProjectIdAndIsDelete(ipv4, projectId, 0);
            if (eip == null) {
                code = ReturnStatus.SC_NOT_FOUND;
                msg = "Query eip failed";
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            dnatptId = eipV6.getDnatptId();
            snatptId = eipV6.getSnatptId();

            if(dnatptId==null && snatptId==null){
                EipV6 newEipV6 = eipV6DaoService.updateIp(ipv4,eipV6);
                if(newEipV6 != null){
                    code="200";
                    msg="update success";
                    return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.OK);
                }
            }else{
                //未完  待续
                Boolean flag = natPtService.delNatPt(snatptId, dnatptId, eipV6.getFirewallId());
                if (flag == true) {
                    NatPtV6 natPtV6 = natPtService.addNatPt(ipv6, ipv4, eipV6.getFirewallId());
                    if (natPtV6 != null) {
                        eipV6.setSnatptId(natPtV6.getNewSnatPtId());
                        eipV6.setDnatptId(natPtV6.getNewDnatPtId());
                        eipV6.setFloatingIp(eip.getFloatingIp());
                        eipV6.setIpv4(ipv4);
                        eipV6.setUpdateTime(CommonUtil.getGmtDate());
                        eipV6Repository.saveAndFlush(eipV6);
                        log.info("add nat successfully. snat:{}, dnat:{},",
                                newSnatptId, newDnatptId);
                        code = ReturnStatus.SC_OK;
                        msg = "Ipv4 was replaced successfully";
                    } else {
                        log.error("Failed to add natPtId" + natPtV6.getNewDnatPtId(), natPtV6.getNewSnatPtId());
                        code = ReturnStatus.SC_FIREWALL_NAT_UNAVAILABLE;
                        msg = "Failed to add natPtId";
                    }
                }else{
                    log.error("Failed to del natPtId" + snatptId, dnatptId);
                    code = ReturnStatus.SC_FIREWALL_NAT_UNAVAILABLE;
                    msg = "Failed to del natPtId";

                }
            }
        } catch (Exception e) {
            log.error("eipbindPort exception", e);

            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        log.info("Error when bind port，code:{}, msg:{}.", code, msg);
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }






}
