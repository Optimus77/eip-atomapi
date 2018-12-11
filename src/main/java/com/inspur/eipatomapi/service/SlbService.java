package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.util.*;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SlbService {

    public final static Logger log = LoggerFactory.getLogger(SlbService.class);

    @Value("${slbUrl}")
    private String slbUrl;

    public boolean isFipInUse(String  vmId) {

        String url = slbUrl + vmId;

        try {
            log.info("Get fip status from slb url:{}, id:{}", url, vmId);

            Map<String,String> header=new HashMap<String,String>();
            header.put(HsConstants.AUTHORIZATION, CommonUtil.getKeycloackToken());
            header.put(HTTP.CONTENT_TYPE, "application/json; charset=utf-8");


            String response = HttpUtil.get(url, header);
            JSONObject returnInfo = JSONObject.parseObject(response);
            log.info("Slb return info:{}", returnInfo);
            if((null != returnInfo) && (null != returnInfo.getString("message"))) {
                if (returnInfo.getString("message").equalsIgnoreCase("true")) {
                    return true;
                } else {
                    return false;
                }
            }
        }catch (Exception e){
            log.error("Exception Get fip status from slb error");
        }
        return true;
    }

}
