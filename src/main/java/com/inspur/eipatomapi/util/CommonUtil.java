package com.inspur.eipatomapi.util;

import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.icp.common.util.Base64Util;
import com.inspur.icp.common.util.OSClientUtil;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.beans.factory.annotation.Value;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtil {

    public final static Logger log = LoggerFactory.getLogger(CommonUtil.class);
    public static boolean isDebug = true;


    public static String getDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(currentTime);
    }

    @Setter
    private static JSONObject KeyClockInfo;

    public static String openstackIp;

    public static String openstackPort;


    private static String authUrl = "https://10.110.25.117:5000/v3"; //endpoint Url
    private static String user = "admin";
    private static String password = "89rqdHLMN5rm0x1P";
    private static String projectId = "140785795de64945b02363661eb9e769";
    private static String userDomainId = "default";
    private static Config config = Config.newConfig().withSSLVerificationDisabled();
    private static String region="RegionOne";
    private static String region1="cn-north-3a";

//    @Value("${bssURL.openstackIp}")
//    public void setOpenstackIp(String openstackIp) {
//        this.openstackIp = openstackIp;
//    }

//    @Value("${bssURL.openstackPort}")
//    public void setOpenstackPort(String openstackPort) {
//        this.openstackPort = openstackPort;
//    }


    private static OSClientV3 getOsClientV3(){
        //String token = getKeycloackToken();
        return OSFactory.builderV3()
                .endpoint(authUrl)
                .credentials(user, password, Identifier.byId(userDomainId))
                .withConfig(config)
                .scopeToProject(Identifier.byId(projectId))
                .authenticate().useRegion(region);
    }


    public static OSClientV3 getOsClientV3Util()  {

        String token = getKeycloackToken();
        log.info(token);
        if(null == token){
            log.error("can't get token, use default project admin 140785795de64945b02363661eb9e769");
            token = "youmustgetatokenfirst";//Todo: debugcode, delte it when push
            return getOsClientV3();
        }
        if(token.startsWith("Bearer Bearer")){
            token = token.substring(7);
        }
        org.json.JSONObject jsonObject = Base64Util.decodeUserInfo(token);
        setKeyClockInfo(jsonObject);
        log.info("decode::"+jsonObject);
//        {
//    "sub":"bd7ee578-3d26-4efc-97ed-c576325cf95a",
//    "resource_access":{
//        "bss-server":{
//            "roles":[
//                "rolewtf",
//                "uma_protection",
//                "vm_atomapi"
//            ]
//        },
//        "vm-atomapi":{
//            "roles":[
//                "uma_protection",
//                "vm_atomapi"
//            ]
//        },
//        "account":{
//            "roles":[
//                "manage-account",
//                "manage-account-links",
//                "view-profile"
//            ]
//        }
//    },
//    "allowed-origins":[
//        "*"
//    ],
//    "iss":"https://10.110.22.13/auth/realms/picp",
//    "project":"jindengke2",
//    "typ":"Bearer",
//    "preferred_username":"jindengke",
//    "nonce":"077856fd-1c28-4017-84a3-5202253c78c3",
//    "aud":"bss-client",
//    "acr":"1",
//    "nbf":0,
//    "realm_access":{
//        "roles":[
//            "rolewtf",
//            "uma_authorization",
//            "vm_atomapi",
//            "user"
//        ]
//    },
//    "phone":"18865313588",
//    "azp":"bss-client",
//    "auth_time":1541557884,
//    "exp":1541593884,
//    "session_state":"60064e42-4671-45dc-96e6-c6e7e068c374",
//    "iat":1541557887,
//    "jti":"b525625d-d049-49a1-8664-996d504b76da",
//    "email":"jindengke@inspur.com"
//        }

        String project = (String) jsonObject.get("project");
        log.info("Get project from token:{}", project);

        //String regionInfo=getReginInfo();
        //log.warning("regionInfo"+regionInfo);
        //accord the param region get the first param ip
        return OSClientUtil.getOSClientV3("10.110.25.117",token,project,region);
    }

    public static JSONObject getTokenInfo(){

        return KeyClockInfo;

    }

    /**
     * get the Keycloak authorization token  from httpHeader;
     * @return  string string
     */
    public static String getKeycloackToken() {

        ServletRequestAttributes requestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        if(null != requestAttributes) {
            HttpServletRequest request = requestAttributes.getRequest();
            String keyCloackToken = request.getHeader("authorization");
            if (keyCloackToken == null) {
                log.error("Failed to get authorization header.");
                return null;
            } else {
                return keyCloackToken;
            }
        }
        return null;
    }

    /**
     * get the region info from httpHeader;
     * @return ret
     * @throws Exception e
     */
    //TODO region is not correct for now
    public static String getReginInfo() throws Exception {
        //ServletRequestAttributes requestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
//        if(null != requestAttributes) {
//            HttpServletRequest request = requestAttributes.getRequest();
//            String regionName = QueryUtil.getEndpoint(request);
//            log.info("regionName"+regionName);
//            if(regionName==null){
//                throw new Exception("get region fail");
//            }
//            return regionName;
//        }else{
//            throw new Exception("get region error");
//        }
        return region1;

    }

    public static String getProjectId() throws KeycloakTokenException {

        String token = getKeycloackToken();
        if(null == token){
            log.info("can't get token, use default project admin 140785795de64945b02363661eb9e769");
            return projectId;
        }else{
            try{
                OSClientV3 os= getOsClientV3Util();
                String projectid_client=os.getToken().getProject().getId();
                log.info("getProjectId:{}", projectid_client);
                return projectid_client;
            }catch (Exception e){
                log.error("get projectid from token error");
                throw new KeycloakTokenException(CodeInfo.getCodeMessage(CodeInfo.KEYCLOAK_TOKEN_EXPIRED));
            }
        }
    }

    public static String getUserId()throws KeycloakTokenException {

        String token = getKeycloackToken();
        if(null == token){
            throw new KeycloakTokenException(CodeInfo.getCodeMessage(CodeInfo.KEYCLOAK_NULL));
        }else{
            org.json.JSONObject jsonObject = Base64Util.decodeUserInfo(token);
            String sub = (String) jsonObject.get("sub");
            if(sub!=null){
                log.info("getUserId:{}", sub);
                return sub;
            }else{
                throw new KeycloakTokenException(CodeInfo.getCodeMessage(CodeInfo.KEYCLOAK_TOKEN_EXPIRED));
            }
        }
    }

    public static String getUsername()throws KeycloakTokenException {

        String token = getKeycloackToken();
        if(null == token){
            throw new KeycloakTokenException(CodeInfo.getCodeMessage(CodeInfo.KEYCLOAK_NULL));
        }else{
            org.json.JSONObject jsonObject = Base64Util.decodeUserInfo(token);
            String username = (String) jsonObject.get("preferred_username");
            if(username!=null){
                log.info("getUsername:{}", username);
                return username;
            }else{
                throw new KeycloakTokenException(CodeInfo.getCodeMessage(CodeInfo.KEYCLOAK_TOKEN_EXPIRED));
            }
        }
    }


}
