package com.inspur.eipatomapi.config;

import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Field;


@Slf4j
public  class CodeInfo {

    private static final String LANGUAGE_CN ="cn";
    private static final String LANGUAGE_EN="en";

    public static final String ERROR_KEY="Error can not get the key.";
    public static final String KEYCLOAK_NULL="KEYCLOAK_NULL";
    public static final String KEYCLOAK_TOKEN_EXPIRED="KEYCLOAK_TOKEN_EXPIRED";


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
    public static final String EIP_FORBIDDEN="EIP_FORBIDDEN";
    public static final String EIP_CREATION_SUCCEEDED="EIP_CREATION_SUCCEEDED";
    public static final String EIP_DELETE_SUCCEEDED="EIP_DELETE_SUCCEEDED";
    public static final String EIP_UPDATE_SUCCEEDED="EIP_UPDATE_SUCCEEDED";
    public static final String EIP_RENEWAL_SUCCEEDED="EIP_RENEWAL_SUCCEEDED";
    public static final String EIP_FORBIDEN_WITH_ID="EIP_FORBIDEN_WITH_ID";
    public static final String KEYCLOAK_NO_PROJECT="CLOAK_NO_PROJECT";
    public static final String SLB_BIND_NOT_FOND="SLB_BIND_NOT_FOND";
    public static final String EIP_BILLTYPE_NOT_HOURLYSETTLEMENT="EIP_BILLTYPE_NOT_HOURLYSETTLEMENT";
    public static final String EIP_FLOATINGIP_NULL="EIP_FLOATINGIP_NULL";
    public static final String EIP_Shared_Band_Width_Id_NOT_NULL="EIP_Shared_Band_Width_Id_NOT_NULL";




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
        public static final String EIP_FORBIDDEN ="无权操作";
        public static final String EIP_FORBIDEN_WITH_ID ="无权操作 :{}";

        //Return messages
        public static final String EIP_CREATION_SUCCEEDED="弹性公网IP创建成功";
        public static final String EIP_DELETE_SUCCEEDED="弹性公网IP删除成功";
        public static final String EIP_UPDATE_SUCCEEDED="弹性公网IP更新成功";
        public static final String EIP_RENEWAL_SUCCEEDED="弹性公网IP续费成功";
        public static final String KEYCLOAK_NO_PROJECT="没有项目信息";

        public static final String SLB_BIND_NOT_FOND ="Bad request: 根据此id无法找到对应的SLB信息";

    }

    public static String getCodeMessage(String key){
        try {
            Field field= EnCode.class.getField(key);
            return String.valueOf(field.get(new EnCode()));
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR_KEY;
        }
    }


    public static String getCodeMessage(String key,String language){

        if(language.equals(LANGUAGE_CN)){
            try {
                Field field= CnCode.class.getField(key);
                return String.valueOf(field.get(new CnCode()));
            } catch (Exception e) {
                //e.printStackTrace();
                return ERROR_KEY;
            }
        }else if(language.equals(LANGUAGE_EN)){
            try {
                Field field= CnCode.class.getField(key);
                return String.valueOf(field.get(new EnCode()));
            } catch (Exception e) {
                //e.printStackTrace();
                return ERROR_KEY;
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
        public static final String EIP_BIND_HAS_BAND="404-Bad request: this eip has bind to instance";
        public static final String EIP_BIND_PARA_SERVERID_ERROR="404-Bad request:  needs param serverid";
        public static final String EIP_BIND_OPENSTACK_ASSOCIA_FAIL="bind floating ip fail";
        public static final String EIP_BIND_OPENSTACK_ERROR="openstack error when bind server";
        public static final String EIP_BIND_FIREWALL_ERROR="fillware error when bind server";
        public static final String EIP_BIND_FIREWALL_DNAT_ERROR="add DNAT rule error when bind server";
        public static final String EIP_BIND_FIREWALL_SNAT_ERROR="add SNAT rule error when bind server";
        public static final String EIP_BIND_FIREWALL_QOS_ERROR="add  QOS  rule error when bind server";

        //changebindwidth interface
        public static final String EIP_CHANGE_BANDWIDTH_ERROR="the fillware error when update the bandwidht";
        public static final String EIP_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR="the bandwidth must bigger than orgin when choose prepaid modle";
        public static final String EIP_FORBIDDEN ="Forbidden to operate.";
        public static final String EIP_FORBIDEN_WITH_ID ="Forbidden to operate,id:{}.";
        //Return messages
        public static final String EIP_CREATION_SUCCEEDED="Eip creation succeeded";
        public static final String EIP_DELETE_SUCCEEDED="Eip deletion succeeded";
        public static final String EIP_UPDATE_SUCCEEDED="Eip updated successfully";
        public static final String EIP_RENEWAL_SUCCEEDED="Eip renew success";
        public static final String KEYCLOAK_NO_PROJECT="keycloak has no project info.";

    }






}
