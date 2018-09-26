package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;

import com.inspur.eipatomapi.entity.EipAllocateParam;
import com.inspur.eipatomapi.entity.EipUpdateParamWrapper;


public interface IEipService {

    /**
     * create a eip
     * @param eipConfig          config
     * @param externalNetWorkId  external network id
     * @param portId             port id
     * @return                   json info of eip
     * @throws Exception         e
     */

    JSONObject createEip(EipAllocateParam eipConfig, String externalNetWorkId, String portId)throws Exception;



    /**
     * 1.delete  floatingIp
     * 2.Determine if Snate and Qos is deleted
     * 3.delete eip
     *
     * @param name  name
     * @param eipId  eip ip
     * @return       result: true/false
     */

    Boolean deleteEip(String name, String eipId)throws Exception ;

    /**
     *  list the eip
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    String listEips(int currentPage,int limit);

    /**
     * get detail of the eip
     * @param eipId  the id of the eip instance
     * @return the json result
     */
    JSONObject getEipDetail(String eipId);


    /**
     * update eip band width
     * @param id    id
     * @param param param
     * @return      result
     */
    String updateEipBandWidth(String id, EipUpdateParamWrapper param);


    /**
     * eip bind with port
     * @param id      id
     * @param portId  port id
     * @param type   //1：ecs // 2：cps // 3：slb
     * @return        result
     */
    String eipbindPort(String id,String type,String portId);

    /**
     * un bind port
     * @param id    id
     * @return      result
     */
    String unBindPort(String id);


    /**
     * add eip into eip pool for test
     */
    void addEipPool();

    /**
     * list all server of current users
     */
    String listServer();

}
