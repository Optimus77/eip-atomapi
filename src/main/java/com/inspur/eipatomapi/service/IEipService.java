package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.eip.EipUpdateParam;
import com.inspur.eipatomapi.entity.eip.EipUpdateParamWrapper;
import org.springframework.http.ResponseEntity;

import java.util.List;


public interface IEipService {


    /**
     * 1.delete  floatingIp
     * 2.Determine if Snate and Qos is deleted
     * 3.delete eip
     *
     * @param eipIds  eip ids
     * @return       result: true/false
     */

    ResponseEntity deleteEipList(List<String> eipIds);

    /**
     *  listShareBandWidth the eip
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    ResponseEntity listEips(int currentPage,int limit, String status);

    /**
     * get detail of the eip
     * @param eipId  the id of the eip instance
     * @return the json result
     */
    ResponseEntity getEipDetail(String eipId);


    /**
     * update eip band width
     * @param id    id
     * @param param param
     * @return      result
     */
    ResponseEntity updateEipBandWidth(String id, EipUpdateParamWrapper param);


    /**
     * eip bind with port
     * @param id      id
     * @param serverId  server id
     * @param type   //1：ecs // 2：cps // 3：slb
     * @param portId   port id
     * @return        result
     */
    ResponseEntity eipbindPort(String id,String type, String serverId, String portId, String slbIp);

    /**
     * un bind port
     * @param id    id
     * @return      result
     */
    ResponseEntity unBindPort(String id);


    /**
     * listShareBandWidth all server of current users
     */
    ResponseEntity listServer(String userRegion, String tag);

    /**
     * get eip by floating ip
     */
    ResponseEntity getEipByInstanceId(String instanceId);

    ResponseEntity getEipByIpAddress(String eip);

    ResponseEntity getEipCount();

    ResponseEntity addEipToSbw(String eipId,  EipUpdateParam eipUpdateParam);

    ResponseEntity removeEipFromSbw(String eipId, EipUpdateParam eipUpdateParam);

    /**
     *   the eipV6
     * @return       result
     */
    ResponseEntity listEipsByBandWidth(String status);



}
