package com.inspur.eipatomapi.controller;

import com.inspur.eipatomapi.config.ConstantClassField;
import com.inspur.eipatomapi.entity.bss.EipReciveOrder;
import com.inspur.eipatomapi.entity.bss.EipSoftDownOrder;
import com.inspur.eipatomapi.entity.eip.EipAllocateParam;
import com.inspur.eipatomapi.entity.eip.EipAllocateParamWrapper;
import com.inspur.eipatomapi.entity.eip.EipDelParam;
import com.inspur.eipatomapi.entity.eip.EipUpdateParamWrapper;
import com.inspur.eipatomapi.service.impl.EipServiceImpl;
import com.inspur.eipatomapi.util.HsConstants;
import com.inspur.eipatomapi.util.ReturnMsgUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import com.inspur.icp.common.util.annotation.ICPControllerLog;
import io.swagger.annotations.*;
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
@Api(value = "/v1", description = "eip API")
@Validated
public class EipController {

    private final static Logger log = LoggerFactory.getLogger(EipController.class);

    @Autowired
    private EipServiceImpl eipService;



    @ICPControllerLog
    @PostMapping(value = "/eips")
    @CrossOrigin(origins = "*",maxAge = 3000)
    public ResponseEntity atomAllocateEip(@Valid @RequestBody EipAllocateParamWrapper eipConfig, BindingResult result) {
        log.info("Allocate a eip:{}.", eipConfig.getEipAllocateParam().toString());
        if (result.hasErrors()) {
            StringBuffer msgBuffer = new StringBuffer();
            List<FieldError> fieldErrors = result.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                msgBuffer.append(fieldError.getField() + ":" + fieldError.getDefaultMessage());
            }
            return new ResponseEntity<>(ReturnMsgUtil.error(ReturnStatus.SC_PARAM_ERROR, msgBuffer.toString()),
                    HttpStatus.BAD_REQUEST);
        }
        return eipService.atomCreateEip(eipConfig.getEipAllocateParam());
    }


    @DeleteMapping(value = "/eips/{eip_id}")
    @ICPControllerLog
    @CrossOrigin(origins = "*",maxAge = 3000)
    public ResponseEntity atomDeleteEip(@Size(min=36, max=36, message = "Must be uuid.")
                                        @PathVariable("eip_id") String eipId) {
        //Check the parameters
        log.info("Atom delete the Eip:{} ",eipId);
        return eipService.atomDeleteEip(eipId);

    }

    @ICPControllerLog
    @GetMapping(value = "/eips")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="listeip",notes="list")
    public ResponseEntity listEip(@RequestParam(required = false) String currentPage ,
                                  @RequestParam(required = false )String limit,
                                  @RequestParam(required = false )String status) {
        log.info("EipController listEip, currentPage:{}, limit:{}", currentPage, limit);
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
        return  eipService.listEips(Integer.parseInt(currentPage),Integer.parseInt(limit),status);
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
    @GetMapping(value = "/eips/search")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="getEipByInstanceId",notes="get")
    public ResponseEntity getEipByInstanceId(@RequestParam(required = false) String resourceid,
                                             @RequestParam(required = false) String eipaddress) {
        if((null == resourceid) && (null == eipaddress) ){
            return new ResponseEntity<>("not found.", HttpStatus.NOT_FOUND);
        }
        if((null != resourceid) && (null != eipaddress) ){
            return new ResponseEntity<>("To be wrong.", HttpStatus.FORBIDDEN);
        }
        if(null != resourceid) {
            log.info("EipController get eip by instance id:{} ", resourceid);
            return eipService.getEipByInstanceId(resourceid);
        } else if(null != eipaddress) {
            log.info("EipController get eip by ip:{} ", eipaddress);
            return eipService.getEipByIpAddress(eipaddress);
        }
        return new ResponseEntity<>("not found.", HttpStatus.NOT_FOUND);
    }

    @ICPControllerLog
    @PostMapping(value = "/ips")
    @CrossOrigin(origins = "*",maxAge = 3000)
    public ResponseEntity addEipPool( @RequestParam String ip,  @RequestParam String eip) {
        eipService.addEipPool(ip, eip);
        return new ResponseEntity<>(ReturnMsgUtil.success(), HttpStatus.OK);
    }


    @ICPControllerLog
    @GetMapping(value = "/servers")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "show all servers", notes = "get")
    public ResponseEntity getServerList(@RequestParam String region) {
        return eipService.listServer(region);
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
                log.info("unbind operate, eipid:{}, param:{} ",eipId, param.getEipUpdateParam() );
                return eipService.unBindPort(eipId);

            }else{
                log.info("bind operate, eipid:{}, param:{}",eipId, param.getEipUpdateParam() );
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
            if(param.getEipUpdateParam().getBillType()==null&&param.getEipUpdateParam().getBandWidth()==0){
                log.info("unbind operate, eipid:{}, param:{} ",eipId, param.getEipUpdateParam() );
                return eipService.unBindPort(eipId);
            }else{
                if(param.getEipUpdateParam().getBillType()!=null&&param.getEipUpdateParam().getBandWidth()!=0){

                    boolean chargeTypeFlag=false;
                    if(param.getEipUpdateParam().getBillType().equals(HsConstants.MONTHLY)||
                            param.getEipUpdateParam().getBillType().equals(HsConstants.HOURLYSETTLEMENT)){
                        chargeTypeFlag=true;
                    }else{
                        msg="chargetype must be [monthly |hourlySettlement]";
                    }
                    if(chargeTypeFlag){
                        log.info("update bandwidth, eipid:{}, param:{} ",eipId, param.getEipUpdateParam() );
                        return eipService.updateEipBandWidth(eipId,param);
                    }
                }else{
                    msg="param not correct. " +
                            "to bind server,body param like{\"eip\" : {\"prot_id\":\"xxx\",\"serverid\":\"xxxxxx\",\"type\":\"[1|2|3]\"}" +
                            "to unbind server , param like {\"eip\" : {\"prot_id\":\"\"} }or   {\"eip\" : {} }" +
                            "to change bindwidht,body param like {\"eip\" : {\"bandwidth\":xxx,\"billType\":\"xxxxxx\"}"  +
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
    @GetMapping(value = "/eipnumbers")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="get number",notes="get number")
    public ResponseEntity getEipCount() {
        log.info("Get eip getEipCount.");
        return  eipService.getEipCount();
    }

    @ICPControllerLog
    @PostMapping(value = "/eips/{eip_id}/renew")
    @CrossOrigin(origins = "*",maxAge = 3000)
    public ResponseEntity renewEip(@PathVariable("eip_id") String eipId,
                                   @RequestBody EipAllocateParam param ) {
        log.info("Renew a eip:{}, order:{}.", eipId, param.toString());
        return eipService.renewEip(eipId, param);
    }

    @PostMapping(value = "/deleiplist", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ICPControllerLog
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value = "deleiplist")
    public ResponseEntity deleteEipList(@RequestBody EipDelParam param) {
        //Check the parameters

        log.info("Delete the Eips:{}.", param.getEipids().toString());
        return eipService.deleteEipList(param.getEipids());
    }

    @GetMapping(value = "/health-status")
    @CrossOrigin(origins = "*", maxAge = 3000)
    @ApiOperation(value = "health check")
    @ICPControllerLog
    public ResponseEntity EipHealthCheck() {
        //HealthCheck
        String code;
        String msg;
        code = ReturnStatus.SC_OK;
        msg ="The eip is running";
        log.info(msg);

        return new ResponseEntity<>(ReturnMsgUtil.msg(code, msg,null), HttpStatus.OK);
    }

}
