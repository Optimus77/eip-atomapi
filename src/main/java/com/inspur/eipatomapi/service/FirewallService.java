package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.entity.fw.*;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.util.HsConstants;
import com.inspur.eipatomapi.util.JaspytUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
class FirewallService {

    @Autowired
    private FirewallRepository firewallRepository;

//    @Value("${jasypt.password}")
    private String secretKey = "EbfYkitulv73I2p0mXI50JMXoaxZTKJ7";
    private Firewall fireWallConfig = null;
    private String vr = "trust-vr";

    private Firewall getFireWallById(String id){
        if(null == fireWallConfig) {

            Optional<Firewall> firewall = firewallRepository.findById(id);
            if (firewall.isPresent()) {
                fireWallConfig = new Firewall();
                Firewall getFireWallEntity = firewall.get();

                fireWallConfig.setUser(JaspytUtils.decyptPwd(secretKey, getFireWallEntity.getUser()));
                fireWallConfig.setPasswd(JaspytUtils.decyptPwd(secretKey, getFireWallEntity.getPasswd()));
                fireWallConfig.setIp(getFireWallEntity.getIp());
                fireWallConfig.setPort(getFireWallEntity.getPort());
            } else {
                log.warn("Failed to find the firewall by id:{}", id);
            }
        }

        return fireWallConfig;
    }

    String addDnat(String innerip, String extip, String equipid) {
        String ruleid = null;

        //添加弹性IP
        FwDnatVo dnatVo = new FwDnatVo();
        Firewall accessFirewallBeanByNeid = getFireWallById(equipid);
        if(accessFirewallBeanByNeid != null) {
            dnatVo.setManageIP(accessFirewallBeanByNeid.getIp());
            dnatVo.setManagePort(accessFirewallBeanByNeid.getPort());
            dnatVo.setManageUser(accessFirewallBeanByNeid.getUser());
            dnatVo.setManagePwd(accessFirewallBeanByNeid.getPasswd());
            dnatVo.setDnatid("0");
            dnatVo.setVrid(vr);
            dnatVo.setVrname(vr);
            dnatVo.setSaddrtype("0");
            dnatVo.setSaddr("Any");
            dnatVo.setDaddrtype("1");
            dnatVo.setDaddr(extip);
            dnatVo.setDnatstat("1");
            dnatVo.setDescription("");
            dnatVo.setTransfer("1");//
            dnatVo.setTransferaddrtype("1");
            dnatVo.setTransferaddr(innerip);
            dnatVo.setIstransferport("1");
            dnatVo.setHa("0");

            NatService dnatimpl = new NatService();
            FwResponseBody body = dnatimpl.addPDnat(dnatVo);
            if (body.isSuccess()) {

                FwPortMapResult result = (FwPortMapResult) body.getObject();
                ruleid = result.getRule_id();
                log.info( "--add dnat successfully.innerIp:{}, dnatId:{}", innerip, ruleid);
            } else {
                log.info(innerip + "--Failed to add dnat:" + body.getException());
            }
        }
        return ruleid;
    }

    String addSnat(String innerip, String extip, String equipid) {
        String ruleid = null;

        FwSnatVo vo = new FwSnatVo();
        Firewall accessFirewallBeanByNeid = getFireWallById(equipid);
        if(accessFirewallBeanByNeid != null) {
            vo.setManageIP(accessFirewallBeanByNeid.getIp());
            vo.setManagePort(accessFirewallBeanByNeid.getPort());
            vo.setManageUser(accessFirewallBeanByNeid.getUser());
            vo.setManagePwd(accessFirewallBeanByNeid.getPasswd());

            vo.setVrid(vr);
            vo.setSnatstat("1");
            vo.setFlag("20");
            vo.setSaddr(innerip);
            vo.setSaddrtype("1");
            vo.setHa("0");
            vo.setSnatlog("true");
            vo.setPos_flag("1");
            vo.setSnatid("0");
            vo.setServicename("Any");

            vo.setDaddr("Any");
            vo.setDaddrtype("1");
            vo.setTransferaddr(extip);

            vo.setFlag("1");

            NatService dnatimpl = new NatService();
            FwResponseBody body = dnatimpl.addPSnat(vo);
            if (body.isSuccess()) {
                // 创建成功
                FwSnatVo result = (FwSnatVo) body.getObject();
                ruleid = result.getSnatid();
                log.info( "--Snat add successfully.innerIp:{}, snatId:{}", innerip, ruleid);
            } else {
                log.info(innerip + "--Failed to add snat:" + body.getException());
            }
        }
        return ruleid;
    }



