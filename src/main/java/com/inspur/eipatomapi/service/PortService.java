package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.NovaServerEntity;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.HttpResponse;
import com.inspur.eipatomapi.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.net.URL;
import java.util.ArrayList;
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
    public List<NovaServerEntity> listServerByTags(String tag, OSClient.OSClientV3 osClientV3) {
        Map<String, String> paramMap = new HashMap<>(4);
        if (tag != null) {
            paramMap.put("tags", tag);
        }
        //The tenant can only himself servers
        paramMap.put("all_tenants", "false");

        Map<String, Object> novaUrlAndHttpEntity = getPublicUrlAndHttpEntity("compute", "nova",osClientV3);
        //add micro version header to use tags
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(((HttpEntity) novaUrlAndHttpEntity.get("httpEntity")).getHeaders());
        //添加可以查询tags的微版本号
        headers.add("X-Openstack-Nova-Api-Version", "2.46");
        //get url
        String novaPublicUrl = novaUrlAndHttpEntity.get("url") + "/servers/detail";
        String msg;
        List<NovaServerEntity> serverList;
        try {
            msg = String.format("invoke url : %s", novaPublicUrl);
            log.info(msg);
            HttpResponse httpResponse = HttpUtil.doGetWithHeaders(novaPublicUrl, paramMap, headers);
            if (httpResponse.getStatusCode() == HttpStatus.OK.value()) {
                serverList = JSONObject.parseObject(httpResponse.getResponseBody()).getJSONArray("servers").toJavaList(NovaServerEntity.class);
                return serverList;
            }
        } catch (Exception e) {
            msg = String.format("Invoke url error : %s", novaPublicUrl);
        }
        log.error("Error when get server by tag", msg);
        return new ArrayList<>();
    }
    /**
     * 获取原生openstack查询servers的url的方法
     */

    private Map<String, Object> getPublicUrlAndHttpEntity(String type, String name,OSClient.OSClientV3 osClientV3)  {
        Map<String, Object> map = new HashMap<>(4);
        try {
            String token = osClientV3.getToken().getId();
            String url = getPublicUrl(osClientV3, type, name);
            map.put("url", url);
            map.put("httpEntity", getHttpEntity(token));
        }catch (Exception e){
            log.error("get public url error.", e);
        }
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
