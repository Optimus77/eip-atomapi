package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.util.HsHttpClient;
import com.inspur.eipatomapi.util.IpUtil;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QosService extends BaseService {
    private String fwIp;
    private String fwPort;
    private String fwUser;
    private String fwPwd;

    public final static Logger log = LoggerFactory.getLogger(QosService.class);

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
            boolean success = jo.getBoolean("success");
            res.put("success", success);
            if (success) {
                Map<String, String> map = this.getQosPipeId((String)info.get("pipeName"));
                if (((String)map.get("success")).equals("true")) {
                    res.put("id", (String)map.get("id"));
                } else {
                    res.put("msg", "创建成功,未能返回id,请主动调用查询接口,根据名称查询新创建的管道id");
                }
            } else {
                res.put("msg", jo.getString("exception"));
            }

            return res;
        } catch (Exception var7) {
            log.error(var7.getMessage());
            res.put("success", "false");
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
            boolean success = jo.getBoolean("success");
            if (success) {
                res.put("success", "true");
            } else if ("Error: The root pipe dose not exist".equals(jo.getJSONObject("exception").getString("message"))) {
                res.put("success", "true");
                res.put("msg", "传入的pipeId不存在");
            } else {
                res.put("success", "false");
                res.put("msg", jo.getString("exception"));
            }

            return res;
        } catch (Exception var7) {
            log.error(var7.getMessage());
            res.put("success", "false");
            res.put("msg", var7.getMessage());
            return res;
        }
    }

    HashMap<String, String> updateQosPipe(String pipeId, String pipeName, String bandWidth) {
        HashMap res = new HashMap();

        try {
            String retr = HsHttpClient.hsHttpPut(this.fwIp, this.fwPort, this.fwUser, this.fwPwd, "/rest/iQos?target=root", this.getUpdateJson(pipeId, pipeName, bandWidth));
            JSONObject jo = new JSONObject(retr);
            String success = jo.getString("success");
            res.put("success", success);
            if (!"true".equals(success)) {
                res.put("msg", jo.getString("exception"));
            }

            return res;
        } catch (Exception var8) {
            log.error(var8.getMessage());
            res.put("success", "false");
            res.put("msg", var8.getMessage());
            return res;
        }
    }


    String getCreatePipeJson(HashMap<String, String> map) throws Exception {
        try {
            String s = "{\"name\": \"first\",\"root\": {\"name\":\"" + (String)map.get("pipeName") + "\",\"desc\": \"\",\"qos_mode\": {\"name\": \"shape\"},\"rule\": [{ \"id\": [],\"src_addr\": [{\"name\": \"any\"}]," + "\"src_host\": [],\"src_subnet\": [],\"src_range\": [],\"dst_addr\": [],\"dst_host\": [],\"dst_subnet\": [{\"ip\":" + IpUtil.ipToLong((String)map.get("ip")) + ",\"netmask\":32" + "}],\"dst_range\": [],\"user\": [],\"usergroup\": [],\"service\": [{\"name\": \"" + (String)map.get("serviceNamne") + "\"}],\"application\": [],\"src_zone\": [],\"ingress_if\": [],\"dst_zone\": [],\"egress_if\": []" + ",\"vlan\": [],\"tos\": []}],\"action\": [{\"dir\": \"1\",\"min\": \"" + (String)map.get("bandWidth") + "\", \"max\":\"" + (String)map.get("bandWidth") + "\",\"per_min\": \"\",\"per_max\": \"\",\"per_using\": \"\",\"priority\": 7,\"set_tos\": \"2\",\"tos\": \"\",\"amask\": {" + "\"action_dir\": true,\"action_bandwidth\": false,\"action_reserve_bandwidth\": false,\"action_min\": false,\"action_max\": false," + "\"action_per_ip_min\": false,\"action_per_ip_max\": false,\"action_per_user_min\": false,\"action_per_user_max\": false," + "\"action_per_ip_using\": false,\"action_average_using\": false,\"action_tos_mark\": false,\"action_tos_int\": true," + "\"action_tos_str\": false,\"action_priority\": true,\"action_bandwidth_mbps\": true,\"action_reserve_bandwidth_mbps\": false," + "\"action_min_mbps\": false,\"action_max_mbps\": false,\"action_per_ip_min_mbps\": false,\"action_per_ip_max_mbps\": false," + "\"action_per_user_min_mbps\": false,\"action_per_user_max_mbps\": false,\"action_reserve_bandwidth_percent\": false,\"action_min_percent\": false," + "\"action_max_percent\": false,\"action_bandwidth_gbps\": false,\"action_rserve_bandwidth_gbps\": false,\"action_min_gbps\": false," + "\"action_max_gbps\": false,\"action_mode\": false}},{\"dir\": \"2\",\"min\": \"" + (String)map.get("bandWidth") + "\", \"max\":\"" + (String)map.get("bandWidth") + "\",\"per_min\": \"\",\"per_max\": \"\",\"per_using\": \"\",\"priority\": 7,\"set_tos\": \"2\",\"tos\": \"\",\"amask\": {" + "\"action_dir\": true,\"action_bandwidth\": false,\"action_reserve_bandwidth\": false,\"action_min\": false,\"action_max\": false," + "\"action_per_ip_min\": false,\"action_per_ip_max\": false,\"action_per_user_min\": false,\"action_per_user_max\": false,\"action_per_ip_using\": false," + "\"action_average_using\": false,\"action_tos_mark\": false,\"action_tos_int\": true,\"action_tos_str\": false,\"action_priority\": true," + "\"action_bandwidth_mbps\": true,\"action_reserve_bandwidth_mbps\": false,\"action_min_mbps\": false,\"action_max_mbps\": false,\"action_per_ip_min_mbps\": false," + "\"action_per_ip_max_mbps\": false,\"action_per_user_min_mbps\": false,\"action_per_user_max_mbps\": false,\"action_reserve_bandwidth_percent\": false," + "\"action_min_percent\": false,\"action_max_percent\": false,\"action_bandwidth_gbps\": false,\"action_rserve_bandwidth_gbps\": false," + "\"action_min_gbps\": false,\"action_max_gbps\": false,\"action_mode\": false}}],\"id\": 0}}";
            return s;
        } catch (Exception var4) {
            throw var4;
        }
    }

    private String getUpdateJson(String pipeId, String pipeName, String bandWidth) {
        String json = "{ \"name\": \"first\",\"root\": {\"id\": \"" + pipeId + "\",\"name\": \"" + pipeName + "\", \"desc\": \"\", \"qos_mode\": { \"name\": \"shape\"}, \"action\": [{\"dir\": \"1\",\"min\": \"" + bandWidth + "\", \"max\": \"" + bandWidth + "\", \"per_min\": \"\", \"per_max\": \"\", \"per_using\": \"\", \"priority\": 7, \"set_tos\": \"2\"," + "\"tos\": \"\", \"amask\": {\"action_dir\": true,\"action_bandwidth\": false,\"action_reserve_bandwidth\": false," + "\"action_min\": false,\"action_max\": false,\"action_per_ip_min\": false,\"action_per_ip_max\": false, \"action_per_user_min\": false," + "\"action_per_user_max\": false, \"action_per_ip_using\": false, \"action_average_using\": false, \"action_tos_mark\": false," + "\"action_tos_int\": true,\"action_tos_str\": false, \"action_priority\": true, \"action_bandwidth_mbps\": true," + "\"action_reserve_bandwidth_mbps\": false,\"action_min_mbps\": false,\"action_max_mbps\": false,\"action_per_ip_min_mbps\": false," + "\"action_per_ip_max_mbps\": false,\"action_per_user_min_mbps\": false,\"action_per_user_max_mbps\": false, \"action_reserve_bandwidth_percent\": false," + "\"action_min_percent\": false,\"action_max_percent\": false,\"action_bandwidth_gbps\": false, \"action_rserve_bandwidth_gbps\": false," + "\"action_min_gbps\": false,\"action_max_gbps\": false,\"action_mode\": false}},{\"dir\": \"2\",\"min\": \"" + bandWidth + "\",\"max\":\"" + bandWidth + "\",\"per_min\": \"\",\"per_max\": \"\",\"per_using\": \"\", \"priority\": 7,\"set_tos\": \"2\", \"tos\": \"\"," + "\"amask\": {\"action_dir\": true, \"action_bandwidth\": false,\"action_reserve_bandwidth\": false,\"action_min\": false," + "\"action_max\": false, \"action_per_ip_min\": false, \"action_per_ip_max\": false,\"action_per_user_min\": false," + "\"action_per_user_max\": false, \"action_per_ip_using\": false, \"action_average_using\": false,\"action_tos_mark\": false," + "\"action_tos_int\": true, \"action_tos_str\": false, \"action_priority\": true, \"action_bandwidth_mbps\": true," + "\"action_reserve_bandwidth_mbps\": false, \"action_min_mbps\": false,\"action_max_mbps\": false,\"action_per_ip_min_mbps\": false," + "\"action_per_ip_max_mbps\": false,\"action_per_user_min_mbps\": false,\"action_per_user_max_mbps\": false,\"action_reserve_bandwidth_percent\": false," + "\"action_min_percent\": false,\"action_max_percent\": false, \"action_bandwidth_gbps\": false, \"action_rserve_bandwidth_gbps\": false," + "\"action_min_gbps\": false, \"action_max_gbps\": false,\"action_mode\": false}}],\"schedule\": [ ]}}";
        return json;
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

            for(int i = 0; i < l; ++i) {
                JSONObject job = array.getJSONObject(i);
                if (pipeName.equals(job.optString("name"))) {
                    id = job.optString("id");
                    break;
                }
            }

            res.put("success", "true");
            res.put("id", id);
            return res;
        } catch (Exception var11) {
            log.error(var11.getMessage());
            res.put("success", "false");
            res.put("msg", var11.getMessage());
            return res;
        }
    }

    public static void main(String[] args) {
        QosService qs = new QosService("172.23.12.222", "443", "InnetAdmin", "innetadmin");
        HashMap<String, String> map = new HashMap();
        map.put("pipeName", "CQS-JXWSHXXHC-0000000361");
        map.put("ip", "100.2.3.80");
        map.put("serviceNamne", "Any");
        map.put("serNetCardName", "ethernet0/1");
        map.put("bandWidth", "100");
        //qs.addRule("1508923278112971634", "2", "172.23.23.3", "any", "ethernet0/0", "ethernet0/1");
    }
}
