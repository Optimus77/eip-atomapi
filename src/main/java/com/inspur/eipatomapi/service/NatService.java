package com.inspur.eipatomapi.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inspur.eipatomapi.entity.fw.*;
import com.inspur.eipatomapi.util.HsConstants;
import com.inspur.eipatomapi.util.HsHttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NatService  {

    public final static Logger log = LoggerFactory.getLogger(NatService.class);

    public List<FwSnatVo> getSnat(FwQuery query, FwBaseObject manage) {
        List<FwSnatVo> snats = new ArrayList<>();
        Gson gson = new Gson();

        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("query", gson.toJson(query)));
            String retr = HsHttpClient.hsHttpGet(manage.getManageIP(), null, manage.getManageUser(), manage.getManagePwd(), "/rest/Snat?isDynamic=0&" + URLEncodedUtils.format(params, "UTF-8"));
            JSONObject jo = new JSONObject(retr);
            jo.getBoolean(HsConstants.SUCCESS);
            return snats;
        } catch (Exception var8) {
            log.error("Exception when get snat.",var8);
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
            if (jo.getBoolean(HsConstants.SUCCESS)) {
                FwSnat hsSnat = gson.fromJson(jo.getJSONArray("result").getJSONObject(0)
                        .getJSONObject("vr").getJSONObject("vrouter")
                        .getJSONObject("snat_rule").toString(), FwSnat.class);
                resultVo.setSnatid(hsSnat.getRule_id());
                body.setObject(resultVo);
            }

            body.setSuccess(jo.getBoolean(HsConstants.SUCCESS));
            body.setException((gson.fromJson(jo.getJSONObject(HsConstants.EXCEPTION).toString(),
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
            Map<String, Object> payloadMap = new HashMap<>();
            Map<String, String> idMap = new HashMap<>();
            idMap.put("rule_id", snat.getSnatid());
            payloadMap.put("vr_name", snat.getVrid());
            payloadMap.put("snat_rule", idMap);
            String retr = HsHttpClient.hsHttpDelete(snat.getManageIP(), snat.getManagePort(), snat.getManageUser(), snat.getManagePwd(), "/rest/Snat?target=snat_rule", gson.toJson(payloadMap));
            JSONObject jo = new JSONObject(retr);
            body.setSuccess(jo.getBoolean(HsConstants.SUCCESS));
            body.setException(gson.fromJson(jo.getJSONObject(HsConstants.EXCEPTION).toString(),
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
            body.setSuccess(jo.getBoolean(HsConstants.SUCCESS));
            if (!body.isSuccess()) {
                body.setException(gson.fromJson(jo.getJSONObject(HsConstants.EXCEPTION).toString(),
                        FwResponseException.class));
                return body;
            } else {
                JSONArray jo_result = jo.getJSONArray("result");
                JSONObject jo_vr = jo_result.getJSONObject(0);
                JSONObject jo_vr_item = jo_vr.getJSONObject("vr");
                JSONObject jo_vrouter = jo_vr_item.getJSONObject("vrouter");
                body.setObject(gson.fromJson(jo_vrouter.getJSONObject("dnat_rule").toString(), FwPortMapResult.class));
                return body;
            }
        } catch (Exception var11) {
            log.error("IO Exception when add dnat.",var11);
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
            body.setSuccess(jo.getBoolean(HsConstants.SUCCESS));
            if (!body.isSuccess()) {
                body.setException(gson.fromJson(jo.getJSONObject(HsConstants.EXCEPTION).toString(),
                        FwResponseException.class));
            }

            return body;
        } catch (Exception var6) {
            log.error("IO Exception when del dnat.",var6);
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
        object.setVr_name(dnat.getVrid());
        rule.setRule_id(dnat.getDnatid());
        rule.setGroup_id(dnat.getHa());
        rule.setFrom(dnat.getSaddr());
        rule.setFrom_is_ip(dnat.getSaddrtype());
        rule.setService(dnat.getServicename());
        rule.setTo(dnat.getDaddr());
        rule.setTo_is_ip(dnat.getDaddrtype());
        rule.setTrans_to(dnat.getTransferaddr());
        rule.setTrans_to_is_ip(dnat.getTransferaddrtype());
        if ("1".equals(dnat.getIstransferport())) {
            rule.setPort(dnat.getTransferport());
        }

        rule.setEnable(dnat.getDnatstat());
        rule.setDescription(dnat.getDescription());
        object.getDnat_rule().add(rule);
        Gson gson = new Gson();
        return gson.toJson(object);
    }

    private String addSnatPayload(String operator,FwSnatVo vo) {

        Gson gson;
        FwSnatParam snatParam = new FwSnatParam();
        snatParam.setVr_name(vo.getVrid());

        FwSnat snat = new FwSnat();

        if ("add".equals(operator)) {
            gson = new Gson();
            snat.setPos_flag(vo.getPos_flag());
            snat.setTrans_to_is_ip("1");
        }else if ("update".equals(operator)) {
            gson = new GsonBuilder().serializeNulls().create();
            snat.setTrans_to("");
            snat.setTrans_to_is_ip("");
            snat.setEvr("");
            snat.setPos_flag("");
        }else {
            return null;
        }

        snat.setDescription(vo.getDescription());
        snat.setEif(vo.getEif());
        snat.setEnable(Integer.parseInt(vo.getSnatstat()));
        snat.setFlag(vo.getFlag());
        snat.setFrom(vo.getSaddr());
        snat.setFrom_is_ip(vo.getSaddrtype());
        snat.setGroup_id(vo.getHa());
        snat.setLog(Boolean.parseBoolean(vo.getSnatlog()));
        snat.setRule_id(vo.getSnatid());
        snat.setService(vo.getServicename());
        snat.setTo(vo.getDaddr());
        snat.setTrans_to(vo.getTransferaddr());
        snat.setTo_is_ip(vo.getDaddrtype());

        snatParam.getSnat_rule().add(snat);

        if ("update".equals(operator)) {
            return "["+gson.toJson(snatParam)+"]";
        }
        return gson.toJson(snatParam);
    }

}
