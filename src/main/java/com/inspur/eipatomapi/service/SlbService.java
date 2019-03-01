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
        if(null ==  vmId || vmId.isEmpty()) {
            return false;
        }
        if(slbUrl.equals("nourl")){
            return false;
        }
        String url = slbUrl + vmId;

        try {
            log.info("Get fip status from slb url:{}, id:{}", url, vmId);

            Map<String,String> header=new HashMap<>();
            header.put(HsConstants.AUTHORIZATION, CommonUtil.getKeycloackToken());
            header.put(HTTP.CONTENT_TYPE, "application/json; charset=utf-8");
            HttpResponse response = HttpUtil.doGet(url, null, header);
            log.info("Slb return info:{}", response.toString());
            JSONObject returnInfo = JSONObject.parseObject(response.getResponseBody());
            if((null != returnInfo) && (null != returnInfo.getString("message"))&&!returnInfo.getString("message").equalsIgnoreCase("true")) {
                    return false;
            }
        }catch (Exception e){
            log.error("Exception Get fip status from slb error");
        }
        return true;
    }

}
