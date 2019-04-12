package com.inspur.eipatomapi.util;

import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.icp.common.util.Base64Util;
import com.inspur.icp.common.util.OSClientUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.inspur.eipatomapi.util.HsConstants.SCHEDULETIME;

@Slf4j
@Component
public class CommonUtil {

    private static boolean isDebug = true;
    public static boolean qosDebug = false;


    public static String getDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(currentTime);
    }

//    Greenwich mean time
    public static Date getGmtDate() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        return new Date();
    }

    /*
     *获取当天日期:yyyy-MM-dd
     */
    public static String getToday() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-ss");
        return formatter.format(currentTime);
    }


    @Setter
    private static JSONObject KeyClockInfo;
    @Value("${openstackIp}")
    private String openstackIp;
    @Value("${openstackUrl}")
    private String openstackUrl;
    @Value("${userNameS}")
    private String userNameS;
    @Value("${passwordS}")
    private String passwordS;
    @Value("${projectIdS}")
    private String projectIdS;
    @Value("${userDomainIdS}")
    private String userDomainIdS;
    @Value("${debugRegionS}")
    private String debugRegionS;

    @Value("${scheduleTime}")
    private String scheduleTime;

    private static Config config = Config.newConfig().withSSLVerificationDisabled();
    private static Map<String,String> userConfig = new HashMap<>(16);

    @PostConstruct
    public void init(){
        userConfig.put("openstackIp",openstackIp);
        userConfig.put("userNameS",userNameS);
        userConfig.put("passwordS",passwordS);
        userConfig.put("projectIdS",projectIdS);
        userConfig.put("userDomainIdS",userDomainIdS);
        userConfig.put("debugRegionS",debugRegionS);
        userConfig.put("openstackUrl",openstackUrl);
        userConfig.put(SCHEDULETIME, scheduleTime);
    }

//    private static OSClientV3 getOsClientV3(){
//        //String token = getKeycloackToken();
//        return OSFactory.builderV3()
//                .endpoint(openstackIp)
//                .credentials(userNameS, passwordS, Identifier.byId(userDomainIdS))
//                .withConfig(config)
//                .scopeToProject(Identifier.byId(projectIdS))
//                .authenticate().useRegion(debugRegionS);
//    }
    public static Map<String, String> getUserConfig(){
        return userConfig;
    }

    //administrator rights
    private static OSClientV3 getOsClientV3(){
        //String token = getKeycloackToken();
        return OSFactory.builderV3()
                .endpoint(userConfig.get("openstackUrl"))
                .credentials(userConfig.get("userNameS"), userConfig.get("passwordS"),
                        Identifier.byId(userConfig.get("userDomainIdS")))
                .withConfig(config)
                .scopeToProject(Identifier.byId(userConfig.get("projectIdS")))
                .authenticate().useRegion(userConfig.get("debugRegionS"));
    }


    public static OSClientV3 getOsClientV3Util(String userRegion) throws KeycloakTokenException {

        String token = getKeycloackToken();
        if(null == token){
            log.error("can't get token.");
            return getOsClientV3();
        }

        if(isDebug){
            userRegion = userConfig.get("debugRegionS");
            log.info("=============={}", userRegion);
        }
        if(token.startsWith("Bearer Bearer")){
            token = token.substring(7);
        }
        org.json.JSONObject jsonObject = Base64Util.decodeUserInfo(token);
        setKeyClockInfo(jsonObject);
        log.info("decode::"+jsonObject);
        if(jsonObject.has("project")){
            String project = (String) jsonObject.get("project");
            log.debug("Get openstack ip:{}, region:{}, project:{}.",userConfig.get("openstackIp"), userRegion, project);
            return OSClientUtil.getOSClientV3(userConfig.get("openstackIp"),token,project,userRegion);
        }else {
            String clientId = jsonObject.getString("clientId");
            if(null != clientId && clientId.equalsIgnoreCase("iaas-server")){
                log.info("Client token, User has right to operation, client:{}", clientId);
                return getOsClientV3();
            }else{
                log.error("User has no right to operation.{}", jsonObject.toString());
                throw new KeycloakTokenException(CodeInfo.getCodeMessage(CodeInfo.KEYCLOAK_NO_PROJECT));
            }
        }

    }

    public static JSONObject getTokenInfo(){

        return KeyClockInfo;

    }

    /**
     * get the Keycloak authorization token  from httpHeader;
     * @return  string string
     */
    public static String getKeycloackToken(){
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(null != requestAttributes) {
            HttpServletRequest request = requestAttributes.getRequest();
            String keyCloackToken = request.getHeader("authorization");

            if (keyCloackToken == null) {
                log.error("Failed to get token,request:{}",request);
            } else {
                log.debug("Get token:{}",keyCloackToken);
            }
            return keyCloackToken;
        }
        return null;
    }

    public static String getProjectId(String userRegion,  OSClientV3 os) throws KeycloakTokenException {

        String token = getKeycloackToken();
        if(null == token){
            log.info("can't get token, use default project admin 140785795de64945b02363661eb9e769");
            return userConfig.get("projectIdS");
        }else{
            try{
//                OSClientV3 os= getOsClientV3Util(userRegion);
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
                log.debug("getUserId:{}", sub);
                return sub;
            }else{
                throw new KeycloakTokenException(CodeInfo.getCodeMessage(CodeInfo.KEYCLOAK_TOKEN_EXPIRED));
            }
        }
    }
    public static String getRegionName() {

        return userConfig.get("debugRegionS");
    }
    public static String getProjectName()throws KeycloakTokenException {

        String token = getKeycloackToken();
        if(null == token){
            throw new KeycloakTokenException(CodeInfo.getCodeMessage(CodeInfo.KEYCLOAK_NULL));
        }
        org.json.JSONObject jsonObject = Base64Util.decodeUserInfo(token);
        String projectName = null;
        if (jsonObject.has("project")) {
            projectName = (String) jsonObject.get("project");
        } else if (jsonObject.has("preferred_username")) {
            projectName = (String) jsonObject.get("preferred_username");
        }
        if (projectName != null) {
            log.info("getProjectName:{}", projectName);
        }
        return projectName;
    }
    public static boolean isAuthoried(String projectId) {

        String token = getKeycloackToken();
        if(null == token){
            log.error("User has no token.");
            return false;
        }
        org.json.JSONObject jsonObject = Base64Util.decodeUserInfo(token);
        String userId = (String) jsonObject.get("sub");
        if(userId.equals(projectId)){
            return true;
        }
        String clientId = null;
        if(jsonObject.has("clientId")) {
            clientId = jsonObject.getString("clientId");
        }
        if(null != clientId && clientId.equalsIgnoreCase("iaas-server")){
            log.info("Client token, User has right to operation, client:{}", clientId);
            return true;
        }else{
            log.error("User has no right to operation.{}", jsonObject.toString());
            return false;
        }
    }


}
