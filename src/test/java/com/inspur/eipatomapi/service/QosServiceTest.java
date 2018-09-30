package com.inspur.eipatomapi.service;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class QosServiceTest {

    @Test
    public void createQosPipe() {
        String ip = "192.168.3.1";
        String port = "443";
        String user = "InnetAdmin";
        String passwd = "innetadmin";
        QosService qs = new QosService(ip, port, user, passwd);
        HashMap<String, String> map = new HashMap<>();
        map.put("pipeName", "1qostest");
        map.put("ip", "1.2.3.8");
        map.put("serviceNamne", "Any");
        map.put("mgNetCardName", "ethernet0/2");
        map.put("serNetCardName", "ethernet0/1");
        map.put("bandWidth", "2");
        HashMap<String, String> res = qs.createQosPipe(map);
        System.out.println(res.get("id"));

        String pipid = res.get("id");
        System.out.println(pipid);
            //添加管道成功，更新数据库

    }

    @Test
    public void delQosPipe() {
        String ip = "192.168.3.1";
        String port = "443";
        String user = "InnetAdmin";
        String passwd = "innetadmin";
        QosService qs = new QosService(ip, port, user, passwd);


        HashMap<String, String> res = qs.delQosPipe("1538330378892617132");
        System.out.println(res.get("id"));


    }
}