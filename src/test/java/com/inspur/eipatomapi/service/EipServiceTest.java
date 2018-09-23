package com.inspur.eipatomapi.service;


import org.junit.Before;
import org.junit.Test;

public class EipServiceTest {

    private String floatingnetworkId = "d9c00a35-fea8-4162-9de1-b8100494a11d";
    private EipService eipService ;
    @Before
    public void init(){
        eipService = new EipService();
    }
    @Test
    public void createFloatingIp() {
//
//        NetFloatingIP netFloatingIP= eipService.createFloatingIp("region", floatingnetworkId, null);
//        System.out.println(netFloatingIP);
//        assertNotEquals("return is null",null, netFloatingIP);
    }

    @Test
    public void deleteFloatingIp() {
//        Boolean result = eipService.deleteFloatingIp("name","bffd406b-fe90-47e9-9ee3-8918c6c36651");
//        System.out.println(result);
//        assertEquals(true, result);
    }


}