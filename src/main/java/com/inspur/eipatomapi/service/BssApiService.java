package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.bss.EipCalculation;
import com.inspur.eipatomapi.entity.bss.EipOrder;
import com.inspur.eipatomapi.entity.bss.EipQuota;
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
import java.io.IOException;
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

    @Value("${bssURL.host}")
    private   String host;
    @Value("${bssURL.port}")
    private   String port;
    @Value("${bssURL.ignoSSL}")
    private   boolean ignoSSL;

    private String getURL(){

        if(ignoSSL){
            return "http://"+host+":"+port;
        }else{
            return "https://"+host+":"+port;
        }
    }

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

    //1.2.1 查询当前用户余额
    @Value("${bssURL.userBalanceURL}")
    private   String userBalanceURL;
    public JSONObject getUserBalance(String userid){
        String  uri=userBalanceURL+"/account/"+userid+"/balance";
        log.info(uri);
        Map<String,String> header= getHeader();
        HttpResponse response= HttpUtil.get(uri,header);
        return handlerResopnse(response);
    }

    //1.2.6	查询用户可购买的产品列表
    @Value("${bssURL.avliableProductList}")
    private   String avliableProductList;
    public JSONObject avliableProductList(String userId,String productLineCode,String region){
        String  uri=avliableProductList+"/product"+"?userId="+userId+"&productLineCode="+productLineCode+"&region="+region;
        log.info(uri);
        Map<String,String> header= getHeader();
        HttpResponse response= HttpUtil.get(uri,header);
        return handlerResopnse(response);
    }


    //1.2.8 订单接口POST
    @Value("${bssURL.ordercreate}")
    private   String ordercreate;
    public JSONObject createOrder(EipOrder order)  {
        String url=getURL()+"/order/create";
        log.info(url);
        Map<String,String> header= getHeader();
        String orderStr=JSONObject.toJSONString(order);
        log.info("body str {}",orderStr);
        HttpResponse response=HttpUtil.post(url,header,orderStr);
        return handlerResopnse(response);
    }

    //1.2.7 算费接口 http://117.73.2.105:8085/product/price
    @Value("${bssURL.orderprice}")
    private   String orderprice;
    public JSONObject getCalculation(EipCalculation calculation){
        JSONObject result=new JSONObject();
        String  uri=orderprice+"/product/price";
        log.info(uri);
        Map<String,String> header= getHeader();
        String calculationStr=JSONObject.toJSONString(calculation);
        log.info(" calculationStr body str {}",calculationStr);
        HttpResponse response=HttpUtil.post(uri,header,calculationStr);
        return handlerResopnse(response);
    }

    //1.2.11	查询用户配额的接口 URL: http://117.73.2.105:8083/crm/quota
    @Value("${bssURL.quotaUrl}")
    private   String quotaUrl;
    public JSONObject getQuota(EipQuota quota){
        JSONObject result=new JSONObject();
        String  uri=quotaUrl+"/mock/5bbda32758c3ee17c7086191/crm/quota?userId="+quota.getUserId()+"&region="+quota.getRegion()+"&productLineCode="+quota.getProductLineCode()+"&productTypeCode="+quota.getProductTypeCode();
        log.info(uri);
        Map<String,String> header= getHeader();
        HttpResponse response= HttpUtil.get(uri,header);
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
