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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
public class FirewallService {

    @Autowired
    private FirewallRepository firewallRepository;

    @Autowired
    private QosService qosService;

    @Autowired
    private SbwRepository sbwRepository;

    @Autowired
    private EipRepository eipRepository;

    @Autowired
    private FireWallCommondService fireWallCommondService;

    //    @Value("${jasypt.password}")
    private String secretKey = "EbfYkitulv73I2p0mXI50JMXoaxZTKJ7";
    private Map<String, Firewall> firewallConfigMap = new HashMap<>();
    private String vr = "trust-vr";

    Firewall getFireWallById(String id) {
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
        String ruleid = cmdAddDnat(innerip, extip, equipid);
        if(ruleid != null){
            return ruleid;
        }
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

        String ruleid = cmdAddSnat(innerip, extip, equipid);
        if(ruleid != null){
            return ruleid;
        }

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
        String pipid ;
        String inBandWidth = "50";
        if(Integer.valueOf(bandwidth)>50) {
            inBandWidth = bandwidth;
        }
        pipid = cmdAddQos(name, innerip, inBandWidth, bandwidth,fireWallId);
        if(null != pipid){
            return pipid;
        }

        return pipid;
    }

    /**
     * update the Qos bindWidth
     * @param firewallId firewall id
     * @param bindwidth  bind width
     */
    boolean updateQosBandWidth(String firewallId, String pipId, String pipNmae, String bindwidth, String fip, String eip) {

        Firewall fwBean = getFireWallById(firewallId);
        if (fwBean != null) {
            if(null != fip && pipId.equals(getRootPipeName(fip))) {
                return cmdUpdateQosBandWidth(eip,fip,bindwidth, firewallId);
            }else if(pipId.length() == "9dea38f8-f59c-4847-ba43-f0ef61a6986c".length()){
                return cmdUpdateRootQosBandWidth(firewallId, pipNmae, bindwidth);
            }
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

    private boolean cmdUpdateRootQosBandWidth(String fireWallId, String pipNmae, String bandwidth) {

        String inBandWidth = "50";
        if(Integer.valueOf(bandwidth)>50) {
            inBandWidth = bandwidth;
        }
        String retString = "Root pipe \""+pipNmae+"\" is unavailable";
        String strResult = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "qos-engine first\r"
                        + "root-pipe  " + pipNmae + "\r"
                        + "pipe-rule forward bandwidth Mbps "+ inBandWidth+"\r"
                        + "pipe-rule backward bandwidth Mbps "+ bandwidth+"\r"
                        + "end",
                null);
        if (strResult == null) {
            return true;
        }
        log.error("Failed to update cmd qos", strResult);
        return false;
    }

    /**
     * del qos
     * @param pipid pipid
     * @param devId devid
     * @return ret
     */
    boolean delQos(String pipid, String eip, String fip, String devId) {
        if (StringUtils.isNotEmpty(pipid)) {
            if(null != eip && null != fip && pipid.equals(getRootPipeName(fip))){
                return cmdDelQos(pipid,eip,devId);
            }
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
        if(cmdDelDnat(ruleid, devId)){
            return true;
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
        if(cmdDelSnat(ruleid, devId)){
            return true;
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
            dnatRuleId = addDnat(fipAddress, eipAddress, firewallId);
            if (dnatRuleId != null) {
                snatRuleId = addSnat(fipAddress, eipAddress, firewallId);
                if (snatRuleId != null) {
                    if (eip.getChargeMode().equalsIgnoreCase(HsConstants.SHAREDBANDWIDTH)) {
                        Sbw sbwEntity = sbwRepository.findBySbwId(eip.getSbwId());
                        if (null != sbwEntity) {
                            pipId = addFloatingIPtoQos(eip.getFirewallId(), fipAddress, sbwEntity.getPipeId());
                        }
                    } else {
                        pipId = addQos(fipAddress, eipAddress, String.valueOf(bandWidth), firewallId);
                    }
                    if (null != pipId || CommonUtil.qosDebug) {
                        eip.setDnatId(dnatRuleId);
                        eip.setSnatId(snatRuleId);
                        eip.setPipId(pipId);
                        log.info("add nat and qos successfully. snat:{}, dnat:{}, qos:{}",
                                eip.getSnatId(), eip.getDnatId(), eip.getPipId());

                        return MethodReturnUtil.success(eip);
                    }else {
                        returnStat = ReturnStatus.SC_FIREWALL_QOS_UNAVAILABLE;
                        returnMsg = CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_QOS_ERROR);
                    }
                } else {
                    returnStat = ReturnStatus.SC_FIREWALL_SNAT_UNAVAILABLE;
                    returnMsg = CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_SNAT_ERROR);
                }
            } else {
                returnStat = ReturnStatus.SC_FIREWALL_DNAT_UNAVAILABLE;
                returnMsg = CodeInfo.getCodeMessage(CodeInfo.EIP_BIND_FIREWALL_DNAT_ERROR);
            }

        } catch (Exception e) {
            log.error("bind server exception", e);
            returnStat = ReturnStatus.SC_OPENSTACK_SERVER_ERROR;
            returnMsg = e.getMessage();
        } finally {
            if (null == pipId) {
                if (null != dnatRuleId) {
                    delDnat(dnatRuleId, eip.getFirewallId());
                }
                if (null != snatRuleId) {
                    delSnat(snatRuleId, eip.getFirewallId());
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
            removeRet = removeFloatingIpFromQos(eipEntity.getFirewallId(), innerIp, eipEntity.getPipId());
        } else {
            removeRet = delQos(eipEntity.getPipId(), eipEntity.getEipAddress(),innerIp, eipEntity.getFirewallId());
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
            eipEntity.setStatus(HsConstants.ERROR);
            return MethodReturnUtil.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, returnStat, msg);
        }

    }

    /**
     * add the Qos bindind ip
     * @param firewallId id
     * @return ret
     */
    public String addFloatingIPtoQos(String firewallId, String floatIp, String pipeId) {
        String retPipeId = null;
        if(pipeId.length() == "9dea38f8-f59c-4847-ba43-f0ef61a6986c".length()) {
            retPipeId = cmdAddIp2SbwPipe(pipeId, floatIp, firewallId);
            if (null != retPipeId) {
                return retPipeId;
            }
        }
        log.info("Param : FirewallId:{}, floatIp:{}, pipeId：{} ", firewallId, floatIp, pipeId);
        Firewall fwBean = getFireWallById(firewallId);
        if (fwBean != null) {
            qosService.setFwIp(fwBean.getIp());
            qosService.setFwPort(fwBean.getPort());
            qosService.setFwUser(fwBean.getUser());
            qosService.setFwPwd(fwBean.getPasswd());

            HashMap<String, String> map = qosService.insertIpToPipe(floatIp, pipeId);
            if (map.get(HsConstants.SUCCESS) != null && Boolean.valueOf(map.get(HsConstants.SUCCESS))) {
                log.info("addFloatingIPtoQos: " + firewallId + "floatIp: " + floatIp + " --success：");
                retPipeId = map.get("id");
            } else if (Boolean.valueOf(map.get(HsConstants.SUCCESS))) {
                log.warn("addFloatingIPtoQos: " + firewallId + HsConstants.FLOATIP + floatIp + " --fail" );
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
    public boolean removeFloatingIpFromQos(String firewallId, String floatIp, String pipeId) {
        log.info("Param : FirewallId:{}, floatIp:{}, pipeId：{} ", firewallId, floatIp, pipeId);
        if(pipeId.length() == "9dea38f8-f59c-4847-ba43-f0ef61a6986c".length()){
            return cmdDelIpInSbwPipe(pipeId, floatIp, firewallId);
        }


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


    public boolean ping(String ipAddress)  {
        try {
            int  timeOut =  3000 ;
            return InetAddress.getByName(ipAddress).isReachable(timeOut);
        }catch (Exception e){
            return false;
        }
    }

    private Boolean cmdDelSnat(String snatId, String fireWallId) {
        if (snatId != null) {
            String delResult = fireWallCommondService.execCustomCommand(fireWallId,
                    "configure\r"
                            + "ip vrouter trust-vr\r"
                            + "no Snatrule id " + snatId + "\r"
                            + "end",
                    null);
            if (delResult != null) {
                if (!delResult.contains("cannot be found")) {
                    return false;
                }
                log.error("Failed to delete snatId.");
            }
        }
        log.info("Cmd delete snat {} successfully", snatId);
        return true;
    }
    private Boolean cmdDelDnat(String dnatId, String fireWallId) {
        if(null != dnatId) {
            String delResult = fireWallCommondService.execCustomCommand(fireWallId,
                    "configure\r"
                            + "ip vrouter trust-vr\r"
                            + "no dnatrule id " + dnatId + "\r"
                            + "end",
                    null);
            if (delResult != null) {
                if (!delResult.contains("cannot be found")) {
                    return false;
                }
                log.error("Failed to delete dnatId.");
            }
        }
        log.info("Cmd delete dnat {} successfully", dnatId);
        return true;
    }



    private String cmdAddDnat(String fip, String eip, String fireWallId)  {

        String strDnatPtId = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "ip vrouter trust-vr\r"
                        + "dnatrule top from  Any to "+eip+" service Any trans-to "+ fip+ "\r"
                        + "end",
                "rule ID=");
        if(strDnatPtId == null){
            log.error("Failed to add dnat", strDnatPtId);
            return null;
        }
        return strDnatPtId.split("=")[1].trim();
    }

    private String cmdAddSnat(String fip, String eip, String fireWallId)  {

        String strDnatPtId = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "ip vrouter trust-vr\r"
                        + "snatrule top from " +fip+  " to Any service Any trans-to "  +eip+ " mode static\r"
                        + "end",
                "rule ID=");
        if(strDnatPtId == null){
            log.error("Failed to add snat", strDnatPtId);
            return null;
        }
        return strDnatPtId.split("=")[1].trim();
    }


    public Boolean cmdDelQos(String rootPipeName,String eip, String fireWallId)  {
        String ret = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "qos-engine first\r"
                        + "root-pipe  " + rootPipeName + "\r"
                        + "no pipe  " + eip + "\r"
                        + "end",
                "^-----");
        if(ret == null){
            log.info("Del qos by cmd successfully:");
            return true;
        }
        if(ret.contains("^-----")){
            log.error("Can not find {} in {}", eip ,rootPipeName);
            return true;
        }
        log.error("Failed to del qos by cmd:{}", ret);
        return false;
    }

    private String cmdAddQos(String eip, String fip, String inboundBandwidth,String outboundBandwidth, String fireWallId)  {
        if(null == fip || null == eip){
            return null;
        }
        String rootPipeNmae = getRootPipeName(fip);
        if(0 >= eipRepository.countByPipId(rootPipeNmae)){
            boolean result = cmdAddRootPipe(rootPipeNmae,eip, fip, inboundBandwidth, outboundBandwidth,fireWallId);
            log.info("Add root-pipe {}, result:{}", rootPipeNmae, result);
            if(result){
                return rootPipeNmae;
            }
            return null;
        }
        String retString = "Tip: Pipe \""+eip+"\" is enabled";
        String strResult = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "qos-engine first\r"
                        + "root-pipe  " + rootPipeNmae + "\r"
                        + "pipe  " + eip + "\r"
                        + "pipe-map \r"
                        + "dst-ip " + fip + "/32\r"
                        + "exit\r"
                        + "pipe-rule forward reserve-bandwidth Mbps 1 max Mbps "+ inboundBandwidth+"\r"
                        + "pipe-rule backward reserve-bandwidth Mbps 1 max Mbps "+ outboundBandwidth+"\r"
                        + "end",
                retString);
        if(strResult == null || !strResult.contains(retString)){
            log.error("Failed to add cmd qos", strResult);
           return null;
        }
        return rootPipeNmae;
    }
    boolean cmdUpdateQosBandWidth(String eip, String fip, String bandwidth, String fireWallId){

        String retString = "Tip: Pipe \""+eip+"\" is enabled";
        String strResult = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "qos-engine first\r"
                        + "root-pipe  " + getRootPipeName(fip) + "\r"
                        + "pipe  " + eip + "\r"
                        + "pipe-rule forward reserve-bandwidth Mbps 1 max Mbps "+ bandwidth+"\r"
                        + "pipe-rule backward reserve-bandwidth Mbps 1 max Mbps "+ bandwidth+"\r"
                        + "end",
                retString);
        if(strResult == null || !strResult.contains(retString)){
            log.error("Failed to update cmd qos", strResult);
            return false;
        }
        return true;
    }
    private String getRootPipeName(String fip){
        String[] ipSplit = fip.split("\\.");
        return ipSplit[0]+"."+ ipSplit[1]+"."+ ipSplit[2]+".0";
    }

    private boolean cmdAddRootPipe(String rootPipeName, String eip, String fip, String inBwd, String outBwd, String fireWallId)  {

        String retString = "Tip: Pipe \""+eip+"\" is enabled";
        String strResult = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "qos-engine first\r"
                        + "root-pipe  " + rootPipeName + "\r"
                        + "pipe-map \r"
                        + "dst-ip " + fip + "/24\r"
                        + "src-addr Any\r"
                        + "service Any\r "
                        + "exit\r"
                        + "pipe-rule forward bandwidth Gbps 1\r"
                        + "pipe-rule backward bandwidth Gbps 1\r"
                        + "pipe  " + eip + "\r"
                        + "pipe-map \r"
                        + "dst-ip " + fip + "/32\r"
                        + "exit\r"
                        + "pipe-rule forward reserve-bandwidth Mbps 1 max Mbps "+ inBwd+"\r"
                        + "pipe-rule backward reserve-bandwidth Mbps 1 max Mbps "+ outBwd+"\r"
                        + "end",
                retString);
        if(strResult == null || !strResult.contains(retString)){
            log.error("Failed to add cmd root qos", strResult);
            return false;
        }
        return true;
    }
    String cmdAddSbwQos(String name, String bandwidth, String fireWallId)  {
        String inBandWidth = "50";
        if(Integer.valueOf(bandwidth)>50) {
            inBandWidth = bandwidth;
        }
        String retString = "Root pipe \""+name+"\" is unavailable";
        String strResult = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "qos-engine first\r"
                        + "root-pipe  " + name + "\r"
                        + "sequence 1\r"
                        + "pipe-rule forward bandwidth Mbps "+ inBandWidth+"\r"
                        + "pipe-rule backward bandwidth Mbps "+ bandwidth+"\r"
                        + "end",
                retString);
        if(strResult == null || !strResult.contains(retString)){
            log.error("Failed to add cmd sbw qos", strResult);
            return null;
        }
        return name;
    }
    boolean cmdDelSbwQos(String name, String fireWallId)  {

        String strResult = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "qos-engine first\r"
                        + "no root-pipe  " + name + "\r"
                        + "end",
                null);
        if(strResult == null){
            return true;
        }
        log.error("Failed to del cmd sbw qos", strResult);
        return false;
    }
    private String cmdAddIp2SbwPipe(String rootPipeName, String fip, String fireWallId)  {


        String retCheck = "unrecognized keyword 1";
        String pipeMapId = "1";
        String strResult = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "qos-engine first\r"
                        + "root-pipe  " + rootPipeName + "\r"
                        + "pipe-map "+pipeMapId+"\r"
                        + "end",
                        retCheck);
        if(strResult != null && strResult.contains(retCheck)){
            pipeMapId = "";
        }

        String addResult = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "qos-engine first\r"
                        + "root-pipe  " + rootPipeName + "\r"
                        + "pipe-map "+pipeMapId+"\r"
                        + "dst-ip " + fip + "/32\r"
                        + "end",
                        null);
        if(addResult == null){
            return rootPipeName;
        }
        log.error("Failed to add cmd qos", strResult);
        return null;
    }
    private boolean cmdDelIpInSbwPipe(String rootPipeName,String fip, String fireWallId)  {

        String strResult = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                        + "qos-engine first\r"
                        + "root-pipe  " + rootPipeName + "\r"
                        + "pipe-map 1\r"
                        + "no dst-ip " + fip + "/32\r"
                        + "end",
                null);
        if(strResult != null){
            log.error("Failed to add cmd qos", strResult);
            return false;
        }
        return true;
    }
}
