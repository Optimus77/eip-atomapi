package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.bss.EipOrderResult;
import com.inspur.eipatomapi.util.HsConstants;
import com.inspur.eipatomapi.util.HttpUtil;
import org.apache.http.HttpResponse;

import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author: jiasirui
 * @date: 2018/10/24 14:52
 * @description:
 */
@Service
public class BssApiService {

    private final static Logger log = LoggerFactory.getLogger(BssApiService.class);


    private static String getKeycloackToken() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(null != requestAttributes) {
            HttpServletRequest request = requestAttributes.getRequest();
            String keyCloackToken = request.getHeader("authorization");
            log.info(keyCloackToken);
            if (keyCloackToken == null) {
                return null;
            } else {
                return keyCloackToken;
            }
        }
        return null;
    }

    private  Map<String,String> getHeader(){
        Map<String,String> header=new HashMap<String,String>();
        header.put("requestId",UUID.randomUUID().toString());
        header.put("Authorization",getKeycloackToken());
        header.put(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
        header.put(HsConstants.HILLTONE_LANGUAGE, HsConstants.LANG);
        return header;
    }

    //1.2.8 订单返回给控制台的消息
    @Value("${bssURL.returnMq}")
    private   String returnMq;
    public JSONObject resultReturnMq(EipOrderResult orderResult)  {
        String url=returnMq;
        log.info(url);
        Map<String,String> header= getHeader();
        String orderStr=JSONObject.toJSONString(orderResult);
        log.info("body str {}",orderStr);
        HttpResponse response=HttpUtil.post(url,header,orderStr);
        return handlerResopnse(response);
    }


    private JSONObject handlerResopnse(HttpResponse response){
        JSONObject result=new JSONObject();
        StringBuffer sb= new StringBuffer("");
        if(response!=null){
            BufferedReader in=null;
            try{
                in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                in.close();
                JSONObject returnInfo=JSONObject.parseObject(sb.toString());
                log.info("BSS RETURN ==>{}",returnInfo);
                if(returnInfo.containsKey("code")){
                    if(returnInfo.getInteger("code")==0){
                        result.put("success",true);
                        result.put("data",returnInfo.get("result"));
                    }else{
                        result.put("success",false);
                        result.put("data",returnInfo);
                    }
                }else{
                    result.put("success",false);
                    result.put("data",returnInfo);
                }
            }catch(Exception e){
                e.printStackTrace();
                result.put("success",false);
                result.put("data",e.getMessage());
            }
        }else{
            result.put("success",false);
            result.put("data",sb.toString());
        }
        return result;
    }








}
