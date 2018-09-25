package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.*;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.service.impl.NatServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Service
public class FirewallService {

    private final static Log log = LogFactory.getLog(FirewallService.class);
    @Autowired
    private FirewallRepository firewallRepository;

    private Firewall getFireWallById(String id){
        Firewall fireWallEntity = null;
        Optional<Firewall> firewall = firewallRepository.findById(id);
        if(firewall.isPresent()){
            fireWallEntity =  firewall.get();
        } else {
            log.warn("Failed to find the firewall by id:"+ id);
        }
        return fireWallEntity;
    }

    public String addDnat(String innerip, String extip, String equipid) {
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
            dnatVo.setVrid("trust-vr");
            dnatVo.setVrname("trust-vr");
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

            NatServiceImpl dnatimpl = new NatServiceImpl();
            FwResponseBody body = dnatimpl.addPDnat(dnatVo);
            if (body.isSuccess()) {
                // 创建成功
                FwPortMapResult result = (FwPortMapResult) body.getObject();
                ruleid = result.getRule_id();
                log.info(innerip + "--DNAT添加成功");
            } else {
                log.info(innerip + "--DNAT添加失败:" + body.getException());
            }
        }
        return ruleid;
    }

    public String addSnat(String innerip, String extip, String equipid) {
        String ruleid = null;
        //String srcIP = innerip;
        //String destIP = extip;

        FwSnatVo vo = new FwSnatVo();
        Firewall accessFirewallBeanByNeid = getFireWallById(equipid);
        if(accessFirewallBeanByNeid != null) {
            vo.setManageIP(accessFirewallBeanByNeid.getIp());
            vo.setManagePort(accessFirewallBeanByNeid.getPort());
            vo.setManageUser(accessFirewallBeanByNeid.getUser());
            vo.setManagePwd(accessFirewallBeanByNeid.getPasswd());

            vo.setVrid("trust-vr");
            vo.setSnatstat("1");
            vo.setFlag("20");
            vo.setSaddr(innerip);  //内网IP地址
            vo.setSaddrtype("1");
            vo.setHa("0");
            vo.setSnatlog("false");
            //vo.setPos_flag("0"); // 列表最后
            vo.setPos_flag("1");   // 列表最前
            vo.setSnatid("0");
            vo.setServicename("Any");
            //vo.setDaddr("21.21.21.21/25");
            vo.setDaddr("Any");
            vo.setDaddrtype("1");
            vo.setTransferaddr(extip); // 外网IP地址

            vo.setFlag("1");

            NatServiceImpl dnatimpl = new NatServiceImpl();
            FwResponseBody body = dnatimpl.addPSnat(vo);
            if (body.isSuccess()) {
                // 创建成功
                FwSnatVo result = (FwSnatVo) body.getObject();
                ruleid = result.getSnatid();
                log.info(innerip + "--SNAT添加成功");
            } else {
                log.info(innerip + "--SNAT添加失败:" + body.getException());
            }
        }
        return ruleid;
    }



    public String addQos(String innerip, String eipid, String bandwidth, String equipid) {
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
            if ("true".equals(res.get("success"))) {
                pipid = res.get("id");
                //添加管道成功，更新数据库
                if (StringUtils.isBlank(pipid)) {
                    Map<String, String> idmap = qs.getQosPipeId(eipid);
                    pipid = idmap.get("id");
                }
                log.info("QOS添加成功");
            } else {
                log.warn("QOS添加失败");
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
    public boolean updateQosBandWidth(String firewallId,String pipId, String pipNmae,String bindwidth){

        Firewall fwBean = getFireWallById(firewallId);
        if(fwBean != null) {
            QosService qs = new QosService(fwBean.getIp(), fwBean.getPort(), fwBean.getUser(), fwBean.getPasswd());
            HashMap<String, String> result = qs.updateQosPipe(pipId, pipNmae, bindwidth);
            log.info(result.toString());
            String successTag = "true";
            if (result.get("success").equals(successTag)) {
                log.info("updateQosBandWidth: " + firewallId + " --success==bindwidth：" + bindwidth);
            } else {
                log.info("updateQosBandWidth: " + firewallId + " --fail==bindwidth：" + bindwidth);
            }
            return Boolean.parseBoolean(result.get("success"));
        }
        return Boolean.parseBoolean("False");
    }


    /**
     * 删除管道
     */
    public boolean delQos(String pipid, String devId) {
        if (StringUtils.isNotEmpty(pipid)) {
            Firewall fwBean = getFireWallById(devId);
            if(null != fwBean) {
                QosService qs = new QosService(fwBean.getIp(), fwBean.getPort(), fwBean.getUser(), fwBean.getPasswd());
                qs.delQosPipe(pipid);
            } else {
                log.info("删除管道失败:"+"dev【"+devId+"】,pipid【"+pipid+"】");
            }
            //Todo: update eip entry
            //eipMapper.updateEipByObjectid(eipid, "");
        }

        return true;
    }

    public boolean delDnat(String ruleid, String devId) {
        boolean bSuccess = false;
        if ("offline".equals(ruleid)) {
            // 离线模式
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
                vo.setVrid("trust-vr");
                vo.setVrname("trust-vr");

                NatServiceImpl dnatimpl = new NatServiceImpl();
                FwResponseBody body = dnatimpl.delPDnat(vo);
                if (body.isSuccess() || (body.getException().getMessage().contains("cannot be found"))) {
                    // 删除成功
                    bSuccess = true;
                } else {
                    bSuccess = false;
                    log.warn("删除DNAT失败:" + "设备【" + devId + "】,ruleid【" + ruleid + "】");
                }
            }
        }
        return bSuccess;
    }
    public boolean delSnat(String ruleid, String devId) {
        boolean bSuccess = false;
        if ("offline".equals(ruleid)) {
            // 离线模式
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

                //vo.setManageIP("172.23.70.133");
                vo.setVrid("trust-vr");
                vo.setSnatid(ruleid);

                NatServiceImpl dnatimpl = new NatServiceImpl();
                FwResponseBody body = dnatimpl.delPSnat(vo);

                if (body.isSuccess() || (body.getException().getMessage().contains("cannot be found"))) {
                    // 删除成功
                    bSuccess = true;
                } else {
                    bSuccess = false;
                    log.info("删除SDNAT失败:" + "dev【" + devId + "】,ruleid【" + ruleid + "】");
                }
            }
        }
        return bSuccess;
    }

}
