package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.EipAllocateParam;
import com.inspur.eipatomapi.entity.EipUpdateParamWrapper;
import com.inspur.eipatomapi.entity.ReturnMsg;


public interface IEipService {

    /**
     * create a eip
     * @param eipConfig          config
     * @param externalNetWorkId  external network id
     * @param portId             port id
     * @return                   json info of eip
     */

    ReturnMsg createEip(EipAllocateParam eipConfig, String externalNetWorkId, String portId);



    /**
     * 1.delete  floatingIp
     * 2.Determine if Snate and Qos is deleted
     * 3.delete eip
     *
     * @param name  name
     * @param eipId  eip ip
     * @return       result: true/false
     */

    ReturnMsg deleteEip(String name, String eipId);

    /**
     *  list the eip
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    String listEips(int currentPage,int limit,boolean returnFloatingip);

    /**
     * get detail of the eip
     * @param eipId  the id of the eip instance
     * @return the json result
     */
    ReturnMsg getEipDetail(String eipId);


    /**
     * update eip band width
     * @param id    id
     * @param param param
     * @return      result
     */
    ReturnMsg updateEipBandWidth(String id, EipUpdateParamWrapper param);


    /**
     * eip bind with port
     * @param id      id
     * @param serverId  server id
     * @param type   //1：ecs // 2：cps // 3：slb
     * @param portId   port id
     * @return        result
     */
    ReturnMsg eipbindPort(String id,String type, String serverId, String portId);

    /**
     * un bind port
     * @param id    id
     * @return      result
     */
    ReturnMsg unBindPort(String id);


    /**
     * add eip into eip pool for test
     */
    void addEipPool(String ip);

    /**
     * list all server of current users
     */
    String listServer();

    /**
     * get eip by floating ip
     */
    ReturnMsg getEipByInstanceId(String instanceId);

}
