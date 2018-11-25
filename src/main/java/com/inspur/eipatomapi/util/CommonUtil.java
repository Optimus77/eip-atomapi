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
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CommonUtil {

    public final static Logger log = LoggerFactory.getLogger(CommonUtil.class);
    public static boolean isDebug = false;
    public static boolean qosDebug = true;


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
    private static String debugRegion="RegionOne";
    private static String region1="cn-north-3a";


    public static Map<String,String> userConfig = new HashMap<>(16);

    static {
        try {
            Properties properties = PropertiesLoaderUtils.loadAllProperties("application.yml");
            for(Object key:properties.keySet()){
                userConfig.put(key.toString(),properties.get(key).toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static OSClientV3 getOsClientV3(){
        //String token = getKeycloackToken();
        return OSFactory.builderV3()
                .endpoint(authUrl)
                .credentials(user, password, Identifier.byId(userDomainId))
                .withConfig(config)
                .scopeToProject(Identifier.byId(projectId))
                .authenticate().useRegion(debugRegion);
    }

    public static OSClientV3 getOsClientV3Util(String userRegion) throws KeycloakTokenException {

        String token = getKeycloackToken();
        log.info(token);
        if(null == token){
            log.error("can't get token, use default project admin 140785795de64945b02363661eb9e769");
            return getOsClientV3();
        }

        if(isDebug){
            userRegion = debugRegion;
        }
        if(token.startsWith("Bearer Bearer")){
            token = token.substring(7);
        }
        org.json.JSONObject jsonObject = Base64Util.decodeUserInfo(token);
        setKeyClockInfo(jsonObject);
        log.info("decode::"+jsonObject);
        if(jsonObject.has("project")){
            String project = (String) jsonObject.get("project");
            log.info("Get project from token:{}", project);
            return OSClientUtil.getOSClientV3(userConfig.get("openstackIp"),token,project,userRegion);
        }else {
            throw new KeycloakTokenException(CodeInfo.getCodeMessage(CodeInfo.KEYCLOAK_NO_PROJECT));
        }

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


    public static String getProjectId(String userRegion) throws KeycloakTokenException {

        String token = getKeycloackToken();
        if(null == token){
            log.info("can't get token, use default project admin 140785795de64945b02363661eb9e769");
            return projectId;
        }else{
            try{
                OSClientV3 os= getOsClientV3Util(userRegion);
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
