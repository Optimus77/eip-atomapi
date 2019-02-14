package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.entity.MethodReturn;
import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.entity.fw.*;
import com.inspur.eipatomapi.entity.sbw.Sbw;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.repository.SbwRepository;
import com.inspur.eipatomapi.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.apache.http.HttpStatus;
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

    @Autowired
    private QosService qosService;

    @Autowired
    private SbwRepository sbwRepository;

    @Autowired
    private EipRepository eipRepository;
    //    @Value("${jasypt.password}")
    private String secretKey = "EbfYkitulv73I2p0mXI50JMXoaxZTKJ7";
    private Map<String, Firewall> firewallConfigMap = new HashMap<>();
    private String vr = "trust-vr";

    private Firewall getFireWallById(String id) {
        if (!firewallConfigMap.containsKey(id)) {

            Optional<Firewall> firewall = firewallRepository.findById(id);
            if (firewall.isPresent()) {
                Firewall fireWallConfig = new Firewall();
                Firewall getFireWallEntity = firewall.get();

                fireWallConfig.setUser(JaspytUtils.decyptPwd(secretKey, getFireWallEntity.getUser()));
                fireWallConfig.setPasswd(JaspytUtils.decyptPwd(secretKey, getFireWallEntity.getPasswd()));
                fireWallConfig.setIp(getFireWallEntity.getIp());
                fireWallConfig.setPort(getFireWallEntity.getPort());
                firewallConfigMap.put(id, fireWallConfig);
                log.info("get firewall ip:{}, port:{}, passwd:{}, user:{}", fireWallConfig.getIp(),
                        fireWallConfig.getPort(), getFireWallEntity.getUser(), getFireWallEntity.getPasswd());
            } else {
                log.warn("Failed to find the firewall by id:{}", id);
            }
        }

        return firewallConfigMap.get(id);
    }

    String addDnat(String innerip, String extip, String equipid) {
        String ruleid = null;

        //添加弹性IP
        FwDnatVo dnatVo = new FwDnatVo();
        Firewall accessFirewallBeanByNeid = getFireWallById(equipid);
        if (accessFirewallBeanByNeid != null) {
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
                log.info("--add dnat successfully.innerIp:{}, dnatId:{}", innerip, ruleid);
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
        if (accessFirewallBeanByNeid != null) {
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
                log.info("--Snat add successfully.innerIp:{}, snatId:{}", innerip, ruleid);
            } else {
                log.info(innerip + "--Failed to add snat:" + body.getException());
            }
        }
        return ruleid;
    }


    String addQos(String innerip, String name, String bandwidth, String fireWallId) {
        String pipid = null;

        Firewall fwBean = getFireWallById(fireWallId);
        if (fwBean != null) {
            QosService qs = new QosService(fwBean.getIp(), fwBean.getPort(), fwBean.getUser(), fwBean.getPasswd());
            HashMap<String, String> map = new HashMap<>();
            map.put("pipeName", name);
            if (innerip != null) {
                map.put("ip", innerip);
            }
            map.put("serviceNamne", "Any");
            map.put("mgNetCardName", fwBean.getParam3());
            map.put("serNetCardName", fwBean.getParam2());
            map.put("bandWidth", bandwidth);
            HashMap<String, String> res = qs.createQosPipe(map);
            JSONObject resJson = (JSONObject) JSONObject.toJSON(res);
            if (resJson.getBoolean(HsConstants.SUCCESS)) {
                pipid = res.get("id");
                if (StringUtils.isBlank(pipid)) {
                    Map<String, String> idmap = qs.getQosPipeId(name);
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
     * @param firewallId firewall id
     * @param bindwidth  bind width
     */
    boolean updateQosBandWidth(String firewallId, String pipId, String pipNmae, String bindwidth) {

        Firewall fwBean = getFireWallById(firewallId);
        if (fwBean != null) {
            QosService qs = new QosService(fwBean.getIp(), fwBean.getPort(), fwBean.getUser(), fwBean.getPasswd());
            HashMap<String, String> map = qs.updateQosPipe(pipId, pipNmae, bindwidth);
            JSONObject resJson = (JSONObject) JSONObject.toJSON(map);
            log.info("", resJson);
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
     * del qos
     * @param pipid pipid
     * @param devId devid
     * @return ret
     */
    boolean delQos(String pipid, String devId) {
        if (StringUtils.isNotEmpty(pipid)) {
            Firewall fwBean = getFireWallById(devId);
            if (null != fwBean) {
                QosService qs = new QosService(fwBean.getIp(), fwBean.getPort(), fwBean.getUser(), fwBean.getPasswd());
                HashMap<String, String> map = qs.delQosPipe(pipid);
                if (Boolean.valueOf(map.get(HsConstants.SUCCESS))) {
                    return true;
                }
            } else {
                log.info("Failed to get fireWall by id when del qos,dev:{}, pipId:{}",devId,pipid);
            }
        }else {
            log.info("qos id is empty, no need to del qos.");
            return true;
        }
        return false;
    }

    boolean delDnat(String ruleid, String devId) {
        boolean bSuccess = true;
        if ("offline".equals(ruleid)) {
            return bSuccess;
        }

        if (StringUtils.isNotEmpty(ruleid)) {
            FwDnatVo vo = new FwDnatVo();
            Firewall accessFirewallBeanByNeid = getFireWallById(devId);
            if (accessFirewallBeanByNeid != null) {
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
            if (accessFirewallBeanByNeid != null) {
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


    MethodReturn addNatAndQos(Eip eip, String fipAddress, String eipAddress, int bandWidth, String firewallId) {
        String pipId = null;
        String dnatRuleId = null;
        String snatRuleId = null;
        String returnStat;
        String returnMsg;
        try {
            if (eip.getChargeMode().equalsIgnoreCase(HsConstants.SHAREDBANDWIDTH)) {
                Sbw sbwEntity = sbwRepository.findBySbwId(eip.getSharedBandWidthId());
                if (null != sbwEntity) {
                    pipId = addFloatingIPtoQos(eip.getFirewallId(), fipAddress, sbwEntity.getPipeId(), eip.getSharedBandWidthId(), eip.getBandWidth());
                }
            } else {
                pipId = addQos(fipAddress, eipAddress, String.valueOf(bandWidth), firewallId);
            }
            if (null != pipId || CommonUtil.qosDebug) {
                dnatRuleId = addDnat(fipAddress, eipAddress, firewallId);
                if (dnatRuleId != null) {
                    snatRuleId = addSnat(fipAddress, eipAddress, firewallId);
                    if (snatRuleId != null) {
                        eip.setDnatId(dnatRuleId);
                        eip.setSnatId(snatRuleId);
                        eip.setPipId(pipId);
                        log.info("add nat and qos successfully. snat:{}, dnat:{}, qos:{}",
                                eip.getSnatId(), eip.getDnatId(), eip.getPipId());

                        return MethodReturnUtil.success(eip);
                    } else {
                        returnStat = ReturnStatus.SC_FIREWALL_SNAT_UNAVAILABLE;
                        returnMsg = CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_SNAT_ERROR);
                    }
                } else {
                    returnStat = ReturnStatus.SC_FIREWALL_DNAT_UNAVAILABLE;
                    returnMsg = CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_DNAT_ERROR);
                }
            } else {
                returnStat = ReturnStatus.SC_FIREWALL_QOS_UNAVAILABLE;
                returnMsg = CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_QOS_ERROR);
            }
        } catch (Exception e) {
            log.error("band server exception", e);
            returnStat = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
            returnMsg = e.getMessage();
        } finally {
            if (null == snatRuleId) {
                if (null != dnatRuleId) {
                    delDnat(dnatRuleId, eip.getFirewallId());
                }
                if (null != pipId) {
                    delQos(pipId, eip.getFirewallId());
                }
            }
        }
        return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, returnStat, returnMsg);
    }

    MethodReturn delNatAndQos(Eip eipEntity) {

        String msg = null;
        String returnStat = "200";
        if (delDnat(eipEntity.getDnatId(), eipEntity.getFirewallId())) {
            eipEntity.setDnatId(null);
        } else {
            returnStat = ReturnStatus.SC_FIREWALL_DNAT_UNAVAILABLE;
            msg = "Failed to del dnat in firewall,eipId:" + eipEntity.getEipId() + "dnatId:" + eipEntity.getDnatId() + "";
            log.error(msg);
        }

        if (delSnat(eipEntity.getSnatId(), eipEntity.getFirewallId())) {
            eipEntity.setSnatId(null);
        } else {
            returnStat = ReturnStatus.SC_FIREWALL_SNAT_UNAVAILABLE;
            msg += "Failed to del snat in firewall, eipId:" + eipEntity.getEipId() + "snatId:" + eipEntity.getSnatId() + "";
            log.error(msg);
        }
        String innerIp = eipEntity.getFloatingIp();
        boolean removeRet;
        if (eipEntity.getChargeMode().equalsIgnoreCase(HsConstants.SHAREDBANDWIDTH) && eipEntity.getPipId() != null) {
            removeRet = removeFloatingIpFromQos(eipEntity.getFirewallId(), innerIp, eipEntity.getPipId(), eipEntity.getSharedBandWidthId());
        } else {
            removeRet = delQos(eipEntity.getPipId(), eipEntity.getFirewallId());
            if (removeRet) {
                eipEntity.setPipId(null);
            }
        }
        if (!removeRet) {
            returnStat = ReturnStatus.SC_FIREWALL_QOS_UNAVAILABLE;
            msg += "Failed to del qos, eipId:" + eipEntity.getEipId() + " pipId:" + eipEntity.getPipId() + "";
            log.error(msg);
        }
        if (msg == null) {
            return MethodReturnUtil.success();
        } else {
            return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, returnStat, msg);
        }

    }

    /**
     * add the Qos bindind ip
     * @param firewallId id
     * @param sbwId      bad id
     * @return ret
     */
    public String addFloatingIPtoQos(String firewallId, String floatIp, String sbwPipeId, String sbwId, int iWidth) {
        log.info("Param : FirewallId:{}, floatIp:{}, sbwPipeId：{}，sbwId：{} ，iWidth:{}", firewallId, floatIp, sbwPipeId, sbwId, iWidth);
        Firewall fwBean = getFireWallById(firewallId);
        String retPipeId = null;
        if (fwBean != null) {
            qosService.setFwIp(fwBean.getIp());
            qosService.setFwPort(fwBean.getPort());
            qosService.setFwUser(fwBean.getUser());
            qosService.setFwPwd(fwBean.getPasswd());

            HashMap<String, String> map = qosService.insertIpToPipe(floatIp, sbwPipeId, sbwId);
            log.info("addFloatingIPtoQos:firewallId:{} fip:{} sbwId:{} reslut:{}", firewallId, floatIp, sbwId, map);
            if (map.get(HsConstants.SUCCESS) != null && Boolean.valueOf(map.get(HsConstants.SUCCESS))) {
                log.info("addFloatingIPtoQos: " + firewallId + "floatIp: " + floatIp + " --success==sbwId：" + sbwId);
                retPipeId = map.get("id");
            } else if (Boolean.valueOf(map.get(HsConstants.SUCCESS))) {
                log.warn("addFloatingIPtoQos: " + firewallId + HsConstants.FLOATIP + floatIp + " --fail==sbwId：" + sbwId);
            }
        }

        return retPipeId;
    }

    /**
     * remove eip from shared band
     * @param firewallId id
     * @param floatIp    fip
     * @param pipeId     bandid
     * @return ret
     */
    public boolean removeFloatingIpFromQos(String firewallId, String floatIp, String pipeId, String sbwId) {
        log.info("Param : FirewallId:{}, floatIp:{}, pipeId：{}，sbwId：{} ", firewallId, floatIp, pipeId);
        Firewall fwBean = getFireWallById(firewallId);
        if (fwBean != null) {
            qosService.setFwIp(fwBean.getIp());
            qosService.setFwPort(fwBean.getPort());
            qosService.setFwUser(fwBean.getUser());
            qosService.setFwPwd(fwBean.getPasswd());
            HashMap<String, String> map = qosService.removeIpFromPipe(floatIp, pipeId);
            if (Boolean.valueOf(map.get(HsConstants.SUCCESS))) {
                log.info("FirewallService : Success removeFloatingIpFromQos: " + firewallId + "floatIp: " + floatIp + " --success==pipeId：" + pipeId);
                return Boolean.parseBoolean(map.get(HsConstants.SUCCESS));
            }
            log.warn("FirewallService : Failed removeFloatingIpFromQos :floatIp pipeId:{} map:{} ", floatIp, pipeId, map);
        }
        return Boolean.parseBoolean("False");
    }

}
