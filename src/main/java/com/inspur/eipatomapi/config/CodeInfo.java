package com.inspur.eipatomapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

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
    public static final String KEYCLOAK_TOKEN_EXPIRED="KEYCLOAK_TOKEN_ERROR";


    public static final String EIP_BIND_NOT_FOND ="EIP_BIND_NOT_FOND";
    public static final String EIP_BIND_HAS_BAND ="EIP_BIND_HAS_BAND";
    public static final String EIP_BIND_PARA_SERVERID_ERROR="EIP_BIND_PARA_SERVERID_ERROR";
    public static final String EIP_BIND_OPENSTACK_ASSOCIA_FAIL="EIP_BIND_OPENSTACK_ASSOCIA_FAIL";
    public static final String EIP_BIND_OPENSTACK_ERROR ="EIP_BIND_OPENSTACK_ERROR";
    public static final String EIP_BIND_FIREWALL_ERROR  ="EIP_BIND_FIREWALL_ERROR";
    public static final String EIP_BIND_FIREWALL_DNAT_ERROR="EIP_BIND_FIREWALL_DNAT_ERROR";
    public static final String EIP_BIND_FIREWALL_SNAT_ERROR="EIP_BIND_FIREWALL_SNAT_ERROR";
    public static final String EIP_BIND_FIREWALL_QOS_ERROR="EIP_BIND_FIREWALL_QOS_ERROR";
    public static final String EIP_CHANGE_BANDWIDTH_ERROR="EIP_CHANGE_BANDWIDTH_ERROR";
    public static final String EIP_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR="EIP_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR";



    static class CnCode{
        public static final String KEYCLOAK_NULL="400-Bad request: http 头信息中无法获得Authorization 参数";
        public static final String KEYCLOAK_TOKEN_EXPIRED="401-Unauthorized:从token中获取projectid出错,请检查token是否过期";

        //bind interface
        public static final String EIP_BIND_NOT_FOND="404-Bad request: 根据此id无法找到对应的EIP信息";
        public static final String EIP_BIND_HAS_BAND="404-Bad request: 此EIP已经绑定，无法再次绑定";
        public static final String EIP_BIND_PARA_SERVERID_ERROR="404-Bad request: 需要参数serverid";
        public static final String EIP_BIND_OPENSTACK_ASSOCIA_FAIL="绑定浮动ip返回失败";
        public static final String EIP_BIND_OPENSTACK_ERROR="绑定时openstack出错";
        public static final String EIP_BIND_FIREWALL_ERROR="绑定时防火墙出错";
        public static final String EIP_BIND_FIREWALL_DNAT_ERROR="绑定时防火墙添加DNAT出错";
        public static final String EIP_BIND_FIREWALL_SNAT_ERROR="绑定时防火墙添加SNAT出错";
        public static final String EIP_BIND_FIREWALL_QOS_ERROR="绑定时防火墙添加QOS出错";

        //changebandwidth interface
        public static final String EIP_CHANGE_BANDWIDTH_ERROR="修改带宽时防火墙出错";
        public static final String EIP_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR="包年包月带宽只能调大";




    }

    public static String getCodeMessage(String key){
        try {
            Field field= EnCode.class.getField(key);
            return String.valueOf(field.get(new EnCode()));
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
        public static final String KEYCLOAK_NULL="400-Bad request:can't get Authorization info from header,please check";
        public static final String KEYCLOAK_TOKEN_EXPIRED="401-Unauthorized:get projecctid from token,please check it expired";

        //bind interface
        public static final String EIP_BIND_NOT_FOND="404-Bad request: can't find eip info by this id";
        public static final String EIP_BIND_HAS_BAND="404-Bad request: this eip has banded";
        public static final String EIP_BIND_PARA_SERVERID_ERROR="404-Bad request:  needs param serverid";
        public static final String EIP_BIND_OPENSTACK_ASSOCIA_FAIL="band floating ip fail";
        public static final String EIP_BIND_OPENSTACK_ERROR="openstack error when band server";
        public static final String EIP_BIND_FIREWALL_ERROR="fillware error when band server";
        public static final String EIP_BIND_FIREWALL_DNAT_ERROR="add DNAT rule error when band server";
        public static final String EIP_BIND_FIREWALL_SNAT_ERROR="add SNAT rule error when band server";
        public static final String EIP_BIND_FIREWALL_QOS_ERROR="add  QOS  rule error when band server";

        //changebandwidth interface
        public static final String EIP_CHANGE_BANDWIDTH_ERROR="the fillware error when update the bandwidht";
        public static final String EIP_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR="the bandwidth must bigger than orgin when choose prepaid modle";


    }






}
