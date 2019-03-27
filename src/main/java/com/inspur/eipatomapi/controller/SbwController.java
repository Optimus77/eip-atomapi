package com.inspur.eipatomapi.controller;

import com.inspur.eipatomapi.config.ConstantClassField;
import com.inspur.eipatomapi.entity.sbw.SbwAllocateParam;
import com.inspur.eipatomapi.entity.sbw.SbwAllocateParamWrapper;
import com.inspur.eipatomapi.entity.sbw.SbwUpdateParamWrapper;
import com.inspur.eipatomapi.service.impl.SbwServiceImpl;
import com.inspur.eipatomapi.util.ReturnMsgUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import com.inspur.icp.common.util.annotation.ICPControllerLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    @ApiOperation(value = "atomCreateSbw", notes = "createSbw")
    public ResponseEntity atomAllocateSbw(@Valid @RequestBody SbwAllocateParamWrapper sbwConfig, BindingResult result) {
        log.info("Create a sbws Atom param:{}.", sbwConfig.getSbwAllocateParam().toString());
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
    public ResponseEntity listSbw(@RequestParam(required = false, name = "currentPageIndex") String pageIndex,
                                  @RequestParam(required = false, name = "currentPageSize") String pageSize,
                                  @RequestParam(required = false, name = "searchValue") String searchValue) {
        log.info("Atom listSbw method param currentPageIndex:{}, currentPageSize:{}, searchValue:{}", pageIndex, pageSize, searchValue);
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
        return sbwService.listShareBandWidth(Integer.parseInt(pageIndex), Integer.parseInt(pageSize), searchValue);
    }


    @ICPControllerLog
    @GetMapping(value = "/sbws/search")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "getSbwByProjectId", notes = "get")
    public ResponseEntity getSbwByProjectId(@RequestParam(required = false) String projectId) {
        log.info("Atom param get Sbw by project Id project:{}",projectId);
        if (null == projectId) {
            return new ResponseEntity<>("not found.", HttpStatus.NOT_FOUND);
        }

        return sbwService.getSbwByProjectId(projectId);

    }

    @DeleteMapping(value = "/sbws/{sbw_id}")
    @ICPControllerLog
    @CrossOrigin(origins = "*", maxAge = 3000)
    public ResponseEntity deleteSbw(@Size(min = 36, max = 36, message = "Must be uuid.")
                                        @PathVariable("sbw_id") String sbwId) {
        log.info("Atom delete the sbw , sbwId:{} ", sbwId);
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
    @ApiOperation(value = "getSbwDetail", notes = "sbwDetail")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "sbw_id", value = "the id of sbw", required = true, dataType = "String"),
    })
    public ResponseEntity getSbwDetail(@PathVariable("sbw_id") String sbwId) {
        log.info("Atom get the sbw detail , sbwId:{} ", sbwId);
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
        log.info("Atom get Sbw Count loading……");
        return sbwService.getSbwCount();
    }

    @ICPControllerLog
    @PostMapping(value = "/sbws/{sbw_id}/renew")
    @CrossOrigin(origins = "*", maxAge = 3000)
    public ResponseEntity renewSbw(@PathVariable("sbw_id") String sbwId,
                                   @RequestBody SbwUpdateParamWrapper param) {
        log.info("Atom renew or softdown sbw sbwId:{}, param:{}.", sbwId, param.getSbwUpdateParam().toString());
        return sbwService.renewSbw(sbwId, param.getSbwUpdateParam());
    }

    /**
     * get the eipList in this sbw
     * @param sbwId id
     * @param pageIndex index
     * @param pageSize size
     * @return ret
     */
    @ICPControllerLog
    @GetMapping(value = "/sbws/{sbw_id}/eips")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "sbwListEip", notes = "listEip")
    public ResponseEntity sbwListEip(@PathVariable( name = "sbw_id") String sbwId,
                                     @RequestParam(required = false, name = "currentPageIndex", defaultValue = "1") String pageIndex,
                                     @RequestParam(required = false, name = "currentPageSize", defaultValue = "10") String pageSize) {
        log.info("Atom get EIP list in this Sbw sbwId:{},currentPageIndex:{}, currentPageSize:{}",sbwId, pageIndex, pageSize);
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
        return sbwService.sbwListEip(sbwId ,Integer.parseInt(pageIndex), Integer.parseInt(pageSize));
    }

    /**
     * modify sbw name
     * @return ret
     */
    @ICPControllerLog
    @PutMapping(value = "/sbws/{sbw_id}/rename", consumes = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "rename sbw name", notes = "put")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "sbw_id", value = "the id of sbw", required = true, dataType = "String"),
    })
    public ResponseEntity renameSbw(@PathVariable("sbw_id") String sbwId, @Valid @RequestBody SbwUpdateParamWrapper param , BindingResult result){
        log.info("Atom rename sbw param:{}",param.getSbwUpdateParam().toString());
        if (result.hasErrors()) {
            StringBuilder builder = new StringBuilder();
            List<FieldError> fieldErrors = result.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                builder.append(fieldError.getField() + ":" + fieldError.getDefaultMessage());
            }
            log.info("{}",builder);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, builder.toString()), HttpStatus.BAD_REQUEST);
        }
        String msg;
        if (StringUtils.isNotBlank(param.getSbwUpdateParam().getSbwName())){
            return sbwService.renameSbw(sbwId, param.getSbwUpdateParam());
        }else {
            msg="The new sbw name must not be blank";
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
        log.info("Atom get the other Eip have not bind in sbw,Sbwid:{}",sbwId);
        return sbwService.getOtherEips(sbwId);
    }


    @ICPControllerLog
    @PutMapping(value = "/sbws/{sbw_id}/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update Sbw config", notes = "post")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip", value = "the sbw wrapper ", required = true, dataType = "json"),
    })
    @CrossOrigin(origins = "*", maxAge = 3000)
    public ResponseEntity updateSbwConfig(@PathVariable("sbw_id") String sbwId, @Valid @RequestBody SbwUpdateParamWrapper param, BindingResult result) {
        log.info("Atom update sbw sbwId:{},param:{}.", sbwId, param.getSbwUpdateParam().toString());
        if (result.hasErrors()) {
            StringBuffer msgBuffer = new StringBuffer();
            List<FieldError> fieldErrors = result.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                msgBuffer.append(fieldError.getField() + ":" + fieldError.getDefaultMessage());
            }
            log.info("{}", msgBuffer);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msgBuffer.toString()), HttpStatus.BAD_REQUEST);
        }
        String msg ;
        if (param.getSbwUpdateParam().getBillType() != null ) {
            log.info("update bandWidth, sbwid:{}, param:{} ", sbwId, param.getSbwUpdateParam());
            return sbwService.updateSbwBandWidth(sbwId, param.getSbwUpdateParam());
        } else {
            msg = "param not correct,body param like {\"sbw\" : {\"bandWidth\":xxx,\"billType\":\"xxxxxx\"}";
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msg), HttpStatus.BAD_REQUEST);
    }
}
