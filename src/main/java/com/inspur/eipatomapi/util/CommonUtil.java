package com.inspur.eipatomapi.util;

import com.inspur.icp.common.util.Base64Util;
import com.inspur.icp.common.util.HttpClientUtil;
import com.inspur.icp.common.util.OSClientUtil;
import com.inspur.icp.common.util.QueryUtil;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtil {

    private final static Log log = LogFactory.getLog(CommonUtil.class);

    public static String getDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(currentTime);
    }

    @Setter
    private static JSONObject KeyClockInfo;



    private static String authUrl = "https://10.110.25.117:5000/v3"; //endpoint Url
    private static String user = "vpc";
    private static String password = "123456a?";
    private static String projectId = "65a859f362f749ce95237cbd08c30edf";
    private static String userDomainId = "default";
    private static Config config = Config.newConfig().withSSLVerificationDisabled();

    private static OSClientV3 getOsClientV3(){
        //String token = getKeycloackToken();
        return OSFactory.builderV3()
                .endpoint(authUrl)
                .credentials(user, password, Identifier.byId(userDomainId))
                .withConfig(config)
                .scopeToProject(Identifier.byId(projectId))
                .authenticate();
    }


    public static OSClientV3 getOsClientV3Util() throws Exception {

        //cancle the auth
        if(1==1){
            return getOsClientV3();
        }


        String token = getKeycloackToken();
        log.info(token);
        org.json.JSONObject jsonObject = Base64Util.decodeUserInfo(token);
        setKeyClockInfo(jsonObject);
        log.info("decode::"+jsonObject);
//        {"sub":"bd7ee578-3d26-4efc-97ed-c576325cf95a",
//                "resource_access":{
//                "vm-atomapi":{"roles":["uma_protection","vm_atomapi"]},
//                "account":{"roles":["manage-account","manage-account-links","view-profile"]}
//                },
//            "allowed-origins":[],
//            "iss":"https://10.110.22.13/auth/realms/picp",
//            "project":"jindengke",
//            "typ":"Bearer",
//            "preferred_username":"jindengke",
//            "aud":"vm-atomapi",
//            "acr":"1",
//            "nbf":0,
//            "realm_access":{"roles":["uma_authorization","vm_atomapi","user"]},
//            "azp":"vm-atomapi",
//            "auth_time":0,
//            "exp":1536895861,
//            "session_state":"3cdcbe28-9ca0-44af-86f6-b960a07ff78f",
//            "iat":1536894661,
//            "jti":"ef531699-48e1-4075-9a89-9d4a4db10935",
//            "email":"jindengke@inspur.com"
//        }

        String project = (String) jsonObject.get("project");
        log.info(project);

        //String regionInfo=getReginInfo();
        //log.warning("regionInfo"+regionInfo);
        //accord the param region get the first param ip
        return OSClientUtil.getOSClientV3("10.110.25.117",token,project);
    }

    public static JSONObject getTokenInfo(){

        return KeyClockInfo;

    }

    /**
     * get the Keycloak authorization token  from httpHeader;
     * @return
     */
    public static String getKeycloackToken() throws Exception {
        //important
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String keyCloackToken  = (String) request.getHeader("authorization");
        log.info(keyCloackToken); ;
        if(keyCloackToken==null){
            throw new Exception("ERROR:request authorization info is null,");
        }else{
            return keyCloackToken;
        }
    }

    /**
     * get the region info from httpHeader;
     * @return
     * @throws Exception
     */
    public static String getReginInfo() throws Exception {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String regionName = QueryUtil.getEndpoint(request);
        if(regionName==null){
            log.info("====regionName:"+regionName);
            String ipAndPortStr = HttpClientUtil.doGet("http://evs.cn-north-1.inspur.com:8081/platform?regionName=" + regionName);
            return ipAndPortStr;
        }else{
            throw new Exception("ERROR:request region info is null,");
        }
    }



}
