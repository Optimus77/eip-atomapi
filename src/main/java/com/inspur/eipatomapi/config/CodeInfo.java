package com.inspur.eipatomapi.config;

import com.inspur.eipatomapi.service.EipDaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author: jiasirui
 * @date: 2018/10/13 09:52
 * @description:
 */
public  class CodeInfo {

    public final static Logger log = LoggerFactory.getLogger(CodeInfo.class);

    private static final String LANGUAGE_CN ="cn";
    private static final String LANGUAGE_EN="en";


    public static final String KEYCLOAK_NULL="KEYCLOAK_NULL";
    public static final String KEYCLOAK_TOKEN_EXPIRED="KEYCLOAK_TOKEN_EXPIRED";

    public static String getCodeMessage(String key){
        try {
            Field field= CnCode.class.getField(key);
            return String.valueOf(field.get(new CnCode()));
        } catch (Exception e) {
            e.printStackTrace();
            return "error can't get "+key+" info";
        }
    }


    public static String getCodeMessage(String key,String language){

        if(language.equals(LANGUAGE_CN)){
            try {
                Field field= CnCode.class.getField(key);
                return String.valueOf(field.get(new CnCode()));
            } catch (Exception e) {
                //e.printStackTrace();
                return "error can't get "+key+" info";
            }
        }else if(language.equals(LANGUAGE_EN)){
            try {
                Field field= CnCode.class.getField(key);
                return String.valueOf(field.get(new EnCode()));
            } catch (Exception e) {
                //e.printStackTrace();
                return "error can't get "+key+" info";
            }
        }else{
            return "can,t get this language";
        }
    }


    static class EnCode{
        public static final String KEYCLOAD_NULL="400-Bad request:can't get Authorization info from header,please check";
        public static final String KEYCLOAK_TOKEN_EXPIRED="401-Unauthorized:get projecctid from token,please check it expired";

    }
    static class CnCode{
        public static final String KEYCLOAD_NULL="400-Bad request: http 头信息中无法获得Authorization 参数";
        public static final String KEYCLOAK_TOKEN_EXPIRED="401-Unauthorized:从token中获取projectid出错,请检查token是否过期";
    }





}
