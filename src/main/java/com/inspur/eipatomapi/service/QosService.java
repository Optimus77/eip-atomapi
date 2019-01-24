package com.inspur.eipatomapi.service;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.inspur.eipatomapi.entity.Qos.*;
import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.util.HsConstants;
import com.inspur.eipatomapi.util.HsHttpClient;
import com.inspur.eipatomapi.util.IpUtil;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class QosService {
    private String fwIp;
    private String fwPort;
    private String fwUser;
    private String fwPwd;

    @Autowired
    private EipRepository eipRepository;

    public QosService() {
    }

    QosService(String fwIp, String fwPort, String fwUser, String fwPwd) {
        this.fwIp = fwIp;
        this.fwPort = fwPort;
        this.fwUser = fwUser;
        this.fwPwd = fwPwd;
    }

    HashMap<String, String> createQosPipe(HashMap<String, String> info) {
        HashMap res = new HashMap();

        try {
            String retr = HsHttpClient.hsHttpPost(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, "/rest/iQos?target=root", this.getCreatePipeJson(info));
            JSONObject jo = new JSONObject(retr);
            boolean success = jo.getBoolean(HsConstants.SUCCESS);
            res.put(HsConstants.SUCCESS, success);
            if (success) {
                Map<String, String> map = this.getQosPipeId(info.get(HsConstants.PIPE_NAME));
                if ((map.get(HsConstants.SUCCESS)).equals("true")) {
                    res.put("id",map.get("id"));
                } else {
                    res.put("msg", "Create success,but id not found,please call find api by pip name.");
                }
            } else {
                log.info("add qos failed, result:{}", jo);
                res.put("msg", jo.getJSONObject(HsConstants.EXCEPTION));
            }

            return res;
        } catch (Exception var7) {
            log.error(var7.getMessage());
            res.put(HsConstants.SUCCESS, HsConstants.FALSE);
            res.put("msg", var7.getMessage());
            return res;
        }
    }

    HashMap<String, String> delQosPipe(String pipeId) {
        HashMap<String, String> res = new HashMap();
        String json = "[{\"target\":\"root\",\"node\":{\"name\":\"first\",\"root\":{\"id\":\"" + pipeId + "\"}}}]";

        try {
            String retr = HsHttpClient.hsHttpDelete(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, "/rest/iQos", json);
            JSONObject jo = new JSONObject(retr);
            boolean success = jo.getBoolean(HsConstants.SUCCESS);
            if (success) {
                res.put(HsConstants.SUCCESS, "true");
            } else if ("Error: The root pipe dose not exist".equals(jo.getJSONObject(HsConstants.EXCEPTION).getString("message"))) {
                res.put(HsConstants.SUCCESS, "true");
                res.put("msg", "pip not found.");
            } else {
                res.put(HsConstants.SUCCESS, HsConstants.FALSE);
                res.put("msg", jo.getString(HsConstants.EXCEPTION));
            }
            return res;
        } catch (Exception var7) {
            log.error(var7.getMessage());
            res.put(HsConstants.SUCCESS, HsConstants.FALSE);
            res.put("msg", var7.getMessage());
            return res;
        }
    }

    HashMap<String, String> updateQosPipe(String pipeId, String pipeName, String bandWidth) {
        HashMap res = new HashMap();
        try {
            String body = this.getUpdateJson(pipeId, pipeName, bandWidth);
            String retr = HsHttpClient.hsHttpPut(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, "/rest/iQos?target=root", body);
            JSONObject jo = new JSONObject(retr);
            log.info("Update: pipeId:{}, pipeName:{}, bandwidht:{}", pipeId, pipeName, bandWidth);
            log.info("updateQosPipe result {}",jo);
            boolean success=jo.getBoolean(HsConstants.SUCCESS);
            res.put(HsConstants.SUCCESS, success);
            if (jo.getBoolean(HsConstants.SUCCESS)) {
                res.put("msg", jo.get(HsConstants.EXCEPTION));
            }
            return res;
        } catch (Exception var8) {
            log.error(var8.getMessage());
            res.put(HsConstants.SUCCESS, HsConstants.FALSE);
            res.put("msg", var8.getMessage());
            return res;
        }
    }


    private String getCreatePipeJson(HashMap<String, String> map)  {
        try {
            String s;
            if(map.containsKey("ip")) {
                s = "{\"name\": \"first\",\"root\": {\"name\":\"" +  map.get("pipeName") + "\",\"desc\": \"\",\"qos_mode\": {\"name\": \"shape\"},\"rule\": [{ \"id\": [],\"src_addr\": [{\"name\": \"any\"}]," + "\"src_host\": [],\"src_subnet\": [],\"src_range\": [],\"dst_addr\": [],\"dst_host\": [],\"dst_subnet\": [{\"ip\":" + IpUtil.ipToLong( map.get("ip")) + ",\"netmask\":32" + "}],\"dst_range\": [],\"user\": [],\"usergroup\": [],\"service\": [{\"name\": \"" +  map.get("serviceNamne") + "\"}],\"application\": [],\"src_zone\": [],\"ingress_if\": [],\"dst_zone\": [],\"egress_if\": []" + ",\"vlan\": [],\"tos\": []}],\"action\": [{\"dir\": \"1\",\"min\": \"" + map.get(HsConstants.BAND_WIDTH) + "\", \"max\":\"" + map.get(HsConstants.BAND_WIDTH) + "\",\"per_min\": \"\",\"per_max\": \"\",\"per_using\": \"\",\"priority\": 7,\"set_tos\": \"2\",\"tos\": \"\",\"amask\": {" + "\"action_dir\": true,\"action_bandwidth\": false,\"action_reserve_bandwidth\": false,\"action_min\": false,\"action_max\": false," + "\"action_per_ip_min\": false,\"action_per_ip_max\": false,\"action_per_user_min\": false,\"action_per_user_max\": false," + "\"action_per_ip_using\": false,\"action_average_using\": false,\"action_tos_mark\": false,\"action_tos_int\": true," + "\"action_tos_str\": false,\"action_priority\": true,\"action_bandwidth_mbps\": true,\"action_reserve_bandwidth_mbps\": false," + "\"action_min_mbps\": false,\"action_max_mbps\": false,\"action_per_ip_min_mbps\": false,\"action_per_ip_max_mbps\": false," + "\"action_per_user_min_mbps\": false,\"action_per_user_max_mbps\": false,\"action_reserve_bandwidth_percent\": false,\"action_min_percent\": false," + "\"action_max_percent\": false,\"action_bandwidth_gbps\": false,\"action_rserve_bandwidth_gbps\": false,\"action_min_gbps\": false," + "\"action_max_gbps\": false,\"action_mode\": false}},{\"dir\": \"2\",\"min\": \"" +map.get(HsConstants.BAND_WIDTH) + "\", \"max\":\"" +map.get(HsConstants.BAND_WIDTH) + "\",\"per_min\": \"\",\"per_max\": \"\",\"per_using\": \"\",\"priority\": 7,\"set_tos\": \"2\",\"tos\": \"\",\"amask\": {" + "\"action_dir\": true,\"action_bandwidth\": false,\"action_reserve_bandwidth\": false,\"action_min\": false,\"action_max\": false," + "\"action_per_ip_min\": false,\"action_per_ip_max\": false,\"action_per_user_min\": false,\"action_per_user_max\": false,\"action_per_ip_using\": false," + "\"action_average_using\": false,\"action_tos_mark\": false,\"action_tos_int\": true,\"action_tos_str\": false,\"action_priority\": true," + "\"action_bandwidth_mbps\": true,\"action_reserve_bandwidth_mbps\": false,\"action_min_mbps\": false,\"action_max_mbps\": false,\"action_per_ip_min_mbps\": false," + "\"action_per_ip_max_mbps\": false,\"action_per_user_min_mbps\": false,\"action_per_user_max_mbps\": false,\"action_reserve_bandwidth_percent\": false," + "\"action_min_percent\": false,\"action_max_percent\": false,\"action_bandwidth_gbps\": false,\"action_rserve_bandwidth_gbps\": false," + "\"action_min_gbps\": false,\"action_max_gbps\": false,\"action_mode\": false}}],\"id\": 0}}";
            }else{
                s = "{\"name\": \"first\",\"root\":{\"name\":\"" + map.get("pipeName")  + "\",\"desc\":\"\",\"qos_mode\":{\"name\":\"shape\"},\"action\":[{\"dir\":\"1\",\"min\":\"" + map.get(HsConstants.BAND_WIDTH) + "\",\"max\":\"" + map.get(HsConstants.BAND_WIDTH) + "\",\"per_min\":\"\",\"per_max\":\"\",\"per_using\":\"\",\"priority\":7,\"set_tos\":\"2\",\"tos\":\"\",\"amask\":{\"action_dir\":true,\"action_bandwidth\":false,\"action_reserve_bandwidth\":false,\"action_min\":false,\"action_max\":false,\"action_per_ip_min\":false,\"action_per_ip_max\":false,\"action_per_user_min\":false,\"action_per_user_max\":false,\"action_per_ip_using\":false,\"action_average_using\":false,\"action_tos_mark\":false,\"action_tos_int\":true,\"action_tos_str\":false,\"action_priority\":true,\"action_bandwidth_mbps\":true,\"action_reserve_bandwidth_mbps\":false,\"action_min_mbps\":false,\"action_max_mbps\":false,\"action_per_ip_min_mbps\":false,\"action_per_ip_max_mbps\":false,\"action_per_user_min_mbps\":false,\"action_per_user_max_mbps\":false,\"action_reserve_bandwidth_percent\":false,\"action_min_percent\":false,\"action_max_percent\":false,\"action_bandwidth_gbps\":false,\"action_rserve_bandwidth_gbps\":false,\"action_min_gbps\":false,\"action_max_gbps\":false,\"action_mode\":false}},{\"dir\":\"2\",\"min\":\"" + map.get(HsConstants.BAND_WIDTH) + "\",\"max\":\"" + map.get(HsConstants.BAND_WIDTH) + "\",\"per_min\":\"\",\"per_max\":\"\",\"per_using\":\"\",\"priority\":7,\"set_tos\":\"2\",\"tos\":\"\",\"amask\":{\"action_dir\":true,\"action_bandwidth\":false,\"action_reserve_bandwidth\":false,\"action_min\":false,\"action_max\":false,\"action_per_ip_min\":false,\"action_per_ip_max\":false,\"action_per_user_min\":false,\"action_per_user_max\":false,\"action_per_ip_using\":false,\"action_average_using\":false,\"action_tos_mark\":false,\"action_tos_int\":true,\"action_tos_str\":false,\"action_priority\":true,\"action_bandwidth_mbps\":true,\"action_reserve_bandwidth_mbps\":false,\"action_min_mbps\":false,\"action_max_mbps\":false,\"action_per_ip_min_mbps\":false,\"action_per_ip_max_mbps\":false,\"action_per_user_min_mbps\":false,\"action_per_user_max_mbps\":false,\"action_reserve_bandwidth_percent\":false,\"action_min_percent\":false,\"action_max_percent\":false,\"action_bandwidth_gbps\":false,\"action_rserve_bandwidth_gbps\":false,\"action_min_gbps\":false,\"action_max_gbps\":false,\"action_mode\":false}}],\"id\":0}}";
            }

            return s;
        } catch (Exception var4) {
            log.error("get create pip error.");
            throw var4;
        }
    }

    private String getUpdateJson(String pipeId, String pipeName, String bandWidth) {
        //return "{ \"name\": \"first\",\"root\": {\"id\": \"" + pipeId + "\",\"name\": \"" + pipeName + "\", \"desc\": \"\", \"qos_mode\": { \"name\": \"shape\"}, \"action\": [{\"dir\": \"1\",\"min\": \"" + bandWidth + "\", \"max\": \"" + bandWidth + "\", \"per_min\": \"\", \"per_max\": \"\", \"per_using\": \"\", \"priority\": 7, \"set_tos\": \"2\"," + "\"tos\": \"\", \"amask\": {\"action_dir\": true,\"action_bandwidth\": false,\"action_reserve_bandwidth\": false," + "\"action_min\": false,\"action_max\": false,\"action_per_ip_min\": false,\"action_per_ip_max\": false, \"action_per_user_min\": false," + "\"action_per_user_max\": false, \"action_per_ip_using\": false, \"action_average_using\": false, \"action_tos_mark\": false," + "\"action_tos_int\": true,\"action_tos_str\": false, \"action_priority\": true, \"action_bandwidth_mbps\": true," + "\"action_reserve_bandwidth_mbps\": false,\"action_min_mbps\": false,\"action_max_mbps\": false,\"action_per_ip_min_mbps\": false," + "\"action_per_ip_max_mbps\": false,\"action_per_user_min_mbps\": false,\"action_per_user_max_mbps\": false, \"action_reserve_bandwidth_percent\": false," + "\"action_min_percent\": false,\"action_max_percent\": false,\"action_bandwidth_gbps\": false, \"action_rserve_bandwidth_gbps\": false," + "\"action_min_gbps\": false,\"action_max_gbps\": false,\"action_mode\": false}},{\"dir\": \"2\",\"min\": \"" + bandWidth + "\",\"max\":\"" + bandWidth + "\",\"per_min\": \"\",\"per_max\": \"\",\"per_using\": \"\", \"priority\": 7,\"set_tos\": \"2\", \"tos\": \"\"," + "\"amask\": {\"action_dir\": true, \"action_bandwidth\": false,\"action_reserve_bandwidth\": false,\"action_min\": false," + "\"action_max\": false, \"action_per_ip_min\": false, \"action_per_ip_max\": false,\"action_per_user_min\": false," + "\"action_per_user_max\": false, \"action_per_ip_using\": false, \"action_average_using\": false,\"action_tos_mark\": false," + "\"action_tos_int\": true, \"action_tos_str\": false, \"action_priority\": true, \"action_bandwidth_mbps\": true," + "\"action_reserve_bandwidth_mbps\": false, \"action_min_mbps\": false,\"action_max_mbps\": false,\"action_per_ip_min_mbps\": false," + "\"action_per_ip_max_mbps\": false,\"action_per_user_min_mbps\": false,\"action_per_user_max_mbps\": false,\"action_reserve_bandwidth_percent\": false," + "\"action_min_percent\": false,\"action_max_percent\": false, \"action_bandwidth_gbps\": false, \"action_rserve_bandwidth_gbps\": false," + "\"action_min_gbps\": false, \"action_max_gbps\": false,\"action_mode\": false}}],\"schedule\": [ ]}}";
        String errorstr =  "{\"name\":\"first\",\"root\":{\"id\":\""+pipeId+"\",\"name\":\""+pipeName+"\",\"desc\":\"\",\"qos_mode\":{\"name\":\"shape\"},\"action\":[{\"dir\":\"1\",\"min\":\""+bandWidth+"\",\"max\":\""+bandWidth+"\",\"per_min\":\"\",\"per_max\":\"\",\"per_using\":\"\",\"priority\":7,\"set_tos\":\"2\",\"tos\":\"\",\"amask\":{\"action_dir\":true,\"action_bandwidth\":false,\"action_reserve_bandwidth\":false,\"action_min\":false,\"action_max\":false,\"action_per_ip_min\":false,\"action_per_ip_max\":false,\"action_per_user_min\":false,\"action_per_user_max\":false,\"action_per_ip_using\":false,\"action_average_using\":false,\"action_tos_mark\":false,\"action_tos_int\":true,\"action_tos_str\":false,\"action_priority\":true,\"action_bandwidth_mbps\":true,\"action_reserve_bandwidth_mbps\":false,\"action_min_mbps\":false,\"action_max_mbps\":false,\"action_per_ip_min_mbps\":false,\"action_per_ip_max_mbps\":false,\"action_per_user_min_mbps\":false,\"action_per_user_max_mbps\":false,\"action_reserve_bandwidth_percent\":false,\"action_min_percent\":false,\"action_max_percent\":false,\"action_bandwidth_gbps\":false,\"action_rserve_bandwidth_gbps\":false,\"action_min_gbps\":false,\"action_max_gbps\":false,\"action_mode\":false}},{\"dir\":\"2\",\"min\":\""+bandWidth+"\",\"max\":\""+bandWidth+"\",\"per_min\":\"\",\"per_max\":\"\",\"per_using\":\"\",\"priority\":7,\"set_tos\":\"2\",\"tos\":\"\",\"amask\":{\"action_dir\":true,\"action_bandwidth\":false,\"action_reserve_bandwidth\":false,\"action_min\":false,\"action_max\":false,\"action_per_ip_min\":false,\"action_per_ip_max\":false,\"action_per_user_min\":false,\"action_per_user_max\":false,\"action_per_ip_using\":false,\"action_average_using\":false,\"action_tos_mark\":false,\"action_tos_int\":true,\"action_tos_str\":false,\"action_priority\":true,\"action_bandwidth_mbps\":true,\"action_reserve_bandwidth_mbps\":false,\"action_min_mbps\":false,\"action_max_mbps\":false,\"action_per_ip_min_mbps\":false,\"action_per_ip_max_mbps\":false,\"action_per_user_min_mbps\":false,\"action_per_user_max_mbps\":false,\"action_reserve_bandwidth_percent\":false,\"action_min_percent\":false,\"action_max_percent\":false,\"action_bandwidth_gbps\":false,\"action_rserve_bandwidth_gbps\":false,\"action_min_gbps\":false,\"action_max_gbps\":false,\"action_mode\":false}}],\"schedule\":[]}}";
        log.debug(errorstr);
        return errorstr;
    }

    HashMap<String, String> getQosPipeId(String pipeName) {
        HashMap res = new HashMap();

        try {
            String params = "/rest/iQos?query=%7B%22conditions%22%3A%5B%7B%22f%22%3A%22name%22%2C%22v%22%3A%22first%22%7D%5D%7D&target=root&node=root&id=%7B%22node%22%3A%22root%22%7D";
            String retr = HsHttpClient.hsHttpGet(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, params);
            JSONObject jo = new JSONObject(retr);
            JSONArray array = jo.getJSONArray("children");
            int l = array.length();
            String id = "";

            for (int i = 0; i < l; ++i) {
                JSONObject job = array.getJSONObject(i);
                if (pipeName.equals(job.optString("name"))) {
                    id = job.optString("id");
                    break;
                }
            }

            res.put(HsConstants.SUCCESS, "true");
            res.put("id", id);
            return res;
        } catch (Exception var11) {
            log.error(var11.getMessage());
            res.put(HsConstants.SUCCESS, HsConstants.FALSE);
            res.put("msg", var11.getMessage());
            return res;
        }
    }

    /**
     * add Qos Pipe bind eip
     *
     * @param fip fip
     * @param sbwId id
     * @return ret
     */
    HashMap<String, String> addIpToQos(String fip, String pipId, String sbwId) {
        HashMap<String, String> res = new HashMap();
        String IP32 = IpUtil.ipToLong(fip);
        IpRange newIp = new IpRange(IP32, IP32);
        UpdateCondition condition = new UpdateCondition();
        condition.setName("first");
        RootConfig root = new RootConfig();
        root.setId(pipId);
        RuleConfig config = new RuleConfig();
        Set<IpRange> ipSet = new HashSet<>();
        ArrayList<RuleConfig> list = new ArrayList<>();
        ipSet.add(newIp);
        try {
            //query qos pipe details by pipeId
            List<Eip> eipList = getQueryQosByDataBase(sbwId);
            if (eipList != null && eipList.size() > 0) {
                for (int i = 0; i < eipList.size(); i++) {
                    Eip eip = eipList.get(i);
                    String floatingIp = eip.getFloatingIp();
                    if(floatingIp.equalsIgnoreCase(fip) && eip.getStatus().equalsIgnoreCase(HsConstants.ACTIVE)){
                        res.put("msg", "ip exist");
                        res.put(HsConstants.SUCCESS, HsConstants.FALSE);
                        res.put("id", pipId);
                        return res;
                    }
                    String longFloatIp = IpUtil.ipToLong(floatingIp);
                    IpRange range = new IpRange(longFloatIp, longFloatIp);
                    if (!ipSet.contains(range)) {
                        ipSet.add(range);
                    }
                }
            }
            //src addr
            ArrayList addrList = new ArrayList();
            addrList.add(new SrcAddr());
            config.setSrcAddr(addrList);
            //dst range
            config.setDstRange(ipSet);
            list.add(config);
            root.setRule(list);
            condition.setRoot(root);
            //register the customer adapter
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(String.class, STRING);
            String conditionStr = gsonBuilder.serializeNulls().create().toJson(condition);
            log.info(conditionStr);
            //add the ip to ip Array
            String retr ="";
            if (ipSet.size() != 1) {
                retr = HsHttpClient.hsHttpPut(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, "/rest/iQos?target=root.rule", conditionStr);
            } else if (ipSet.size() == 1 ){
                retr = HsHttpClient.hsHttpPost(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, "/rest/iQos?target=root.rule", conditionStr);
            }
//            String retr = HsHttpClient.hsHttpPut("10.110.29.206", "443", "hillstone", "hillstone", "/rest/iQos?target=root.rule", conditionStr);
            JSONObject jo = new JSONObject(retr);
            log.info("addQosPipeBindEip result:{}", jo);
            boolean success = jo.getBoolean(HsConstants.SUCCESS);
            if (Boolean.valueOf(success)) {
                res.put(HsConstants.RESULT, "true");
                res.put("id", pipId);
            } else {
                res.put(HsConstants.RESULT, HsConstants.FALSE);
            }
        } catch (Exception var8) {
            log.error(var8.getMessage());
            res.put(HsConstants.SUCCESS, HsConstants.FALSE);
            res.put("msg", var8.getMessage());
            return res;
        }
        return res;
    }

    /**
     * remove
     *
     * @param floatIp
     * @param pipeId
     * @return
     */
    HashMap<String, String> removeIpFromQos(String floatIp, String pipeId, String sbwId) {
        HashMap<String, String> res = new HashMap();
        UpdateCondition condition = new UpdateCondition();
        condition.setName("first");
        RootConfig root = new RootConfig();
        root.setId(pipeId);
        RuleConfig config = new RuleConfig();
        Set<IpRange> ipSet = new HashSet<>();
        ArrayList<RuleConfig> ruleList = new ArrayList<>();
        try {
            //query qos pipe details by pipeId
            List<Eip> eipList = getQueryQosByDataBase(sbwId);
            if (eipList == null || eipList.isEmpty()) {
                res.put("msg", "qos not exist");
                res.put(HsConstants.SUCCESS, HsConstants.FALSE);
                return res;
            }

            for (Eip eip: eipList) {
                String floatingIp = eip.getFloatingIp();
                if (StringUtils.isNotBlank(floatingIp)) {
                    if (!floatingIp.equals(floatIp)){
                        String longFloatIp = IpUtil.ipToLong(floatingIp);
                        IpRange range = new IpRange(longFloatIp, longFloatIp);
                        ipSet.add(range);
                    }
                }
            }
            //src addr
            ArrayList addrList = new ArrayList();
            addrList.add(new SrcAddr());
            config.setSrcAddr(addrList);
            //dst range
            config.setDstRange(ipSet);
            ruleList.add(config);
            root.setRule(ruleList);
            condition.setRoot(root);
            //register the customer adapter
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(String.class, STRING);
            String conditionStr = gsonBuilder.serializeNulls().create().toJson(condition);
            log.info(conditionStr);
            //add the ip to ip Array
            String retr = HsHttpClient.hsHttpPut(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, "/rest/iQos?target=root.rule", conditionStr);
            JSONObject jo = new JSONObject(retr);
            log.info("removeQosPipeBindEip  result:{}", jo);
            boolean success = jo.getBoolean(HsConstants.SUCCESS);
            if (Boolean.valueOf(success)) {
                res.put(HsConstants.RESULT, "true");
                res.put(HsConstants.SUCCESS, "true");
            } else {
                res.put(HsConstants.RESULT, HsConstants.FALSE);
                res.put(HsConstants.SUCCESS, HsConstants.FALSE);
            }
        } catch (Exception var8) {
            log.error(var8.getMessage());
            res.put(HsConstants.SUCCESS, HsConstants.FALSE);
            res.put("msg", var8.getMessage());
            return res;
        }
        return res;
    }

    /**
     * add eip to sharedBand
     *
     * @param pipeId
     * @param pipeName
     * @param bandWidth
     * @return
     */
    private String getAddEipAddressJson(String pipeId, String pipeName, String bandWidth) {
        String json = "{ \"name\": \"first\",\"root\": {\"id\": \"" + pipeId + "\",\"name\": \"" + pipeName + "\", \"desc\": \"\", \"qos_mode\": { \"name\": \"shape\"}, \"action\": [{\"dir\": \"1\",\"min\": \"" + bandWidth + "\", \"max\": \"" + bandWidth + "\", \"per_min\": \"\", \"per_max\": \"\", \"per_using\": \"\", \"priority\": 7, \"set_tos\": \"2\"," + "\"tos\": \"\", \"amask\": {\"action_dir\": true,\"action_bandwidth\": false,\"action_reserve_bandwidth\": false," + "\"action_min\": false,\"action_max\": false,\"action_per_ip_min\": false,\"action_per_ip_max\": false, \"action_per_user_min\": false," + "\"action_per_user_max\": false, \"action_per_ip_using\": false, \"action_average_using\": false, \"action_tos_mark\": false," + "\"action_tos_int\": true,\"action_tos_str\": false, \"action_priority\": true, \"action_bandwidth_mbps\": true," + "\"action_reserve_bandwidth_mbps\": false,\"action_min_mbps\": false,\"action_max_mbps\": false,\"action_per_ip_min_mbps\": false," + "\"action_per_ip_max_mbps\": false,\"action_per_user_min_mbps\": false,\"action_per_user_max_mbps\": false, \"action_reserve_bandwidth_percent\": false," + "\"action_min_percent\": false,\"action_max_percent\": false,\"action_bandwidth_gbps\": false, \"action_rserve_bandwidth_gbps\": false," + "\"action_min_gbps\": false,\"action_max_gbps\": false,\"action_mode\": false}},{\"dir\": \"2\",\"min\": \"" + bandWidth + "\",\"max\":\"" + bandWidth + "\",\"per_min\": \"\",\"per_max\": \"\",\"per_using\": \"\", \"priority\": 7,\"set_tos\": \"2\", \"tos\": \"\"," + "\"amask\": {\"action_dir\": true, \"action_bandwidth\": false,\"action_reserve_bandwidth\": false,\"action_min\": false," + "\"action_max\": false, \"action_per_ip_min\": false, \"action_per_ip_max\": false,\"action_per_user_min\": false," + "\"action_per_user_max\": false, \"action_per_ip_using\": false, \"action_average_using\": false,\"action_tos_mark\": false," + "\"action_tos_int\": true, \"action_tos_str\": false, \"action_priority\": true, \"action_bandwidth_mbps\": true," + "\"action_reserve_bandwidth_mbps\": false, \"action_min_mbps\": false,\"action_max_mbps\": false,\"action_per_ip_min_mbps\": false," + "\"action_per_ip_max_mbps\": false,\"action_per_user_min_mbps\": false,\"action_per_user_max_mbps\": false,\"action_reserve_bandwidth_percent\": false," + "\"action_min_percent\": false,\"action_max_percent\": false, \"action_bandwidth_gbps\": false, \"action_rserve_bandwidth_gbps\": false," + "\"action_min_gbps\": false, \"action_max_gbps\": false,\"action_mode\": false}}],\"schedule\": [ ]}}";
        return json;
    }

    String getNewQosCheck(String pipeId) {
        String jsonStr = null;
        try {
            String params = "/rest/new_qos_qos_check?isDynamic=1&id=%7B%22pipe_id%22:%22" + pipeId + "%22,%22engine%22:%22first%22,%22type%22:0%7D&_dc=1544753559508";
            jsonStr = HsHttpClient.hsHttpGet(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, params);
            log.info("newQosCheck:------------" + jsonStr);
        } catch (Exception var11) {
            log.error(var11.getMessage());
        }
        return jsonStr;
    }

    List<Eip> getQueryQosByDataBase(String shareBandWidthId) {
        int isDelete = 0;
        List<Eip> eipList = null;
        try {
            eipList = eipRepository.findBySharedBandWidthIdAndIsDelete(shareBandWidthId, isDelete);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return eipList;
    }

    String getQueryQosRuleByStone(String bandId) {
        String jsonStr = null;
        String params = "/rest/iQos?query=%7B%22conditions%22%3A%5B%7B%22f%22%3A%22name%22%2C%22v%22%3A%22first%22%7D%2C%7B%22f%22%3A%22root.id%22%2C%22v%22%3A%221545234839113919447%22%7D%5D%7D&target=root.rule";
        try {
            HashMap<String, String> test = getQosPipeId("test");
            jsonStr = HsHttpClient.HttpGet(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, params);
            log.info("getQueryQosByDataBase:------------" + jsonStr);
            if (StringUtils.isBlank(jsonStr)) {
                throw new IllegalArgumentException("IllegalArgument ：" + bandId + " cannot get the qos rule");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonStr;
    }

//    public static void main(String[] args) {
//        QosService qs = new QosService("172.23.12.222", "443", "InnetAdmin", "innetadmin");
//        HashMap<String, String> map = new HashMap();
//        map.put("pipeName", "CQS-JXWSHXXHC-0000000361");
//        map.put("ip", "100.2.3.80");
//        map.put("serviceNamne", "Any");
//        map.put("serNetCardName", "ethernet0/1");
//        map.put("bandWidth", "100");
//        //qs.addRule("1508923278112971634", "2", "172.23.23.3", "any", "ethernet0/0", "ethernet0/1");
//    }

    //Customize the Strig adapter
    private static final TypeAdapter STRING = new TypeAdapter() {
        @Override
        public void write(JsonWriter out, Object value) throws IOException {
            if (value == null) {
                // 在这里处理null改为空字符串
                out.value("");
                return;
            }
            out.value((String) value);
        }

        public String read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return "";
            }
            return reader.nextString();
        }
    };

    public void setFwIp(String fwIp) {
        this.fwIp = fwIp;
    }

    public void setFwPort(String fwPort) {
        this.fwPort = fwPort;
    }

    public void setFwUser(String fwUser) {
        this.fwUser = fwUser;
    }

    public void setFwPwd(String fwPwd) {
        this.fwPwd = fwPwd;
    }


    JSONArray getQosRuleId(String pipeId) {

        try {
            String params = "/rest/iQos?query=%7B%22conditions%22%3A%5B%7B%22f%22%3A%22name%22%2C%22v%22%3A%22first%22%7D%2C%7B%22f%22%3A%22root.id%22%2C%22v%22%3A%22"+pipeId+"%22%7D%5D%7D&target=root.rule";
            String retr = HsHttpClient.hsHttpGet(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, params);
//            String retr = HsHttpClient.hsHttpGet("10.110.29.206", "443", "hillstone", "hillstone", params);
            JSONArray jo = new JSONArray(retr);
            log.info("Get qos rule return:{} ",jo.toString());
            return jo;
        } catch (Exception var11) {
            log.error(var11.getMessage());
            return null;
        }
    }

}
