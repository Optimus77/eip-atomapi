package com.inspur.eipatomapi.controller;

import com.inspur.eipatomapi.config.ConstantClassField;
import com.inspur.eipatomapi.entity.bss.EipReciveOrder;
import com.inspur.eipatomapi.entity.eip.EipAllocateParamWrapper;
import com.inspur.eipatomapi.entity.eip.EipDelParam;
import com.inspur.eipatomapi.entity.eip.EipUpdateParamWrapper;
import com.inspur.eipatomapi.service.impl.EipServiceImpl;
import com.inspur.eipatomapi.util.ReturnMsgUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import com.inspur.icp.common.util.annotation.ICPControllerLog;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;


@RestController
@RequestMapping(value= ConstantClassField.VERSION_REST, produces={"application/json;charset=UTF-8"})
@Api(value = "eips", description = "eip API")
@Validated
public class EipController {

    private final static Logger log = LoggerFactory.getLogger(EipController.class);

    @Autowired
    private EipServiceImpl eipService;


    @ICPControllerLog
    @PostMapping(value = "/eips")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="allocateEip",notes="allocate")
    public ResponseEntity allocateEip(@RequestBody EipReciveOrder eipConfig) {
        log.info("Allocate a eip:{}.", eipConfig.getReturnConsoleMessage());

        return eipService.createEip(eipConfig);
     }


    @DeleteMapping(value = "/eips/{eip_id}")
    @ICPControllerLog
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "deleteEip")
    public ResponseEntity deleteEip(@PathVariable("eip_id") String eipId, @RequestBody EipReciveOrder eipConfig) {
        //Check the parameters
        log.info("Delete a eip:{}.", eipConfig.getReturnConsoleMessage());
        return eipService.deleteEip(eipId, eipConfig);

    }

    @ICPControllerLog
    @GetMapping(value = "/eips")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="listeip",notes="list")
    public ResponseEntity listEip(@RequestParam(required = false) String currentPage , @RequestParam(required = false )String limit) {
        log.info("EipController listEip, currentPage:{}, limit:{}", currentPage, limit);
        if(currentPage==null||limit==null){
            currentPage="0";
            limit="0";
        }else{
            try{
                int currentPageNum=Integer.parseInt(currentPage);
                int limitNum =Integer.parseInt(limit);
                if(currentPageNum<0||limitNum<0){
                    currentPage="0";
                }
            }catch (Exception e){
                e.printStackTrace();
                log.error("number is not correct ");
                currentPage="0";
                limit="0";
            }
        }
        return  eipService.listEips(Integer.parseInt(currentPage),Integer.parseInt(limit),false);
    }



    @PostMapping(value = "/eips/deleiplist", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ICPControllerLog
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "deleiplist")
    public ResponseEntity deleteEipList(@RequestBody EipDelParam param) {
        //Check the parameters

        log.info("Delete the Eips:{}.", param.getEipids().toString());
        return eipService.deleteEipList(param.getEipids());
    }


    /**
     * get eip instance detail
     * @param eipId  the id of eip
     * @return  retrun
     */
    @ICPControllerLog
    @GetMapping(value = "/eips/{eip_id}")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "get detail of  eip instance", notes = "get")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public ResponseEntity getEipDetail(@PathVariable("eip_id") String eipId){
        return eipService.getEipDetail(eipId);
    }



    @ICPControllerLog
    @CrossOrigin(origins = "*",maxAge = 3000)
    @PostMapping(value = "/eips/bind/{eip_id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "eipBindWithServer", notes = "get")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public ResponseEntity eipBindWithPort(@PathVariable("eip_id") String eipId, @RequestBody EipUpdateParamWrapper param ) {
        log.info("Bind eip.{}, {}", eipId, param.getEipUpdateParam().toString());
        return eipService.eipbindPort(eipId,param.getEipUpdateParam().getType(),
                param.getEipUpdateParam().getServerId(),
                param.getEipUpdateParam().getPortId());
    }

    @ICPControllerLog
    @CrossOrigin(origins = "*",maxAge = 3000)
    @PostMapping(value = "/eips/unbind/{eip_id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "eipUnbinWithServer", notes = "get")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public ResponseEntity eipUnbindWithPort(@PathVariable("eip_id") String eipId) {
        log.info("Unbind eip.{}.", eipId);
        return eipService.unBindPort(eipId);
    }

    @ICPControllerLog
    @CrossOrigin(origins = "*",maxAge = 3000)
    @PutMapping(value = "/eips/{eip_id}/bindwidth", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update eip bandWidth", notes = "put")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public ResponseEntity changeEipBandWidht(@PathVariable("eip_id") String eipId, @RequestBody EipUpdateParamWrapper param) {
        log.info("Update eip.{}, {}", eipId, param.getEipUpdateParam().toString());
        return eipService.updateEipBandWidth(eipId,param);
    }


    @ICPControllerLog
    @PostMapping(value = "/eips/addeippool")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="addEipPool",notes="add eip")
    public ResponseEntity addEipPool( @RequestParam String ip) {
        eipService.addEipPool(ip);
        return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
    }


    @ICPControllerLog
    @GetMapping(value = "/eips/servers")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "show all servers", notes = "get")
    public ResponseEntity getServerList() {
        return eipService.listServer();
    }


    @ICPControllerLog
    @GetMapping(value = "/eips/instance/{instance_id}")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="getEipByInstanceId",notes="get")
    public ResponseEntity getEipByInstanceId(@Size(min=36, max=36, message = "Must be uuid.")
                                                 @PathVariable String instance_id) {
        log.info("EipController get eip by instance id:{} ",instance_id);
        return  eipService.getEipByInstanceId(instance_id);
    }


    @ICPControllerLog
    @GetMapping(value = "/eips/eipaddress/{eipaddress}")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="getEipByEipAddress",notes="get")
    public ResponseEntity getEipByEipAddress(@PathVariable String eipaddress) {
        log.info("EipController get eip by ip:{} ", eipaddress);
        return  eipService.getEipByIpAddress(eipaddress);
    }

    @ICPControllerLog
    @GetMapping(value = "/eips/eipnumber")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="get number",notes="get number")
    public ResponseEntity getEipNumber() {
        log.info("Get eip number.");
        return  eipService.getEipNumber();
    }


    @ICPControllerLog
    @PutMapping(value = "/eips/{eip_id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "update eip", notes = "put")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public ResponseEntity updateEip(@PathVariable("eip_id") String eipId,@Valid @RequestBody EipUpdateParamWrapper param , BindingResult result) {

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
        if(param.getEipUpdateParam().getPortId()!=null){
            //may be unbind oprate or bind oprate,use this param ,chargetype and bindwidth do nothing
            if(param.getEipUpdateParam().getPortId().trim().equals("")){
                log.debug("unbind oprate ");
                return eipService.unBindPort(eipId);

            }else{
                log.debug("bind oprate");
                if(param.getEipUpdateParam().getServerId()!=null&&param.getEipUpdateParam().getType()!=null){
                    return eipService.eipbindPort(eipId,param.getEipUpdateParam().getType(),
                            param.getEipUpdateParam().getServerId(),
                            param.getEipUpdateParam().getPortId());
                }else{
                    msg="need param serverid and type";
                }
            }
        }else{
            // protid is null ,maybe unbind or update bind width
            if(param.getEipUpdateParam().getChargeType()==null&&param.getEipUpdateParam().getBandWidth()==0){
                //
                return eipService.unBindPort(eipId);
            }else{
                if(param.getEipUpdateParam().getChargeType()!=null&&param.getEipUpdateParam().getBandWidth()!=0){

                    boolean chargeTypeFlag=false;
                    if(param.getEipUpdateParam().getChargeType().equals("PrePaid")||param.getEipUpdateParam().getChargeType().equals("PostPaid")){
                        chargeTypeFlag=true;
                    }else{
                        msg="chargetype must be [PrePaid |PostPaid]";
                    }
                    if(chargeTypeFlag){
                        return eipService.updateEipBandWidth(eipId,param);
                    }else{

                    }
                }else{
                    msg="param not correct. " +
                            "to bind server,body param like{\"eip\" : {\"prot_id\":\"xxx\",\"serverid\":\"xxxxxx\",\"type\":\"[1|2|3]\"}" +
                            "to unbind server , param like {\"eip\" : {\"prot_id\":\"\"} }or   {\"eip\" : {} }" +
                            "to change bindwidht,body param like {\"eip\" : {\"bandwidth\":xxx,\"chargetype\":\"xxxxxx\"}"  +
                            "";
                }
            }
        }
        return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msg), HttpStatus.BAD_REQUEST);

    }

    /**
     *
     * @return
     */

    @ICPControllerLog
    @GetMapping(value = "/{tenantId}/instance_num")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="get number",notes="get number")
    public ResponseEntity getEipCount(@PathVariable  String tenantId) {
        log.info("Get eip getEipCount. {}",tenantId);
        return  eipService.getEipCount();
    }

    // //续费接口
    // @ICPControllerLog
    // @CrossOrigin(origins = "*",maxAge = 3000)
    // @PostMapping(value ="/eips/renew/{eip_id}")
    // @ApiOperation(value = "renew Eip",notes = "renew Eip")
    // public ResponseEntity renewEip(@PathVariable("eip_id") String eipId,@RequestBody EipAllocateParamWrapper eipParam){
    //     log.info("Renew Eip:{}",eipId,eipParam.getEipAllocateParam().toString());
    //     return  eipService.renewEip(eipId,eipParam.getEipAllocateParam());
    // }


}
