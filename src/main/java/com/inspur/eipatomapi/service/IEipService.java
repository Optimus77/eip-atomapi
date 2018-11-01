package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.bss.EipOrder;
import com.inspur.eipatomapi.entity.eip.EipAllocateParam;
import com.inspur.eipatomapi.entity.eip.EipUpdateParamWrapper;
import org.springframework.http.ResponseEntity;

import java.util.List;


public interface IEipService {

    /**
     * create a eip
     * @param eipConfig          config
     * @return                   json info of eip
     */

    ResponseEntity createEip(EipOrder eipConfig);



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
     * 1.delete  eip
     * 2.Determine if Snate and Qos is deleted
     * 3.delete eip
     *
     * @param eipId  eip id
     * @param eipOrder  eip order
     * @return       result: true/false
     */

    ResponseEntity deleteEip(String eipId, EipOrder eipOrder);



    /**
     *  list the eip
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    ResponseEntity listEips(int currentPage,int limit,boolean returnFloatingip);

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
    ResponseEntity eipbindPort(String id,String type, String serverId, String portId);

    /**
     * un bind port
     * @param id    id
     * @return      result
     */
    ResponseEntity unBindPort(String id);


    /**
     * add eip into eip pool for test
     */
    void addEipPool(String ip);

    /**
     * list all server of current users
     */
    ResponseEntity listServer();

    /**
     * get eip by floating ip
     */
    ResponseEntity getEipByInstanceId(String instanceId);

    /**
     * get eip number of ther user
     */
    ResponseEntity getEipNumber();

    ResponseEntity getEipByIpAddress(String eip);


    ResponseEntity getEipCount();

    //1.2.1 查询当前用户余额
    ResponseEntity userBanlance();


    //1.2.8	订单提交接口
    ResponseEntity createOrder(EipAllocateParam eipAllocateParam);

    //1.2.13	配额接口
    ResponseEntity getQuota();





}
