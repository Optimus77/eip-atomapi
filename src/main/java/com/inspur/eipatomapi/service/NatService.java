package com.inspur.eipatomapi.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inspur.eipatomapi.entity.*;
import com.inspur.eipatomapi.util.HsConstants;
import com.inspur.eipatomapi.util.HsHttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NatService extends BaseService {

    public List<FwSnatVo> getSnat(FwQuery query, FwBaseObject manage) {
        List<FwSnatVo> snats = new ArrayList();
        Gson gson = new Gson();

        try {
            List<NameValuePair> params = new ArrayList();
            params.add(new BasicNameValuePair("query", gson.toJson(query)));
            String retr = HsHttpClient.hsHttpGet(manage.getManageIP(), null, manage.getManageUser(), manage.getManagePwd(), "/rest/Snat?isDynamic=0&" + URLEncodedUtils.format(params, "UTF-8"));
            JSONObject jo = new JSONObject(retr);
            jo.getBoolean("successful");
            return snats;
        } catch (Exception var8) {
            this.logger.error(var8);
            return snats;
        }
    }

    public FwResponseBody addPSnat(FwSnatVo snat) {
        FwResponseBody body = new FwResponseBody();
        FwSnatVo resultVo = new FwSnatVo();
        Gson gson = new Gson();
        try {

            String retr = HsHttpClient.hsHttpPost(snat.getManageIP(), snat.getManagePort(), snat.getManageUser(), snat.getManagePwd(),
                    HsConstants.REST_SNAT + HsConstants.REST_SNAT_ADD_UPDATE_DELETE, addSnatPayload("add",snat));


            JSONObject jo = new JSONObject(retr);
            if (jo.getBoolean("Success")) {
                FwSnat hsSnat = gson.fromJson(jo.getJSONArray("result").getJSONObject(0)
                        .getJSONObject("vr").getJSONObject("vrouter")
                        .getJSONObject("snat_rule").toString(), FwSnat.class);
                resultVo.setSnatid(hsSnat.getRuleId());
                body.setObject(resultVo);
            }

            body.setSuccess(jo.getBoolean("success"));
            body.setException((gson.fromJson(jo.getJSONObject("exception").toString(),
                    FwResponseException.class)));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return body;
    }
    public FwResponseBody delPSnat(FwSnatVo snat) {
        FwResponseBody body = new FwResponseBody();
        Gson gson = new Gson();

        try {
            Map<String, Object> payloadMap = new HashMap();
            Map<String, String> idMap = new HashMap();
            idMap.put("rule_id", snat.getSnatid());
            payloadMap.put("vr_name", snat.getVrid());
            payloadMap.put("snat_rule", idMap);
            String retr = HsHttpClient.hsHttpDelete(snat.getManageIP(), snat.getManagePort(), snat.getManageUser(), snat.getManagePwd(), "/rest/Snat?target=snat_rule", gson.toJson(payloadMap));
            JSONObject jo = new JSONObject(retr);
            body.setSuccess(jo.getBoolean("success"));
            body.setException(gson.fromJson(jo.getJSONObject("exception").toString(),
                    FwResponseException.class));
        } catch (Exception var8) {
            var8.printStackTrace();
        }

        return body;
    }

    public FwResponseBody addPDnat(FwDnatVo dnat) {
        Gson gson = new Gson();
        FwResponseBody body = new FwResponseBody();

        try {
            String retr = HsHttpClient.hsHttpPost(dnat.getManageIP(), dnat.getManagePort(), dnat.getManageUser(), dnat.getManagePwd(), "/rest/Dnat?target=dnat_rule", this.getPayload(dnat));
            JSONObject jo = new JSONObject(retr);
            body.setSuccess(jo.getBoolean("success"));
            if (!body.isSuccess()) {
                body.setException(gson.fromJson(jo.getJSONObject("exception").toString(),
                        FwResponseException.class));
                return body;
            } else {
                JSONArray jo_result = jo.getJSONArray("result");
                JSONObject jo_vr = jo_result.getJSONObject(0);
                JSONObject jo_vr_item = jo_vr.getJSONObject("vr");
                JSONObject jo_vrouter = jo_vr_item.getJSONObject("vrouter");
                body.setObject(gson.fromJson(jo_vrouter.getJSONObject("dnat_rule").toString(), FwPortMapResult.class));
                FwPortMapResult result = (FwPortMapResult) body.getObject();
                return body;
            }
        } catch (Exception var11) {
            this.logger.error(var11);
            body.setSuccess(false);
            FwResponseException ex = new FwResponseException();
            ex.setCode("-1");
            ex.setMessage(var11.getMessage());
            body.setException(ex);
            return body;
        }
    }

    public FwResponseBody delPDnat(FwDnatVo dnat) {
        Gson gson = new Gson();
        FwResponseBody body = new FwResponseBody();

        try {
            String retr = HsHttpClient.hsHttpDelete(dnat.getManageIP(), dnat.getManagePort(),
                    dnat.getManageUser(), dnat.getManagePwd(),
                    "/rest/Dnat?target=dnat_rule", this.getPayload(dnat));
            JSONObject jo = new JSONObject(retr);
            body.setSuccess(jo.getBoolean("success"));
            if (!body.isSuccess()) {
                body.setException(gson.fromJson(jo.getJSONObject("exception").toString(),
                        FwResponseException.class));
            }

            return body;
        } catch (Exception var6) {
            this.logger.error(var6);
            body.setSuccess(false);
            FwResponseException ex = new FwResponseException();
            ex.setCode("-1");
            ex.setMessage(var6.getMessage());
            body.setException(ex);
            return body;
        }
    }
    private String getPayload(FwDnatVo dnat) {
        FwAddAndDelDnat object = new FwAddAndDelDnat();
        FwDnatRule rule = new FwDnatRule();
        object.setVrName(dnat.getVrid());
        rule.setRuleId(dnat.getDnatid());
        rule.setGroupId(dnat.getHa());
        rule.setFrom(dnat.getSaddr());
        rule.setFromIsIp(dnat.getSaddrtype());
        rule.setService(dnat.getServicename());
        rule.setTo(dnat.getDaddr());
        rule.setToIsIp(dnat.getDaddrtype());
        rule.setTransTo(dnat.getTransferaddr());
        rule.setTransToIsIp(dnat.getTransferaddrtype());
        if ("1".equals(dnat.getIstransferport())) {
            rule.setPort(dnat.getTransferport());
        }

        rule.setEnable(dnat.getDnatstat());
        rule.setDescription(dnat.getDescription());
        object.getDnatRule().add(rule);
        Gson gson = new Gson();
        String payload = gson.toJson(object);
        return payload;
    }

    private String addSnatPayload(String operator,FwSnatVo vo) throws Exception{

        Gson gson;
        FwSnatParam snatParam = new FwSnatParam();
        snatParam.setVrName(vo.getVrid());

        FwSnat snat = new FwSnat();

        if ("add".equals(operator)) {
            gson = new Gson();
            snat.setPosFlag(vo.getPosFlag());
            snat.setTransToIsIp("1");
        }else if ("update".equals(operator)) {
            gson = new GsonBuilder().serializeNulls().create();
            snat.setTransTo("");
            snat.setTransToIsIp("");
            snat.setEvr("");
            snat.setPosFlag("");
        }else {
            return null;
        }

        snat.setDescription(vo.getDescription());
        snat.setEif(vo.getEif());
        snat.setEnable(Integer.parseInt(vo.getSnatstat()));
        snat.setFlag(vo.getFlag());
        snat.setFrom(vo.getSaddr());
        snat.setFromIsIp(vo.getSaddrtype());
        snat.setGroupId(vo.getHa());
        snat.setLog(Boolean.parseBoolean(vo.getSnatlog()));
        snat.setRuleId(vo.getSnatid());
        snat.setService(vo.getServicename());
        snat.setTo(vo.getDaddr());
        snat.setTransTo(vo.getTransferaddr());
        snat.setToIsIp(vo.getDaddrtype());

        snatParam.getSnatRule().add(snat);

        if ("update".equals(operator)) {
            return "["+gson.toJson(snatParam)+"]";
        }
        System.out.println("-----------------------------");
        System.out.println(snatParam.toString());
        return gson.toJson(snatParam);
    }

}
