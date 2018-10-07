package com.inspur.eipatomapi.controller;

import com.inspur.eipatomapi.config.ConstantClassField;
import com.inspur.eipatomapi.entity.*;
import com.inspur.eipatomapi.service.impl.EipServiceImpl;
import com.inspur.icp.common.util.annotation.ICPControllerLog;
import io.swagger.annotations.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value= ConstantClassField.VERSION_REST, produces={"application/json;charset=UTF-8"})
@Api(value = "eips", description = "eip API")
public class EipController {

    private final static Log log = LogFactory.getLog(EipController.class);


    @Autowired
    private EipServiceImpl eipService;

    //Todo: find the external net id
    private String floatingnetworkId = "d9c00a35-fea8-4162-9de1-b8100494a11d";

    @ICPControllerLog
    @PostMapping(value = "/eips")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="allocateEip",notes="allocate")
    public ResponseEntity allocateEip(@RequestBody EipAllocateParamWrapper eipConfig) {
        log.info(eipConfig);
        return eipService.createEip(eipConfig.getEipAllocateParam(), floatingnetworkId, null);
     }


    @ICPControllerLog
    @GetMapping(value = "/eips")
    @ApiOperation(value="listeip",notes="list")
    public String listEip(@RequestParam String currentPage , @RequestParam String limit) {
        log.info("EipController listEip");
        if(currentPage==null){
            currentPage="1";
        }
        if(limit==null){
            limit="10";
        }
        return  eipService.listEips(Integer.parseInt(currentPage),Integer.parseInt(limit),false);
    }



    @DeleteMapping(value = "/eips/{eip_id}")
    @ICPControllerLog
    @ApiOperation(value = "deleteEip")
    public ResponseEntity deleteEip(@PathVariable("eip_id") String eipId) {
        //Check the parameters

        log.warn("Delete the Eip "+eipId);
        return eipService.deleteEip(eipId);

    }


    @PostMapping(value = "/eips/deleiplist", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ICPControllerLog
    @ApiOperation(value = "deleiplist")
    public ResponseEntity deleteEipList(@RequestBody EipDelParam param) {
        //Check the parameters

        log.warn("Delete the Eip "+param);
        return eipService.deleteEipList(param.getEipids());
    }


    /**
     * get eip instance detail
     * @param eipId  the id of eip
     * @return  retrun
     */
    @ICPControllerLog
    @GetMapping(value = "/eips/{eip_id}")
    @ApiOperation(value = "get detail of  eip instance", notes = "get")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public ResponseEntity getEipDetail(@PathVariable("eip_id") String eipId){
        return eipService.getEipDetail(eipId);
    }



    @ICPControllerLog
    @PostMapping(value = "/eips/bind/{eip_id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "eipBindWithServer", notes = "get")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public ResponseEntity eipBindWithPort(@PathVariable("eip_id") String eipId, @RequestBody EipUpdateParamWrapper param ) {
        return eipService.eipbindPort(eipId,param.getEipUpdateParam().getType(),
                param.getEipUpdateParam().getServerId(),
                param.getEipUpdateParam().getPortId());
    }

    @ICPControllerLog
    @PostMapping(value = "/eips/unbind/{eip_id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "eipUnbinWithServer", notes = "get")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public ResponseEntity eipUnbindWithPort(@PathVariable("eip_id") String eipId) {
        return eipService.unBindPort(eipId);
    }

    @ICPControllerLog
    @PutMapping(value = "/eips/{eip_id}/bindwidth", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update eip bandWidth", notes = "put")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public ResponseEntity changeEipBandWidht(@PathVariable("eip_id") String eipId, @RequestBody EipUpdateParamWrapper param) {
        return eipService.updateEipBandWidth(eipId,param);
    }


    @ICPControllerLog
    @PostMapping(value = "/eips/addeippool")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="addEipPool",notes="add eip")
    public ResponseEntity<String> addEipPool( @RequestParam String ip) {
        eipService.addEipPool(ip);
        return new ResponseEntity<>("True", HttpStatus.OK);
    }


    @ICPControllerLog
    @GetMapping(value = "/eips/servers")
    @ApiOperation(value = "show all servers", notes = "get")
    public String getServerList() {
        return eipService.listServer();
    }

    @ICPControllerLog
    @GetMapping(value = "/eips/eips_ext")
    @ApiOperation(value="listeip",notes="list")
    public String listEipExt(@RequestParam String currentPage , @RequestParam String limit) {
        log.info("EipController listEip ext");
        if(currentPage==null){
            currentPage="1";
        }
        if(limit==null){
            limit="10";
        }
        return  eipService.listEips(Integer.parseInt(currentPage),Integer.parseInt(limit),true);
    }

    @ICPControllerLog
    @GetMapping(value = "/eips/instance/{instance_id}")
    @ApiOperation(value="getEipByInstanceId",notes="get")
    public ResponseEntity getEipByInstanceId(@PathVariable String instance_id) {
        log.info("EipController get eip by instance id.");
        return  eipService.getEipByInstanceId(instance_id);
    }


    @ICPControllerLog
    @GetMapping(value = "/eips/eipaddress/{eipaddress}")
    @ApiOperation(value="getEipByEipAddress",notes="get")
    public ResponseEntity getEipByEipAddress(@PathVariable String eipaddress) {
        log.info("EipController get eip by instance id.");
        return  eipService.getEipByIpAddress(eipaddress);
    }

    @ICPControllerLog
    @GetMapping(value = "/eips/eipnumber")
    @ApiOperation(value="get number",notes="get number")
    public ResponseEntity getEipNumber() {
        log.info("Get eip number.");
        return  eipService.getEipNumber();
    }

}
