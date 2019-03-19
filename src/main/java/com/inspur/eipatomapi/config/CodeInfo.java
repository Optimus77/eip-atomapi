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
    public static final String EIP_BIND_EIPV6_ERROR = "EIP_BIND_EIPV6_ERROR";
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
    public static final String EIP_SHARED_BAND_WIDTH_ID_NOT_NULL="EIP_SHARED_BAND_WIDTH_ID_NOT_NULL";


    //SBW
    public static final String SBW_FORBIDDEN="SBW_FORBIDDEN";
    public static final String SBW_NOT_FOND_BY_ID ="EIP_NOT_FOND_BY_ID";
    public static final String SBW_FORBIDEN_WITH_ID="SBW_FORBIDEN_WITH_ID";
    public static final String SBW_CHANGE_BANDWIDTH_ERROR="SBW_CHANGE_BANDWIDTH_ERROR";
    public static final String SBW_THE_NEW_BANDWIDTH_VALUE_ERROR="SBW_THE_NEW_BANDWIDTH_VALUE_ERROR";
    public static final String SBW_BILLTYPE_NOT_HOURLYSETTLEMENT="SBW_BILLTYPE_NOT_HOURLYSETTLEMENT";
    public static final String SBW_DELETE_ERROR ="SBW_DELETE_ERROR";



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
        public static final String EIP_BIND_EIPV6_ERROR ="这个eip已经绑定eipv6";

        //Return messages
        public static final String EIP_CREATION_SUCCEEDED="弹性公网IP创建成功";
        public static final String EIP_DELETE_SUCCEEDED="弹性公网IP删除成功";
        public static final String EIP_UPDATE_SUCCEEDED="弹性公网IP更新成功";
        public static final String EIP_RENEWAL_SUCCEEDED="弹性公网IP续费成功";
        public static final String KEYCLOAK_NO_PROJECT="没有项目信息";

        public static final String SLB_BIND_NOT_FOND ="Bad request: 根据此id无法找到对应的SLB信息";

        //sbw
        public static final String ELASTIC_IP_IN_SBW_CANNOT_BE_REMOVED = "共享带宽中存在弹性IP，无法删除";
        public static final String SBW_FORBIDDEN = "无权操作，请重新确认";
        public static final String SBW_BILLTYPE_NOT_HOURLYSETTLEMENT = "按需付费的共享带宽不支持操作";
        public static final String SBW_NOT_FOND_BY_ID = "404-Bad request: 根据此id无法找到对应的SBW信息";
        public static final String EIP_SHARED_BAND_WIDTH_ID_NOT_NULL="eip 的共享带宽ID不能为空!";
        public static final String SBW_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR="sbw更改带宽预付增加错误!";
        public static final String SBW_DELETE_NOT_FIND_QOS ="未找到该条sbw的qos信息";
        public static final String SBW_THE_NEW_BANDWIDTH_VALUE_ERROR="修改的包年包月的共享带宽值不能小于旧值";
        public static final String SBW_FORBIDEN_WITH_ID="通过id未找到改共享带宽信息";
        public static final String SBW_CHANGE_BANDWIDTH_ERROR="共享带宽带宽值调整失败";
        public static final String SBW_DELETE_ERROR ="共享带宽删除错误";

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
        public static final String EIP_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR="the bandWidth must bigger than orgin when choose prepaid modle";
        public static final String EIP_FORBIDDEN ="Forbidden to operate.";
        public static final String EIP_FORBIDEN_WITH_ID ="Forbidden to operate,id:{}.";
        public static final String EIP_BIND_EIPV6_ERROR ="EIP is already bound to eipv6";
        //Return messages
        public static final String EIP_CREATION_SUCCEEDED="Eip creation succeeded";
        public static final String EIP_DELETE_SUCCEEDED="Eip deletion succeeded";
        public static final String EIP_UPDATE_SUCCEEDED="Eip updated successfully";
        public static final String EIP_RENEWAL_SUCCEEDED="Eip renew success";
        public static final String KEYCLOAK_NO_PROJECT="keycloak has no project info.";

        //sbw
        public static final String EIP_SHARED_BAND_WIDTH_ID_NOT_NULL="Eip had already added other SBW !";
        public static final String SBW_FORBIDDEN="Sbw_forbidden，You have no right to operate!";
        public static final String SBW_BILLTYPE_NOT_HOURLYSETTLEMENT = "sbw_billtype_not_hourlysettlement";
        public static final String SBW_NOT_FOND_BY_ID = "404-Bad request: sbw_not_fond_by_id";
        public static final String SBW_DELETE_NOT_FIND_QOS ="sbw_delete_not_find_qos";
        public static final String SBW_THE_NEW_BANDWIDTH_VALUE_ERROR="sbw the new bandWidth value cannot be smaller than the old one";
        public static final String ELASTIC_IP_IN_SBW_CANNOT_BE_REMOVED = "Elastic IP in Shared bandwidth cannot be deleted";
        public static final String SBW_CHANGE_BANDWIDHT_PREPAID_INCREASE_ERROR="SBW change bandwidth prepaid increase error!";
        public static final String SBW_FORBIDEN_WITH_ID="Shared bandwidth information not found by id";
        public static final String SBW_CHANGE_BANDWIDTH_ERROR="Shared bandwidth bandwidth value adjustment failed";
        public static final String SBW_DELETE_ERROR ="Shared bandwidth deletion error";

    }






}
