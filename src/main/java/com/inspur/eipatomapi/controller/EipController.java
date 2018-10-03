package com.inspur.eipatomapi.controller;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.ConstantClassField;
import com.inspur.eipatomapi.entity.EipAllocateParamWrapper;
import com.inspur.eipatomapi.entity.EipUpdateParamWrapper;
import com.inspur.eipatomapi.service.impl.EipServiceImpl;
import com.inspur.eipatomapi.util.FastjsonUtil;
import com.inspur.icp.common.util.annotation.ICPControllerLog;
import io.swagger.annotations.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
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
    public JSONObject allocateEip(@RequestBody EipAllocateParamWrapper eipConfig) {
        log.info(eipConfig);
        try {
            return eipService.createEip(eipConfig.getEipAllocateParam(), floatingnetworkId, null);
         } catch (Exception e){
            e.printStackTrace();
        }
        return null;
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



    @RequestMapping(value = "/eips/{eip_id}", method = RequestMethod.DELETE)
    @ICPControllerLog
    @ApiOperation(value = "deleteEip")
    public ResponseEntity<String> deleteEip(@PathVariable("eip_id") String id) {
        //Check the parameters
        if (id == null || id.length() == 0) {
            return new ResponseEntity<>("Id is not empty ", HttpStatus.BAD_REQUEST);
        }
        try {
            log.info("Delete the Eip");
            Boolean result = eipService.deleteEip("name", id);
            return new ResponseEntity<>(FastjsonUtil.toJSONString(result), HttpStatus.OK);
        } catch (Exception e) {
            log.info("Delete failed");
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @ICPControllerLog
    @GetMapping(value = "/eips_fixed")
    @ApiOperation(value = "The eip information is obtained through fixed IP", notes = "")
    public JSONObject fixedIpGetEipDetail(@RequestParam String fixedIp , @RequestParam String vpcId)  {
        return eipService.fixedIpgetEipDetail(fixedIp,vpcId);
    }


    /**
     * get eip instance detail
     * @param eipId  the id of eip
     * @return  retrun
     */
    @ICPControllerLog
    @GetMapping(value = "/eips/{eip_id}")
    @ApiOperation(value = "get detail of  eip instance", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public JSONObject getEipDetail(@PathVariable("eip_id") String eipId){
        return eipService.getEipDetail(eipId);
    }



    @ICPControllerLog
    @PostMapping(value = "/eips/bind/{eip_id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "eipBindWithServer", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public String eipBindWithPort(@PathVariable("eip_id") String eipId, @RequestBody EipUpdateParamWrapper param ) {
        return eipService.eipbindPort(eipId,param.getEipUpdateParam().getType(),param.getEipUpdateParam().getServerId());
    }

    @ICPControllerLog
    @PostMapping(value = "/eips/unbind/{eip_id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "eipUnbinWithServer", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public String eipUnbindWithPort(@PathVariable("eip_id") String eipId) {
        return eipService.unBindPort(eipId);
    }

    @ICPControllerLog
    @PutMapping(value = "/eips/{eip_id}/bindwidth", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update eip bandWidth", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eip", required = true, dataType = "String"),
    })
    public String changeEipBandWidht(@PathVariable("eip_id") String eipId, @RequestBody EipUpdateParamWrapper param) {
        return eipService.updateEipBandWidth(eipId,param);
    }


    @ICPControllerLog
    @PostMapping(value = "/eips/addeippool")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="addEipPool",notes="add eip")
    public ResponseEntity<String> addEipPool( @RequestParam String ip) {
        try {
            eipService.addEipPool(ip);
        } catch (Exception e){
            e.printStackTrace();
        }
        return new ResponseEntity<>("True", HttpStatus.OK);
    }


    @ICPControllerLog
    @GetMapping(value = "/servers/")
    @ApiOperation(value = "show all servers", notes = "")
    public String getServerList() {
        return eipService.listServer();
    }

    @ICPControllerLog
    @GetMapping(value = "/eips_ext")
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
    @GetMapping(value = "/instance/{instance_id}")
    @ApiOperation(value="getEipByInstanceId",notes="get")
    public JSONObject getEipByInstanceId(@PathVariable String instance_id) {
        log.info("EipController get eip by instance id.");
        return  eipService.getEipByInstanceId(instance_id);
    }

}
