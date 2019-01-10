package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.entity.sbw.*;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.SbwRepository;
import com.inspur.eipatomapi.service.ISbwService;
import com.inspur.eipatomapi.service.QosService;
import com.inspur.eipatomapi.service.SbwDaoService;
import com.inspur.eipatomapi.util.*;
import com.inspur.icp.common.util.annotation.ICPServiceLog;
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
public class SbwServiceImpl implements ISbwService {
    @Autowired
    private SbwRepository sbwRepository;

    @Autowired
    private SbwDaoService sbwDaoService;

    @Autowired
    private QosService qosService;

    @Autowired
    private EipRepository eipRepository;

    @ICPServiceLog
    public ResponseEntity atomCreateSbw(SbwAllocateParam sbwConfig) {

        String code;
        String msg;
        try {
            Sbw sbwMo = sbwDaoService.allocateSbw(sbwConfig);
            if (null != sbwMo) {
                SbwReturnBase sbwInfo = new SbwReturnBase();
                BeanUtils.copyProperties(sbwMo, sbwInfo);
                log.info("Atom create a sbw success:{}", sbwMo);
                return new ResponseEntity<>(SbwReturnMsgUtil.success(sbwInfo), HttpStatus.OK);
            } else {
                code = ReturnStatus.SC_OPENSTACK_FIPCREATE_ERROR;
                msg = "Failed to create sbw :" + sbwConfig.getRegion();
                log.error(msg);
            }
        }
        catch (KeycloakTokenException e){
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN, e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch ( Exception e) {
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(SbwReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Override
    @ICPServiceLog
    public ResponseEntity listSbws(Integer pageIndex, Integer pageSize, String searchValue) {
        try {
            String MATCHE = "(\\w{8}(-\\w{4}){3}-\\w{12}?)";
            String projcectid = CommonUtil.getUserId();
            log.debug("listSbws  of user, userId:{}", projcectid);
            if (projcectid == null) {
                return new ResponseEntity<>(SbwReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data = new JSONObject();
            JSONArray sbws = new JSONArray();
            Page<Sbw> page = null;
            if (pageIndex != 0) {
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable = PageRequest.of(pageIndex - 1, pageSize, sort);
                if (searchValue != null) {
                    if (searchValue.matches(MATCHE)) {
                        page = sbwRepository.findBySbwIdAndProjectIdAndIsDelete(searchValue, projcectid, 0, pageable);
                    } else {
                        page = sbwRepository.findByProjectIdAndIsDeleteAndSharedbandwidthNameContaining(projcectid, 0, searchValue, pageable);
                    }
                } else {
                    page = sbwRepository.findByProjectIdAndIsDelete(projcectid, 0, pageable);
                }
                log.info("page projectId:", page);
                for (Sbw sbw : page.getContent()) {
                    SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                    BeanUtils.copyProperties(sbw, sbwReturnDetail);
                    sbws.add(sbwReturnDetail);
                }
                data.put("sbws", sbws);
                data.put("totalPages", page.getTotalPages());
                data.put("totalElements", page.getTotalElements());
                data.put("currentPage", pageIndex);
                data.put("currentPagePer", pageSize);
            } else {
                List<Sbw> sbwList = sbwDaoService.findByProjectId(projcectid);
                log.info("sbwList size:{}", sbwList.size());
                for (Sbw sbw : sbwList) {
                    if (null != searchValue) {
                        continue;
                    }
                    SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                    BeanUtils.copyProperties(sbw, sbwReturnDetail);
                    sbws.add(sbwReturnDetail);
                }
                data.put("sbws", sbws);
                data.put("totalPages", 1);
                data.put("totalElements", sbws.size());
                data.put("currentPage", 1);
                data.put("currentPagePer", sbws.size());
            }
            log.info("data :{}", data.toString());
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (KeycloakTokenException e) {
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN, e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Exception in listSbws", e);
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @ICPServiceLog
    public ResponseEntity atomDeleteSbw(String sbwId) {
        String msg;
        String code;

        try {
            ActionResponse actionResponse = sbwDaoService.deleteSbw(sbwId);
            if (actionResponse.isSuccess()) {
                log.info("Atom delete eip successfully, eipId:{}", sbwId);
                return new ResponseEntity<>(SbwReturnMsgUtil.success(), HttpStatus.OK);
            } else {
                msg = actionResponse.getFault();
                code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
                log.info("Atom delete eip failed,{}", msg);
            }
        } catch (Exception e) {
            log.error("Exception in atomDeleteEip", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage() + "";
        }
        return new ResponseEntity<>(SbwReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * get sbw Detail
     *
     * @param sbwId
     * @return
     */
    public ResponseEntity getSbwDetail(String sbwId) {
        try {
            Sbw sbwEntity = sbwDaoService.getSbwById(sbwId);
            if (null != sbwEntity) {
                SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                BeanUtils.copyProperties(sbwEntity, sbwReturnDetail);
                log.info("sbwReturnDetail:{}", sbwReturnDetail.toString());
                return new ResponseEntity<>(SbwReturnMsgUtil.success(sbwReturnDetail), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "Can not find sbw by id:" + sbwId + "."),
                        HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Exception in getSbwDetail", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @ICPServiceLog
    public ResponseEntity updateSbwBandWidth(String id, SbwUpdateParamWrapper param) {
        String code;
        String msg;
        try {
            JSONObject result = sbwDaoService.updateSbwEntity(id, param);
            if(!result.getString("interCode").equals(ReturnStatus.SC_OK)){
                code = result.getString("interCode");
                int httpResponseCode=result.getInteger("httpCode");
                msg = result.getString("reason");
                log.error(msg);
                return new ResponseEntity<>(SbwReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));
            }else{
                SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                Sbw sbwEntity=(Sbw)result.get("data");
                BeanUtils.copyProperties(sbwEntity, sbwReturnDetail);
                return new ResponseEntity<>(SbwReturnMsgUtil.success(sbwReturnDetail), HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Exception in updateSbwBandWidth", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        return new ResponseEntity<>(SbwReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    @ICPServiceLog
    public ResponseEntity getSbwByInstanceId(String instanceId) {
        return null;
    }

    @Override
    @ICPServiceLog
    public ResponseEntity getSbwCount() {
        try {
            String projectid = CommonUtil.getUserId();
            return new ResponseEntity<>(SbwReturnMsgUtil.msg(ReturnStatus.SC_OK, "get instance_num_success", sbwDaoService.getSbwNum(projectid)), HttpStatus.OK);
        } catch (KeycloakTokenException e) {
            return new ResponseEntity<>(SbwReturnMsgUtil.msg(ReturnStatus.SC_FORBIDDEN, e.getMessage(), null), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<>(SbwReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * get by current user
     *
     * @param projectId
     * @return
     */
    @Override
    @ICPServiceLog
    public ResponseEntity getSbwByProjectId(String projectId) {
        try {
            if (projectId == null) {
                return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data = new JSONObject();
            JSONArray sbws = new JSONArray();
            List<Sbw> sbwList = sbwDaoService.findByProjectId(projectId);
            for (Sbw sbw : sbwList) {

                SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                BeanUtils.copyProperties(sbw, sbwReturnDetail);
                sbws.add(sbwReturnDetail);
            }
            data.put("sbws", sbws);
            data.put("totalPages", 1);
            data.put("totalElements", sbws.size());
            data.put("currentPage", 1);
            data.put("currentPagePer", sbws.size());
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Exception in listSbws", e);
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * renew sbw
     *
     * @param sbwId
     * @param sbwUpdateInfo
     * @return
     */
    @ICPServiceLog
    public ResponseEntity renewSbw(String sbwId, SbwAllocateParam sbwUpdateInfo) {
        String msg = "";
        String code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
        try {
            String renewTime = sbwUpdateInfo.getDuration();
            if (null == renewTime) {
                return new ResponseEntity<>(SbwReturnMsgUtil.error(code, msg), HttpStatus.BAD_REQUEST);
            } else if (renewTime.trim().equals("0")) {
                ActionResponse actionResponse = sbwDaoService.softDownSbw(sbwId);
                if (actionResponse.isSuccess()) {
                    return new ResponseEntity<>(SbwReturnMsgUtil.success(), HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(SbwReturnMsgUtil.error(
                            String.valueOf(actionResponse.getCode()), actionResponse.getFault()),
                            HttpStatus.BAD_REQUEST);
                }
            }
            ActionResponse actionResponse = sbwDaoService.reNewSbwEntity(sbwId, renewTime);
            if (actionResponse.isSuccess()) {
                log.info("renew sbw:{} , add duration:{}", sbwId, renewTime);
                return new ResponseEntity<>(SbwReturnMsgUtil.success(), HttpStatus.OK);
            } else {
                msg = actionResponse.getFault();
                log.error(msg);
            }
        } catch (Exception e) {
            log.error("Exception in deleteSbw", e);
            msg = e.getMessage() + "";
        }
        return new ResponseEntity<>(SbwReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * add eip's floating ip to sbw
     *
     * @param sbwId
     * @param paramWrapper
     * @return
     */
    public ResponseEntity addEipToSbw(String sbwId, SbwUpdateParamWrapper paramWrapper) {
        Sbw sbw = sbwDaoService.getSbwById(sbwId);
        if (sbw.getIsDelete() != 1 && sbw.getStatus().equalsIgnoreCase(HsConstants.ACTIVE)) {

        }
        return null;
    }

    /**
     * remove floating ip from sbw
     *
     * @param sbwId
     * @param paramWrapper
     * @return
     */
    public ResponseEntity removeEipFromSbw(String sbwId, SbwUpdateParamWrapper paramWrapper) {
        return null;
    }

    public ResponseEntity getFloatingIpListByUser() {
        return null;
    }

    /**
     * get eipList by sbwid
     *
     * @param sbwId
     * @param currentPage
     * @param limit
     * @param status
     * @return
     */
    public ResponseEntity sbwListEip(String sbwId, Integer currentPage, Integer limit, String status) {
        String projcectid = null;
        try {
            projcectid = CommonUtil.getUserId();
            log.debug("listEips  of user, userId:{}", projcectid);
            if (projcectid == null) {
                return new ResponseEntity<>(SbwReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data = new JSONObject();
            JSONArray eips = new JSONArray();
            if (currentPage != 0) {
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable = PageRequest.of(currentPage - 1, limit, sort);
                Page<Eip> page = eipRepository.findByProjectIdAndIsDeleteAndSharedBandWidthId(projcectid, 0, sbwId, pageable);
                for (Eip eip : page.getContent()) {
                    if ((null != status) && (!eip.getStatus().trim().equalsIgnoreCase(status))) {
                        continue;
                    }
                    EipReturnDetail eipReturnDetail = new EipReturnDetail();
                    BeanUtils.copyProperties(eip, eipReturnDetail);
                    eipReturnDetail.setResourceset(Resourceset.builder()
                            .resourceid(eip.getInstanceId())
                            .resourcetype(eip.getInstanceType()).build());
                    eips.add(eipReturnDetail);
                }
                data.put("eips", eips);
                data.put("totalPages", page.getTotalPages());
                data.put("totalElements", page.getTotalElements());
                data.put("currentPage", currentPage);
                data.put("currentPagePer", limit);
            } else {
                List<Eip> eipList = eipRepository.findByProjectIdAndIsDeleteAndSharedBandWidthId(projcectid, 0, sbwId);
                for (Eip eip : eipList) {
                    if ((null != status) && (!eip.getStatus().trim().equalsIgnoreCase(status))) {
                        continue;
                    }
                    EipReturnDetail eipReturnDetail = new EipReturnDetail();
                    BeanUtils.copyProperties(eip, eipReturnDetail);
                    eipReturnDetail.setResourceset(Resourceset.builder()
                            .resourceid(eip.getInstanceId())
                            .resourcetype(eip.getInstanceType()).build());
                    eips.add(eipReturnDetail);
                }
                data.put("eips", eips);
                data.put("totalPages", 1);
                data.put("totalElements", eips.size());
                data.put("currentPage", 1);
                data.put("currentPagePer", eips.size());
            }
            log.info("data:{}", data.toString());
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (KeycloakTokenException e) {
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN, e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Exception in listEips", e);
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * rename sbw
     *
     * @return
     */
    public ResponseEntity renameSbw(String sbwId, SbwUpdateParamWrapper wrapper) {
        String code;
        String msg;
        try {
            JSONObject result = sbwDaoService.renameSbw(sbwId, wrapper);
            if (!result.getString("interCode").equals(ReturnStatus.SC_OK)) {
                code = result.getString("interCode");
                int httpResponseCode = result.getInteger("httpCode");
                msg = result.getString("reason");
                log.error(msg);
                return new ResponseEntity<>(SbwReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));
            } else {
                SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                Sbw sbwEntity = (Sbw) result.get("data");
                BeanUtils.copyProperties(sbwEntity, sbwReturnDetail);
                return new ResponseEntity<>(SbwReturnMsgUtil.success(sbwReturnDetail), HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Exception in rename sbw", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage() + "";
        }
        return new ResponseEntity<>(SbwReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * get unbinding ip
     * @param sbwId
     * @return
     */
    public ResponseEntity getOtherEips(String sbwId) {
        try {
            String projcectid=CommonUtil.getUserId();
            if (projcectid == null) {
                return new ResponseEntity<>(SbwReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            List<Eip> eipList = eipRepository.getEipListNotBinding(projcectid,0,HsConstants.HOURLYSETTLEMENT, sbwId);
            log.info("get the other eips size:{}",eipList.size());
            JSONArray eips = new JSONArray();
            JSONObject data = new JSONObject();

            if (eipList !=null && eipList.size()>0){
                for (int i = 0; i < eipList.size(); i++) {
                    EipReturnDetail eipReturn = new EipReturnDetail();
                    Eip eip =  eipList.get(i);
                    BeanUtils.copyProperties(eip, eipReturn);

                    eips.add(eipReturn);
                }
                data.put("eips",eips);
                return new ResponseEntity<>(data, HttpStatus.OK);
            }else {
                log.warn("Failed to find EIP by sbw id, sbwid:{}", sbwId);
                return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_NOT_FOUND,
                        "can not find EIP by this id:" + sbwId+""),
                        HttpStatus.NOT_FOUND);
            }
        } catch (KeycloakTokenException e) {
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN,e.getMessage()), HttpStatus.UNAUTHORIZED);
        }catch (Exception e){
            log.error("Exception in getOtherEips", e);
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