    String addQos(String innerip, String eipid, String bandwidth, String equipid) {
        String pipid = null;

        Firewall fwBean = getFireWallById(equipid);
        if(fwBean != null) {
            QosService qs = new QosService(fwBean.getIp(), fwBean.getPort(), fwBean.getUser(), fwBean.getPasswd());
            HashMap<String, String> map = new HashMap<>();
            map.put("pipeName", eipid);
            map.put("ip", innerip);
            map.put("serviceNamne", "Any");
            map.put("mgNetCardName", fwBean.getParam3());
            map.put("serNetCardName", fwBean.getParam2());
            map.put("bandWidth", bandwidth);
            HashMap<String, String> res = qs.createQosPipe(map);
            JSONObject resJson= (JSONObject) JSONObject.toJSON(res);
            log.info("{}",resJson);
            if(resJson.getBoolean(HsConstants.SUCCESS)) {
                pipid = res.get("id");
                if (StringUtils.isBlank(pipid)) {
                    Map<String, String> idmap = qs.getQosPipeId(eipid);
                    pipid = idmap.get("id");
                }
                log.info("Qos add successfully.pipid:{}", pipid);
            } else {
                log.warn("Failde to add qos.");
            }
        }
        return pipid;
    }

    /**
     * update the Qos bindWidth
     * @param firewallId  firewall id
     * @param bindwidth   bind width
     * @return            result
     */
    boolean updateQosBandWidth(String firewallId,String pipId, String pipNmae,String bindwidth){

        Firewall fwBean = getFireWallById(firewallId);
        if(fwBean != null) {
            QosService qs = new QosService(fwBean.getIp(), fwBean.getPort(), fwBean.getUser(), fwBean.getPasswd());
            HashMap<String, String> result = qs.updateQosPipe(pipId, pipNmae, bindwidth);
            JSONObject resJson= (JSONObject) JSONObject.toJSON(result);
            log.info("",resJson);
            if (resJson.getBoolean(HsConstants.SUCCESS)) {
                log.info("updateQosBandWidth: " + firewallId + " --success==bindwidth：" + bindwidth);
            } else {
                log.info("updateQosBandWidth: " + firewallId + " --fail==bindwidth：" + bindwidth);
            }
            return resJson.getBoolean(HsConstants.SUCCESS);
        }
        return Boolean.parseBoolean("False");
    }


    /**
     *  del qos
     * @param pipid pipid
     * @param devId  devid
     * @return  ret
     */
    boolean delQos(String pipid, String devId) {
        if (StringUtils.isNotEmpty(pipid)) {
            Firewall fwBean = getFireWallById(devId);
            if(null != fwBean) {
                QosService qs = new QosService(fwBean.getIp(), fwBean.getPort(), fwBean.getUser(), fwBean.getPasswd());
                qs.delQosPipe(pipid);
            } else {
                log.info("Failed to del qos:"+"dev【"+devId+"】,pipid【"+pipid+"】");
            }
        }

        return true;
    }

    boolean delDnat(String ruleid, String devId) {
        boolean bSuccess = true;
        if ("offline".equals(ruleid)) {
            return bSuccess;
        }

        if (StringUtils.isNotEmpty(ruleid)) {
            FwDnatVo vo = new FwDnatVo();
            Firewall accessFirewallBeanByNeid = getFireWallById(devId);
            if(accessFirewallBeanByNeid != null) {
                vo.setManageIP(accessFirewallBeanByNeid.getIp());
                vo.setManagePort(accessFirewallBeanByNeid.getPort());
                vo.setManageUser(accessFirewallBeanByNeid.getUser());
                vo.setManagePwd(accessFirewallBeanByNeid.getPasswd());

                vo.setDnatid(ruleid);
                vo.setVrid(vr);
                vo.setVrname(vr);

                NatService dnatimpl = new NatService();
                FwResponseBody body = dnatimpl.delPDnat(vo);
                if (body.isSuccess() || (body.getException().getMessage().contains("cannot be found"))) {
                    bSuccess = true;
                } else {
                    bSuccess = false;
                    log.warn("Failed to del dnat:" + "dev[" + devId + "],ruleid[" + ruleid + "]");
                }
            }
        }
        return bSuccess;
    }
    boolean delSnat(String ruleid, String devId) {
        boolean bSuccess = true;
        if ("offline".equals(ruleid)) {
            return bSuccess;
        }
        if (StringUtils.isNotEmpty(ruleid)) {
            FwSnatVo vo = new FwSnatVo();

            Firewall accessFirewallBeanByNeid = getFireWallById(devId);
            if(accessFirewallBeanByNeid != null) {
                vo.setManageIP(accessFirewallBeanByNeid.getIp());
                vo.setManagePort(accessFirewallBeanByNeid.getPort());
                vo.setManageUser(accessFirewallBeanByNeid.getUser());
                vo.setManagePwd(accessFirewallBeanByNeid.getPasswd());

                vo.setVrid(vr);
                vo.setSnatid(ruleid);

                NatService dnatimpl = new NatService();
                FwResponseBody body = dnatimpl.delPSnat(vo);

                if (body.isSuccess() || (body.getException().getMessage().contains("cannot be found"))) {
                    bSuccess = true;
                } else {
                    bSuccess = false;
                    log.info("Failed to del snat:" + "dev[" + devId + "],ruleid[" + ruleid + "]");
                }
            }
        }
        return bSuccess;
    }

}
