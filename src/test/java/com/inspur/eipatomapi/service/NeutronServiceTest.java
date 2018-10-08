package com.inspur.eipatomapi.service;

import org.junit.Before;
import org.junit.Test;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.NetFloatingIP;

import static org.junit.Assert.*;

public class NeutronServiceTest {
    private NeutronService neutronserver;
    @Before
    public void init(){
        neutronserver = new NeutronService();
    }

    @Test
    public void associaPortWithFloatingIp() {
        String fipId = "167cf9ba-723b-42fe-91f1-12408fae78e8";
        String portId = "5e7c47ff-d431-40c5-a867-82fb86fbb256";
        try {
            NetFloatingIP netFloatingIP = neutronserver.associaPortWithFloatingIp(fipId, portId);
            System.out.println(netFloatingIP);
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    // @Test
    // public void associaInstanceWithFloatingIp() {
    //     String fip = "10.110.26.46";
    //     String portId = "de79eb9b-4299-4304-b3e9-d94d94f098ec";
    //     try {
    //         ActionResponse netFloatingIP = neutronserver.associaInstanceWithFloatingIp(fip, portId);
    //         System.out.println(netFloatingIP);
    //     }catch(Exception e){
    //         e.printStackTrace();
    //     }
    // }
}