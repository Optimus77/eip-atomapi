package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
class SlbService {

    @Value("${slbUrl}")
    private String slbUrl;

    boolean isFipInUse(String  vmId) {

        String url = slbUrl + vmId;

        try {
            log.info("Get fip status from slb url:{}, id:{}", url, vmId);

            Map<String,String> header=new HashMap<>();
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
