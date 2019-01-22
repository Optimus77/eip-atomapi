package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.MethodSbwReturn;
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
        } catch (KeycloakTokenException e){
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
            String projectid = CommonUtil.getUserId();
            log.debug("listSbws  of user, userId:{}", projectid);
            if (projectid == null) {
                return new ResponseEntity<>(SbwReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data = new JSONObject();
            JSONArray sbws = new JSONArray();
            Page<Sbw> page ;
            if (pageIndex != 0) {
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable = PageRequest.of(pageIndex - 1, pageSize, sort);
                if (searchValue != null) {
                    if (searchValue.matches(MATCHE)) {
                        page = sbwRepository.findBySbwIdAndProjectIdAndIsDelete(searchValue, projectid, 0, pageable);
                    } else {
                        page = sbwRepository.findByProjectIdAndIsDeleteAndSharedbandwidthNameContaining(projectid, 0, searchValue, pageable);
                    }
                } else {
                    page = sbwRepository.findByProjectIdAndIsDelete(projectid, 0, pageable);
                }
                log.info("page projectId:", page);
                for (Sbw sbw : page.getContent()) {
                    SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                    BeanUtils.copyProperties(sbw, sbwReturnDetail);
                    long ipCount = eipRepository.countBySharedBandWidthIdAndIsDelete(sbw.getSbwId(), 0);
                    sbwReturnDetail.setIpCount((int)ipCount);
                    sbws.add(sbwReturnDetail);
                }
                data.put("sbws", sbws);
                data.put("totalPages", page.getTotalPages());
                data.put("totalElements", page.getTotalElements());
                data.put("currentPage", pageIndex);
                data.put("currentPagePer", pageSize);
            } else {
                List<Sbw> sbwList = sbwDaoService.findByProjectId(projectid);
                log.info("sbwList size:{}", sbwList.size());
                for (Sbw sbw : sbwList) {
                    if (null != searchValue) {
                        continue;
                    }
                    SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                    BeanUtils.copyProperties(sbw, sbwReturnDetail);
                    long ipCount = eipRepository.countBySharedBandWidthIdAndIsDelete(sbw.getSbwId(), 0);
                    sbwReturnDetail.setIpCount((int)ipCount);
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


    public ResponseEntity getSbwDetail(String sbwId) {
        try {
            Sbw sbwEntity = sbwDaoService.getSbwById(sbwId);
            if (null != sbwEntity) {
                SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                BeanUtils.copyProperties(sbwEntity, sbwReturnDetail);
                sbwReturnDetail.setIpCount((int)eipRepository.countBySharedBandWidthIdAndIsDelete(sbwId, 0));
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
            MethodSbwReturn result = sbwDaoService.updateSbwEntity(id, param);
            if(!result.getInnerCode().equals(ReturnStatus.SC_OK)){
                code = result.getInnerCode();
                int httpResponseCode=result.getHttpCode();
                msg = result.getMessage();
                log.error(msg);
                return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.valueOf(httpResponseCode));
            }else{
                SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                Sbw sbwEntity=(Sbw)result.getSbw();
                BeanUtils.copyProperties(sbwEntity, sbwReturnDetail);
                long count = eipRepository.countBySharedBandWidthIdAndIsDelete(sbwEntity.getSbwId(), 0);
                sbwReturnDetail.setIpCount((int)count);
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
            long num = sbwRepository.countByProjectIdAndIsDelete(projectid, 0);

            return new ResponseEntity<>(SbwReturnMsgUtil.msg(ReturnStatus.SC_OK, "get instance_num_success", num), HttpStatus.OK);
        } catch (KeycloakTokenException e) {
            return new ResponseEntity<>(SbwReturnMsgUtil.msg(ReturnStatus.SC_FORBIDDEN, e.getMessage(), null), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<>(SbwReturnMsgUtil.msg(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * get by current user
     *
     * @param projectId project id
     * @return ret
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
                long count = eipRepository.countBySharedBandWidthIdAndIsDelete(sbw.getSbwId(), 0);
                sbwReturnDetail.setIpCount((int)count);
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
     * get eipList by sbwid
     *
     * @param sbwId sbw id
     * @param currentPage page
     * @param limit limit
     * @return ret
     */
    public ResponseEntity sbwListEip(String sbwId, Integer currentPage, Integer limit) {
        String projcectid ;
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
     * @return ret
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
                sbwReturnDetail.setIpCount((int)eipRepository.countBySharedBandWidthIdAndIsDelete(sbwId, 0));
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
     * @param sbwId sbw id
     * @return ret
     */
    public ResponseEntity getOtherEips(String sbwId) {
        try {
            String projcectid=CommonUtil.getUserId();
            if (projcectid == null) {
                return new ResponseEntity<>(SbwReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            List<Eip> eipList = eipRepository.getEipListNotBinding(projcectid,0,HsConstants.HOURLYSETTLEMENT, "");
            log.info("get the other eips size:{}",eipList.size());
            JSONArray eips = new JSONArray();
            JSONObject data = new JSONObject();

            for (Eip eip: eipList){
                EipReturnDetail eipReturn = new EipReturnDetail();
                BeanUtils.copyProperties(eip, eipReturn);
                eips.add(eipReturn);
            }
            data.put("eips",eips);
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (KeycloakTokenException e) {
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN,e.getMessage()), HttpStatus.UNAUTHORIZED);
        }catch (Exception e){
            log.error("Exception in getOtherEips", e);
            return new ResponseEntity<>(SbwReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
