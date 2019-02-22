package com.inspur.eipatomapi.controller;


import com.inspur.eipatomapi.config.ConstantClassField;
import com.inspur.eipatomapi.entity.eip.EipUpdateParam;
import com.inspur.eipatomapi.entity.eip.EipUpdateParamWrapper;
import com.inspur.eipatomapi.entity.eipv6.*;
import com.inspur.eipatomapi.service.impl.EipV6ServiceImpl;
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
@RequestMapping(value= ConstantClassField.VERSION_REST, produces={"application/json;charset=UTF-8"})
@Api(value = "/v1", description = "eipv6 API")
@Validated
public class EipV6Controller {

    @Autowired
    private EipV6ServiceImpl eipV6Service;

    @PostMapping(value = "/eipv6/createnatwithouteip")
    @CrossOrigin(origins = "*",maxAge = 3000)
    public ResponseEntity atomAllocateEipV6(@Valid @RequestBody EipV6AllocateParamWrapper eipV6Config, BindingResult result) {
        log.info("Allocate a eipv6:{}.", eipV6Config.getEipV6AllocateParam().toString());
        if (result.hasErrors()) {
            StringBuffer msgBuffer = new StringBuffer();
            List<FieldError> fieldErrors = result.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                msgBuffer.append(fieldError.getField() + ":" + fieldError.getDefaultMessage());
            }
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msgBuffer.toString()),
                    HttpStatus.BAD_REQUEST);
        }
        return eipV6Service.atomCreateEipV6(eipV6Config.getEipV6AllocateParam());
    }


    @GetMapping(value = "/eipv6")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="listeipv6",notes="list")
    public ResponseEntity listEipV6(@RequestParam(required = false) String currentPage ,
                                    @RequestParam(required = false )String limit,
                                    @RequestParam(required = false )String status) {
        log.debug("EipController listEipv6, currentPage:{}, limit:{}", currentPage, limit);
        if(currentPage==null||limit==null){
            currentPage="0";
            limit="0";
        }else{
            try{
                int currentPageNum = Integer.parseInt(currentPage);
                int limitNum = Integer.parseInt(limit);
                if (currentPageNum < 0 || limitNum < 0) {
                    currentPage = "0";
                }
            }catch (Exception e){
                log.error("number is not correct ");
                currentPage="0";
                limit="0";
            }
        }
        return  eipV6Service.listEipV6s(Integer.parseInt(currentPage),Integer.parseInt(limit),status);
    }

    @DeleteMapping(value = "/eipv6/{eip_v6_id}")
    @CrossOrigin(origins = "*",maxAge = 3000)
    public ResponseEntity atomDeleteEip(@Size(min=36, max=36, message = "Must be uuid.")
                                        @PathVariable("eip_v6_id") String eipV6Id) {
        //Check the parameters
        log.info("Atom delete the Eip:{} ",eipV6Id);
        return eipV6Service.atomDeleteEipV6(eipV6Id);

    }



    /**
     * get eipV6 instance detail
     *
     * @param eipV6Id the id of eipV6
     * @return retrun
     */
    @ICPControllerLog
    @GetMapping(value = "/eipv6/{eip_v6_id}")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "geteipv6Detail", notes = "get")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_v6_id", value = "the id of eipv6", required = true, dataType = "String"),
    })
    public ResponseEntity geteipV6Detail(@PathVariable("eip_v6_id") String eipV6Id) {
        return eipV6Service.getEipV6Detail(eipV6Id);
    }



    @PutMapping(value = "/eipv6/{eip_id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "update eipv6", notes = "put")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_v6_id", value = "the id of eipv6", required = true, dataType = "String"),
    })
    public ResponseEntity updateEip(@PathVariable("eip_v6_id") String eipV6Id, @Valid @RequestBody EipV6UpdateParamWrapper param , BindingResult result) {

        if (result.hasErrors()) {
            StringBuffer msgBuffer = new StringBuffer();
            List<FieldError> fieldErrors = result.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                msgBuffer.append(fieldError.getField() + ":" + fieldError.getDefaultMessage());
            }
            log.info("{}",msgBuffer);
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msgBuffer.toString()), HttpStatus.BAD_REQUEST);
        }
        EipV6UpdateParam updateParam = param.getEipV6UpdateParam();
        return eipV6Service.eipV6bindPort(eipV6Id, updateParam.getEipAddress());
    }




}
