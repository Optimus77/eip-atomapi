package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.eip.*;
import com.inspur.eipatomapi.entity.sbw.*;
import com.inspur.eipatomapi.repository.SbwRepository;
import com.inspur.eipatomapi.service.ISbwService;
import com.inspur.eipatomapi.service.QosService;
import com.inspur.eipatomapi.service.SbwDaoService;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.KeycloakTokenException;
import com.inspur.eipatomapi.util.ReturnMsgUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
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
                return new ResponseEntity<>(ReturnMsgUtil.success(sbwInfo), HttpStatus.OK);
            } else {
                code = ReturnStatus.SC_OPENSTACK_FIPCREATE_ERROR;
                msg = "Failed to create sbw :" + sbwConfig.getRegion();
                log.error(msg);
            }

        } catch (Exception e) {
            log.error("Exception in atomCreateSbw", e.getMessage());
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage() + "";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Override
    @ICPServiceLog
    public ResponseEntity listSbws(int pageIndex, int pageSize, String searchValue) {
        try {
            String projcectid = CommonUtil.getUserId();
            log.debug("listSbws  of user, userId:{}", projcectid);
            if (projcectid == null) {
                return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data = new JSONObject();
            JSONArray sbws = new JSONArray();
            if (pageIndex != 0) {
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable = PageRequest.of(pageIndex - 1, pageSize, sort);
                Page<Sbw> page = sbwRepository.findByProjectIdAndIsDelete(projcectid, 0, pageable);
                log.info("page projectId:",page);
                for (Sbw sbw : page.getContent()) {
                    if ((null != searchValue)) {
                        continue;
                    }
                    SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                    BeanUtils.copyProperties(sbw, sbwReturnDetail);
                    sbwReturnDetail.setResourceset(Resourceset.builder()
                            .resourceid(sbw.getInstanceId())
                            .resourcetype(sbw.getInstanceType()).build());
                    sbws.add(sbwReturnDetail);
                }
                data.put("sbws", sbws);
                data.put("totalPages", page.getTotalPages());
                data.put("totalElements", page.getTotalElements());
                data.put("currentPage", pageIndex);
                data.put("currentPagePer", pageSize);
            } else {
                List<Sbw> sbwList = sbwDaoService.findByProjectId(projcectid);
                log.info("sbwList size:{}",sbwList.size());
                for (Sbw sbw : sbwList) {
                    if (null != searchValue) {
                        continue;
                    }
                    SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                    BeanUtils.copyProperties(sbw, sbwReturnDetail);
                    sbwReturnDetail.setResourceset(Resourceset.builder()
                            .resourceid(sbw.getInstanceId())
                            .resourcetype(sbw.getInstanceType()).build());
                    sbws.add(sbwReturnDetail);
                }
                data.put("sbws", sbws);
                data.put("totalPages", 1);
                data.put("totalElements", sbws.size());
                data.put("currentPage", 1);
                data.put("currentPagePer", sbws.size());
            }
            log.info("data :{}",data.toString());
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (KeycloakTokenException e) {
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_FORBIDDEN, e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Exception in listSbws", e);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @ICPServiceLog
    public ResponseEntity atomDeleteSbw(String sbwId) {
        String msg;
        String code;

        try {
            ActionResponse actionResponse =  sbwDaoService.deleteEip(sbwId);
            if (actionResponse.isSuccess()){
                log.info("Atom delete eip successfully, eipId:{}", sbwId);
                return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
            }else {
                msg = actionResponse.getFault();
                code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
                log.info("Atom delete eip failed,{}", msg);
            }
        }catch (Exception e){
            log.error("Exception in atomDeleteEip", e);
            code = ReturnStatus.SC_INTERNAL_SERVER_ERROR;
            msg = e.getMessage()+"";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(code, msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    @ICPServiceLog
    public ResponseEntity getSbwDetail(String sbwId) {
        return null;
    }

    @Override
    @ICPServiceLog
    public ResponseEntity updateSbwBandWidth(String id, SbwUpdateParamWrapper param) {
        return null;
    }

    @Override
    @ICPServiceLog
    public ResponseEntity getSbwByInstanceId(String instanceId) {
        return null;
    }

    @Override
    @ICPServiceLog
    public ResponseEntity getSbwCount() {
        return null;
    }

    /**
     * get by current user
     * @param projectId
     * @return
     */
    @Override
    @ICPServiceLog
    public ResponseEntity getSbwByProjectId(String projectId){
        try {
            if( projectId == null ){
                return new ResponseEntity<>(ReturnMsgUtil.error(String.valueOf(HttpStatus.BAD_REQUEST),
                        "get projcetid error please check the Authorization param"), HttpStatus.BAD_REQUEST);
            }
            JSONObject data=new JSONObject();
            JSONArray sbws=new JSONArray();
            List<Sbw> sbwList=sbwDaoService.findByProjectId(projectId);
            for(Sbw sbw:sbwList){

                SbwReturnDetail sbwReturnDetail = new SbwReturnDetail();
                BeanUtils.copyProperties(sbw, sbwReturnDetail);
                sbwReturnDetail.setResourceset(Resourceset.builder()
                        .resourceid(sbw.getInstanceId())
                        .resourcetype(sbw.getInstanceType()).build());
                sbws.add(sbwReturnDetail);
            }
            data.put("sbws",sbws);
            data.put("totalPages",1);
            data.put("totalElements",sbws.size());
            data.put("currentPage",1);
            data.put("currentPagePer",sbws.size());
            return new ResponseEntity<>(data, HttpStatus.OK);
        }catch (Exception e){
            log.error("Exception in listSbws", e);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_INTERNAL_SERVER_ERROR,e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
