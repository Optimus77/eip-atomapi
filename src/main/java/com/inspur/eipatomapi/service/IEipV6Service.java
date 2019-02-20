package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.eip.EipUpdateParam;
import com.inspur.eipatomapi.entity.eip.EipUpdateParamWrapper;
import org.springframework.http.ResponseEntity;

import java.util.List;


public interface IEipV6Service {




    /**
     *   the eipV6
     * @param currentPage  the current page
     * @param limit  element of per page
     * @return       result
     */
    ResponseEntity listEipV6s(int currentPage, int limit, String status);

    /**
     * get detail of the eip
     * @param eipV6Id  the id of the eip instance
     * @return the json result
     */
    ResponseEntity getEipV6Detail(String eipV6Id);


    /**
     * eip bind with port
     * @param id      id
     * @param eipAddress
     * @return        result
     */
    ResponseEntity eipV6bindPort(String id,String eipAddress);


}
