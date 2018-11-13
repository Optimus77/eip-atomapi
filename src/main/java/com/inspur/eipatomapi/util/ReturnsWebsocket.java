package com.inspur.eipatomapi.util;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.bss.EipReciveOrder;
import com.inspur.eipatomapi.entity.eip.SendMQEIP;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

public class ReturnsWebsocket {

    @Value("${bssURL.pushMq}")
    static private String pushMq;

    public final static Logger log = LoggerFactory.getLogger(ReturnsWebsocket.class);

    public static void get(String eipId,EipReciveOrder eipOrder,String type){
        if ("console".equals(eipOrder.getReturnConsoleMessage().getOrderSource())){
            SendMQEIP sendMQEIP = new SendMQEIP();
            try {
                sendMQEIP.setUserName(CommonUtil.getUsername());
                sendMQEIP.setHandlerName("operateEipHandler");
                sendMQEIP.setInstanceId(eipId);
                sendMQEIP.setInstanceStatus("active");
                sendMQEIP.setOperateType(type);
                sendMQEIP.setMessageType("success");
                sendMQEIP.setMessage(CodeInfo.getCodeMessage(CodeInfo.EIP_RENEWAL_SUCCEEDED));
                String url=pushMq;
                log.info(url);
                String orderStr=JSONObject.toJSONString(sendMQEIP);
                log.info("return mq body str {}",orderStr);
                Map<String,String> headers = new HashMap<>();
                headers.put("Authorization", CommonUtil.getKeycloackToken());
                headers.put(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
                HttpResponse response = HttpUtil.post(url,headers,orderStr);
                log.info(response.getEntity().toString());
                log.info(String.valueOf(response.getStatusLine().getStatusCode()));
            } catch (KeycloakTokenException e) {
                e.printStackTrace();
            }
        }else {
            log.info("Wrong source of order",eipOrder.getReturnConsoleMessage().getOrderSource());
        }
    }
}
