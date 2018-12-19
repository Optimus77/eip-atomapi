package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.HttpUtil;
import com.inspur.eipatomapi.util.KeycloakTokenException;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.inspur.eipatomapi.util.CommonUtil.getOsClientV3Util;

@Slf4j
@org.springframework.stereotype.Service
public class PortService {

    /**
     * invoke openstack interface
     */

    public List<Server> listServerByTags(String tag, String region) throws KeycloakTokenException {
        Map<String, String> paramMap = new HashMap<>(4);
        if (tag != null) {
            paramMap.put("tags", tag);
        }
        //The tenant can only himself servers
        paramMap.put("all_tenants", "false");

        Map<String, Object> novaUrlAndHttpEntity = getPublicUrlAndHttpEntity("compute","nova",region);
        //add micro version header to use tags
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(((HttpEntity) novaUrlAndHttpEntity.get("httpEntity")).getHeaders());
        //添加可以查询tags的微版本号
        headers.add("X-Openstack-Nova-Api-Version", "2.46");
        //get url
        String novaPublicUrl = novaUrlAndHttpEntity.get("url") + "/servers/detail";

        try {
            String msg = String.format("invoke url : %s", novaPublicUrl);
                String  httpResponse = HttpUtil.get(novaPublicUrl, paramMap);
            if (httpResponse!=null) {
                JSONObject jsonObject = JSONObject.parseObject(httpResponse);
                List<Server> list = jsonObject.getJSONArray("servers").toJavaList(Server.class);
                return list;
            }
        } catch (Exception e) {
            String msg = String.format("Invoke url error : %s", novaPublicUrl);
            return null;
        }
        return null;
    }

    /**
     * 获取原生openstack查询servers的url的方法
     */

    private Map<String, Object> getPublicUrlAndHttpEntity(String type, String name,String region) throws KeycloakTokenException {
        OSClient.OSClientV3 osClientV3 = getOsClientV3Util(region);
        String token = osClientV3.getToken().getId();
        String url = getPublicUrl(osClientV3, type, name);
        Map<String, Object> map = new HashMap<>(4);
        map.put("url", url);
        map.put("httpEntity", getHttpEntity(token));
        return map;
    }

    private HttpEntity getHttpEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("X-Auth-Token", token);
        return new HttpEntity<>(null, headers);
    }

    private String getPublicUrl(OSClient.OSClientV3 osClientV3, String type, String name) {

        List<? extends Service> serviceCatalogs = osClientV3.getToken().getCatalog();
        if (null == serviceCatalogs || serviceCatalogs.isEmpty()) {
            return null;
        }
        for (Service service : serviceCatalogs) {
            if (type.equals(service.getType()) || name.equals(service.getName())) {
                List<? extends Endpoint> pointList = service.getEndpoints();
                for (Endpoint endpoint : pointList) {
                    // url类型:PUBLIC,ADMIN,INTERNAL
                    if ("PUBLIC".equals(endpoint.getIface().toString()) && CommonUtil.getRegionName().equals(endpoint.getRegion())) {
                        URL url = endpoint.getUrl();
                        return url.toString();
                    }
                }
            }
        }
        return null;
    }
}
