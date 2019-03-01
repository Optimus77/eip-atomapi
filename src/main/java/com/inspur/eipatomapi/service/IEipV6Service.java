package com.inspur.eipatomapi.service;

import org.springframework.http.ResponseEntity;



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
     * @param eipAddress  ip
     * @return        result
     */
    ResponseEntity eipV6bindPort(String id, String eipAddress);


}
