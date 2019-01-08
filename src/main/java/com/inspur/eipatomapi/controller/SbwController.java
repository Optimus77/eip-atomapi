package com.inspur.eipatomapi.controller;

import com.inspur.eipatomapi.config.ConstantClassField;
import com.inspur.eipatomapi.entity.sbw.SbwAllocateParam;
import com.inspur.eipatomapi.entity.sbw.SbwAllocateParamWrapper;
import com.inspur.eipatomapi.entity.sbw.SbwUpdateParamWrapper;
import com.inspur.eipatomapi.service.impl.SbwServiceImpl;
import com.inspur.eipatomapi.util.HsConstants;
import com.inspur.eipatomapi.util.ReturnMsgUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import com.inspur.icp.common.util.annotation.ICPControllerLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = ConstantClassField.VERSION_REST, produces = {"application/json;charset=UTF-8"})
@Api(value = "/v1", description = "sbw API")
@Validated
public class SbwController {

    @Autowired
    private SbwServiceImpl sbwService;

    @ICPControllerLog
    @PostMapping(value = "/sbws")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "atomAllocateSbw", notes = "list")
    public ResponseEntity atomAllocateSbw(@Valid @RequestBody SbwAllocateParamWrapper sbwConfig, BindingResult result) {
        log.info("Allocate a sbws:{}.", sbwConfig.getSbwAllocateParam().toString());
        if (result.hasErrors()) {
            StringBuffer msgBuffer = new StringBuffer();
            List<FieldError> fieldErrors = result.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                msgBuffer.append(fieldError.getField() + ":" + fieldError.getDefaultMessage());
            }
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msgBuffer.toString()),
                    HttpStatus.BAD_REQUEST);
        }
        return sbwService.atomCreateSbw(sbwConfig.getSbwAllocateParam());
    }


    @ICPControllerLog
    @GetMapping(value = "/sbws")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "listsbw", notes = "list")
    public ResponseEntity listSbw(@RequestParam(required = false, name = "currentPageIndex", defaultValue = "1") String pageIndex,
                                  @RequestParam(required = false, name = "currentPageSize", defaultValue = "10") String pageSize,
                                  @RequestParam(required = false, name = "searchValue") String searchValue) {
        log.info("SbwController listSbw currentPageIndex:{}, currentPageSize:{}, searchValue:{}", pageIndex, pageSize, searchValue);
        if (pageIndex == null || pageSize == null) {
            pageIndex = "0";
            pageSize = "0";
        } else {
            try {
                int currentPageNum = Integer.parseInt(pageIndex);
                int limitNum = Integer.parseInt(pageSize);
                if (currentPageNum < 0 || limitNum < 0) {
                    pageIndex = "0";
                }
            } catch (Exception e) {
                log.error("number is not correct ");
                pageIndex = "0";
                pageSize = "0";
            }
        }
        return sbwService.listSbws(Integer.parseInt(pageIndex), Integer.parseInt(pageSize), searchValue);
    }


    @ICPControllerLog
    @GetMapping(value = "/sbws/search")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "getSbwByProjectId", notes = "get")
    public ResponseEntity getSbwByProjectId(@RequestParam(required = false) String projectId) {
        if (null == projectId) {
            return new ResponseEntity<>("not found.", HttpStatus.NOT_FOUND);
        }
        if (null != projectId) {
            return sbwService.getSbwByProjectId(projectId);
        }
        return new ResponseEntity<>("not found.", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping(value = "/sbws/{sbw_id}")
    @ICPControllerLog
    @CrossOrigin(origins = "*", maxAge = 3000)
    public ResponseEntity atomDeleteSbw(@Size(min = 36, max = 36, message = "Must be uuid.")
                                        @PathVariable("sbw_id") String sbwId) {
        //Check the parameters
        log.info("Atom delete the sbw:{} ", sbwId);
        return sbwService.atomDeleteSbw(sbwId);

    }


    /**
     * get sbw instance detail
     *
     * @param sbwId the id of sbw
     * @return retrun
     */
    @ICPControllerLog
    @GetMapping(value = "/sbws/{sbw_id}")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "getSbwDetail", notes = "get")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "sbw_id", value = "the id of sbw", required = true, dataType = "String"),
    })
    public ResponseEntity getSbwDetail(@PathVariable("sbw_id") String sbwId) {
        return sbwService.getSbwDetail(sbwId);
    }

    /**
     * get sbw number of user
     *
     * @return response
     */
    @ICPControllerLog
    @GetMapping(value = "/sbwnumbers")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "getSbwCount", notes = "get number")
    public ResponseEntity getSbwCount() {
        return sbwService.getSbwCount();
    }

    @ICPControllerLog
    @PostMapping(value = "/sbws/{sbw_id}/renew")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "renewSbw", notes = "post")
    public ResponseEntity renewSbw(@PathVariable("sbw_id") String sbwId,
                                   @RequestBody SbwAllocateParam param) {
        log.info("Renew a sbw sbwId:{}, param:{}.", sbwId, param.toString());
        return sbwService.renewSbw(sbwId, param);
    }

    @ICPControllerLog
    @PutMapping(value = "/sbws/{sbw_id}/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "updateSBWconfig", notes = "post")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip", value = "the sbw wrapper ", required = true, dataType = "json"),
    })
    @CrossOrigin(origins = "*", maxAge = 3000)
    public ResponseEntity updateSbwConfig(@PathVariable("sbw_id") String sbwId, @Valid @RequestBody SbwUpdateParamWrapper paramWrapper, BindingResult result) {
        log.info("removeFromShared sbwId:{}, :{}.", sbwId, paramWrapper.toString());
        if (result.hasErrors()) {
            StringBuffer msgBuffer = new StringBuffer();
            List<FieldError> fieldErrors = result.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                msgBuffer.append(fieldError.getField() + ":" + fieldError.getDefaultMessage());
            }
            log.info("{}", msgBuffer);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msgBuffer.toString()), HttpStatus.BAD_REQUEST);
        }
        String method = paramWrapper.getSbwUpdateParam().getMethod();
        if (HsConstants.ADD_EIP_TO_SBW_METHOD.equalsIgnoreCase(method)){
            return  new ResponseEntity<>("addEIPtoSBW", HttpStatus.OK);
//            if (paramWrapper.getSbwUpdateParam().getEipAddress() != null && paramWrapper.getSbwUpdateParam().getEipAddress().size() > 0) {
//                return sbwService.addEipToSbw(sbwId, paramWrapper);
//            }
        }else if( HsConstants.REMOVE_EIP_FROM_SBW_METHOD.equalsIgnoreCase(method)){
            return  new ResponseEntity<>("removeEIPfromSBW", HttpStatus.OK);
        }else if (HsConstants.ADJUST_BANDWIDTH_SBW_METHOD.equalsIgnoreCase(method)){
            return  new ResponseEntity<>("addEIPtoSBW", HttpStatus.OK);
        }
        return null;
    }

    /**
     * get the eipList by sbw
     * @param sbwId
     * @param pageIndex
     * @param pageSize
     * @return
     */
    @ICPControllerLog
    @GetMapping(value = "/sbws/{sbw_id}/eips")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "sbwListEip", notes = "list")
    public ResponseEntity sbwListEip(@PathVariable( name = "sbw_id") String sbwId,
                                     @RequestParam(required = false, name = "currentPageIndex", defaultValue = "1") String pageIndex,
                                     @RequestParam(required = false, name = "currentPageSize", defaultValue = "10") String pageSize) {
        String status = "ACTIVE";
        log.info("SbwController sbwListEip currentPageIndex:{}, currentPageSize:{}", pageIndex, pageSize);
        if (pageIndex == null || pageSize == null) {
            pageIndex = "0";
            pageSize = "0";
        } else {
            try {
                int currentPageNum = Integer.parseInt(pageIndex);
                int limitNum = Integer.parseInt(pageSize);
                if (currentPageNum < 0 || limitNum < 0) {
                    pageIndex = "0";
                }
            } catch (Exception e) {
                log.error("number is not correct ");
                pageIndex = "0";
                pageSize = "0";
            }
        }
        return sbwService.sbwListEip(sbwId ,Integer.parseInt(pageIndex), Integer.parseInt(pageSize),status);
    }

    /**
     * modify sbw name
     * @return
     */
    @ICPControllerLog
    @PutMapping(value = "/sbws/{sbw_id}/rename", consumes = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "rename sbw name", notes = "put")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "sbw_id", value = "the id of sbw", required = true, dataType = "String"),
    })
    public ResponseEntity renameSbw(@PathVariable("sbw_id") String sbwId, @Valid @RequestBody SbwUpdateParamWrapper param , BindingResult result){
        if (result.hasErrors()) {
            StringBuffer msgBuffer = new StringBuffer();
            List<FieldError> fieldErrors = result.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                msgBuffer.append(fieldError.getField() + ":" + fieldError.getDefaultMessage());
            }
            log.info("{}",msgBuffer);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msgBuffer.toString()), HttpStatus.BAD_REQUEST);
        }
        String msg="";
        if (param.getSbwUpdateParam().getSbwName() !=null && !"".equalsIgnoreCase(param.getSbwUpdateParam().getSbwName())){
            return sbwService.renameSbw(sbwId, param);
        }else {
            msg="new sbw name must not be null";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msg), HttpStatus.BAD_REQUEST);
    }
    @ICPControllerLog
    @GetMapping(value = "/sbws/{sbw_id}/othereips")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "get othereips without the sbw", notes = "get")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "sbw_id", value = "the id of sbw", required = true, dataType = "String"),
    })
    public ResponseEntity getOtherEips(@PathVariable("sbw_id") String sbwId){
        return sbwService.getOtherEips(sbwId);
    }

    @ICPControllerLog
    @PutMapping(value = "/sbws/{sbw_id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "update sbw", notes = "put")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public ResponseEntity updateSbw(@PathVariable("sbw_id") String sbwId, @Valid @RequestBody SbwUpdateParamWrapper param , BindingResult result) {
        if (result.hasErrors()) {
            StringBuffer msgBuffer = new StringBuffer();
            List<FieldError> fieldErrors = result.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                msgBuffer.append(fieldError.getField() + ":" + fieldError.getDefaultMessage());
            }
            log.info("{}",msgBuffer);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msgBuffer.toString()), HttpStatus.BAD_REQUEST);
        }
        //todo update interface condition
        String method = param.getSbwUpdateParam().getMethod();
        String msg="";
        if(param.getSbwUpdateParam().getSbwName()!=null){
        }else{
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msg), HttpStatus.BAD_REQUEST);
    }

}
