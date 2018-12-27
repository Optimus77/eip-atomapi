package com.inspur.eipatomapi.controller;

import com.inspur.eipatomapi.config.ConstantClassField;
import com.inspur.eipatomapi.entity.sbw.SbwAllocateParamWrapper;
import com.inspur.eipatomapi.service.impl.SbwServiceImpl;
import com.inspur.eipatomapi.util.ReturnMsgUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import com.inspur.icp.common.util.annotation.ICPControllerLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
@RequestMapping(value= ConstantClassField.VERSION_REST, produces={"application/json;charset=UTF-8"})
@Api(value = "/v1", description = "sbw API")
@Validated
public class SbwController {

    @Autowired
    private SbwServiceImpl sbwService;

    @ICPControllerLog
    @PostMapping(value = "/sbws")
    @CrossOrigin(origins = "*",maxAge = 3000)
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
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="listsbw",notes="list")
    public ResponseEntity listSbw(@RequestParam(required = false ,name = "currentPageIndex" ,defaultValue = "1") String pageIndex ,
                                  @RequestParam(required = false ,name = "currentPageSize" )String pageSize,
                                  @RequestParam(required = false ,name = "searchValue")String searchValue) {
        log.info("SbwController listSbw, currentPage:{}, limit:{}", pageIndex, pageSize);
        if(pageIndex==null||pageSize==null){
            pageIndex="0";
            pageSize="0";
        }else{
            try{
                int currentPageNum = Integer.parseInt(pageIndex);
                int limitNum = Integer.parseInt(pageSize);
                if (currentPageNum < 0 || limitNum < 0) {
                    pageIndex = "0";
                }
            }catch (Exception e){
                log.error("number is not correct ");
                pageIndex="0";
                pageSize="0";
            }
        }
        return  sbwService.listSbws(Integer.parseInt(pageIndex),Integer.parseInt(pageSize),searchValue);
    }


    @ICPControllerLog
    @GetMapping(value = "/sbws/search")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="getSbwByProjectId",notes="get")
    public ResponseEntity getSbwByProjectId(@RequestParam(required = false) String projectId) {
        if(null == projectId){
            return new ResponseEntity<>("not found.", HttpStatus.NOT_FOUND);
        }
        if(null != projectId){
            return sbwService.getSbwByProjectId(projectId);
        }
        return new ResponseEntity<>("not found.", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping(value = "/sbws/{sbw_id}")
    @ICPControllerLog
    @CrossOrigin(origins = "*",maxAge = 3000)
    public ResponseEntity atomDeleteSbw(@Size(min=36, max=36, message = "Must be uuid.")
                                        @PathVariable("sbw_id") String sbwId) {
        //Check the parameters
        log.info("Atom delete the sbw:{} ",sbwId);
        return sbwService.atomDeleteSbw(sbwId);

    }


}
