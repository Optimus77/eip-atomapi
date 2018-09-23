package com.inspur.eipatomapi.controller;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.ConstantClassField;
import com.inspur.eipatomapi.entity.EipAllocateParamWrapper;
import com.inspur.eipatomapi.entity.EipUpdateParamWrapper;
import com.inspur.eipatomapi.service.EipService;
import com.inspur.icp.common.util.annotation.ICPControllerLog;
import io.swagger.annotations.*;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;


@RestController
@RequestMapping(value= ConstantClassField.VERSION_REST, produces={"application/json;charset=UTF-8"})
@Api(value = "eips", description = "eipatomapi API")
public class EipController {

    private final static Logger log = Logger.getLogger(EipController.class.getName());
    @Autowired
    private EipService eipService;

    //Todo: find the external net id
    private String floatingnetworkId = "d9c00a35-fea8-4162-9de1-b8100494a11d";

    @ICPControllerLog
    @PostMapping(value = "/eips")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="allocateEip",notes="allocate")
    public JSONObject allocateEip(@RequestBody EipAllocateParamWrapper eipConfig) {
        try {
            return eipService.createEip(eipConfig.getEipAllocateParam(), floatingnetworkId, null);
         } catch (Exception e){
            e.printStackTrace();
        }
        return null;
     }



    @GetMapping(value = "/eips")
    @ApiOperation(value="listeip",notes="list")
    public String listEip(@RequestParam String currentPage ,@RequestParam String limit,@RequestParam String vpcId) {
        log.info("EipController listEip");
        if(currentPage==null){
            currentPage="1";
        }
        if(limit==null){
            limit="10";
        }
        return  eipService.listEips(vpcId,Integer.parseInt(currentPage),Integer.parseInt(limit));
    }



    @RequestMapping(value = "/eips/{eip_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteEip(@PathVariable("eip_id") String eipId) {
        Boolean result = eipService.deleteEip("name", eipId);
        return new ResponseEntity<>(result,HttpStatus.OK);
    }


    /**
     * get eipatomapi instance detail
     * @param eipId  the id of eipatomapi
     * @param authorization --
     * @param region
     * @return
     */
    @ICPControllerLog
    @GetMapping(value = "/eips/{eip_id}")
    @ApiOperation(value = "get detail of  eipatomapi instance", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eipatomapi", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "header", name = "authorization", value = "the token from the keycolock", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "header", name = "region", value = "the region ", required = true, dataType = "String")
    })
    public JSONObject getEipDetail(@PathVariable("eip_id") String eipId, @RequestHeader("authorization")String authorization , @RequestHeader("region")String region){
        return eipService.getEipDetail(eipId);
    }



    @ICPControllerLog
    @PostMapping(value = "/eips/{eip_id}/port", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "eipBindWithPort", notes = "")
    @Transactional
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eipatomapi", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "header", name = "authorization", value = "the token from the keycolock", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "header", name = "region", value = "the region ", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "body",   name = "param", value = "the json param ", required = true, dataType = "String")
    })
    public ResponseEntity eipBindWithPort(@PathVariable("eip_id") String eipId, @RequestBody EipUpdateParamWrapper param,@RequestHeader("authorization")String authorization ,@RequestHeader("region")String region) {

        if(param.getEipUpdateParam().getPortId()!=null){
            String result=eipService.eipbindPort(eipId,param.getEipUpdateParam().getPortId());
            return new ResponseEntity(result, HttpStatus.OK);
        }else{
            return new ResponseEntity("{error:\"port_id is not null\"}", HttpStatus.OK);
        }

    }

    @ICPControllerLog
    @DeleteMapping(value = "/eips/{eip_id}/port", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "eipUnbinWithPort", notes = "")
    @Transactional
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eipatomapi", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "header", name = "authorization", value = "the token from the keycolock", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "header", name = "region", value = "the region ", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "body",   name = "param", value = "the json ", required = true, dataType = "String")
    })
    public ResponseEntity eipUnbindWithPort(@PathVariable("eip_id") String eipId, @RequestBody EipUpdateParamWrapper param,@RequestHeader("authorization")String authorization ,@RequestHeader("region")String region) {

        String result=eipService.unBindPort(eipId);
        return new ResponseEntity(result, HttpStatus.OK);

    }

    @ICPControllerLog
    @PutMapping(value = "/eips/{eip_id}/bindwidth", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update eipatomapi bandWidth", notes = "")
    @Transactional
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", name = "eip_id", value = "the id of eipatomapi", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "header", name = "authorization", value = "the token from the keycolock", required = true, dataType = "String"),
            @ApiImplicitParam(paramType = "header", name = "region", value = "the region ", required = true, dataType = "String"),
            //@ApiImplicitParam(paramType = "body",   name = "param", value = "the json ", required = true, dataType = "String")
    })
    public String changeEipBandWidht(@PathVariable("eip_id") String eipId, @RequestBody EipUpdateParamWrapper param,@RequestHeader("authorization")String authorization ,@RequestHeader("region")String region) {
        log.info(eipId);
        log.info(JSONObject.toJSONString(param));
        log.info(region);
        return eipService.updateEipBandWidth(eipId,param);
    }
    //add for test
    @ICPControllerLog
    @PostMapping(value = "/eips")
    @CrossOrigin(origins = "*",maxAge = 3000)
    @ApiOperation(value="addEipPool",notes="add eipatomapi")
    public ResponseEntity<String> addEipPool() {
        try {
            eipService.addEipPool();
        } catch (Exception e){
            e.printStackTrace();
        }
        return new ResponseEntity<>("True", HttpStatus.OK);
    }

}