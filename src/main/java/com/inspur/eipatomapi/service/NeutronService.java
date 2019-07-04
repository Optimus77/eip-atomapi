package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.KeycloakTokenException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.options.PortListOptions;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.builder.NetFloatingIPBuilder;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public  class NeutronService {

    @Autowired
    private  SlbService slbService;

    /**
     * 创建FloatingIp
     * @param region
     * @param networkId
     * @param portId
     * @return
     * @throws KeycloakTokenException
     */
    public synchronized NetFloatingIP createFloatingIp(String region, String networkId, String portId) throws KeycloakTokenException {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);

        NetFloatingIP netFloatingIP = getFloatingIpAddrByPortId(osClientV3, portId);
        if(null != netFloatingIP){
            return netFloatingIP;
        }

        NetFloatingIPBuilder builder = new NeutronFloatingIP.FloatingIPConcreteBuilder();
        builder.floatingNetworkId(networkId);
        if (null != portId) {
            builder.portId(portId);
        }
        netFloatingIP = osClientV3.networking().floatingip().create(builder.build());
        if (netFloatingIP != null) {
            log.info("Allocated Floating ip: {}",netFloatingIP.getId());
        } else {
            String message = String.format(
                    "Cannot create floating ip under network: %s in region: %s",
                    networkId, region);
            log.error(message);
            throw new ResponseException(message, 500);
        }

        return netFloatingIP;
    }

    /**
     * 删除FloatingIp
     * @param region
     * @param fipId
     * @param instanceId
     * @return
     * @throws KeycloakTokenException
     */
    synchronized Boolean deleteFloatingIp(String region, String fipId, String instanceId) throws KeycloakTokenException {

        if (slbService.isFipInUse(instanceId)) {
            return true;
        }

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
        return osClientV3.networking().floatingip().delete(fipId).isSuccess();
    }
    /**
     * 超级管理员删除FloatingIp
     * @param fipId
     * @param instanceId
     * @return
     * @throws KeycloakTokenException
     */
    synchronized Boolean superDeleteFloatingIp( String fipId, String instanceId) {

        if (slbService.isFipInUse(instanceId)) {
            return true;
        }

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3();
        return osClientV3.networking().floatingip().delete(fipId).isSuccess();
    }
    /**
     * 创建并且关联FloatingIp
     * @param region
     * @param networkId
     * @param portId
     * @param eip
     * @param serverId
     * @return
     * @throws KeycloakTokenException
     */
    synchronized  NetFloatingIP createAndAssociateWithFip(String region, String networkId, String portId,
                                                          Eip eip, String serverId) throws KeycloakTokenException {

        if(portId.isEmpty()){
            log.error("Port id is null when bind instance with eip. server:{}, eip:{}", serverId, eip.getId());
            return null;
        }
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);

        Port port = osClientV3.networking().port().get(portId);
        if(null == port) {
            log.error("Can not get port by id:{} in associate with server:{}, eipId:{}",portId, serverId, eip.getId());
            return null;
        }

        Server server = osClientV3.compute().servers().get(serverId);
        if(null == server) {
            log.error("Can not get server when associate with serverId:{}, eipId:{}", serverId, eip.getId());
            return null;
        }
        String projectId = CommonUtil.getProjectId(region, osClientV3);
        if (!(projectId.equalsIgnoreCase(port.getTenantId())) || !(projectId.equalsIgnoreCase(server.getTenantId()))) {
            log.error("Port eip and server is not in the same project.UserPorjectId:{}, id in port:{}, id in server:{}",
                    projectId, port.getTenantId(), server.getTenantId());
            return null;
        }
        NetFloatingIP netFloatingIP = getFloatingIpAddrByPortId(osClientV3, portId);
        if(null != netFloatingIP){
            Set<? extends IP> fixedIps = port.getFixedIps();
            for (IP fixedIp : fixedIps) {
                eip.setPrivateIpAddress(fixedIp.getIpAddress());
            }
            return netFloatingIP;
        }

        NetFloatingIPBuilder builder = new NeutronFloatingIP.FloatingIPConcreteBuilder();
        builder.floatingNetworkId(networkId);
        builder.portId(portId);

        netFloatingIP = osClientV3.networking().floatingip().create(builder.build());
        if (netFloatingIP != null) {
            log.info("Allocated Floating ip: {}, id:{}",netFloatingIP.getFloatingIpAddress(), netFloatingIP.getId());
        } else {
            String message = String.format(
                    "Cannot create floating ip under network: %s in region: %s",
                    networkId, region);
            log.error(message);
            throw new ResponseException(message, 500);
        }
        Set<? extends IP> fixedIps = port.getFixedIps();
        for (IP fixedIp : fixedIps) {
            eip.setPrivateIpAddress(fixedIp.getIpAddress());
        }
        return netFloatingIP;
    }

    /**
     * 关联实例和floatingIp
     * @param eip
     * @param serverId
     * @param portId
     * @return
     * @throws KeycloakTokenException
     */
    synchronized ActionResponse associaInstanceWithFloatingIp(Eip eip, String serverId,
                                                                     String portId) throws KeycloakTokenException {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(eip.getRegion());
        NetFloatingIP netFloatingIP = getFloatingIpAddrByPortId(osClientV3, portId);
        if(null != netFloatingIP){
            return ActionResponse.actionSuccess();
        }

        Server server = osClientV3.compute().servers().get(serverId);
        if(null == server) {
            return ActionResponse.actionFailed("Can not find server by id"+ serverId, HttpStatus.SC_NOT_FOUND);
        }
        ActionResponse result = osClientV3.compute().floatingIps().addFloatingIP(server, eip.getFloatingIp());

        if (result.isSuccess()) {
            Map<String, List<? extends Address>> novaAddresses = server.getAddresses().getAddresses();
            log.info(novaAddresses.toString());
            Set<String> keySet = novaAddresses.keySet();
            for (String netname : keySet) {
                List<? extends Address> address = novaAddresses.get(netname);
                log.info(address.toString());
                for (Address addr : address) {
                    log.debug(server.getId() + server.getName() + "   " + addr.getType());
                    if (addr.getType().equals("fixed")) {
                        eip.setPrivateIpAddress(addr.getAddr());
                    }
                }
            }
        } else {
            log.error("openstack api return faild when bind instance to eip.");
        }

        return result;
    }

    /**
     * 分离实例和FloatingIp
     * @param floatingIp
     * @param serverId
     * @param region
     * @return
     * @throws KeycloakTokenException
     */
    public synchronized ActionResponse disassociateInstanceWithFloatingIp(String floatingIp, String serverId,
                                                                          String region) throws KeycloakTokenException {

        if(slbService.isFipInUse(serverId)){
            return ActionResponse.actionSuccess();
        }
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
        Server server = osClientV3.compute().servers().get(serverId);
        if (server == null) {
            log.info("Can not found serverid:{}", server);
        }

        return osClientV3.compute().floatingIps().removeFloatingIP(server, floatingIp);
    }

    synchronized ActionResponse disassociateAndDeleteFloatingIp(String floatingIp, String fipId, String serverId,
                                                                String region) throws KeycloakTokenException {

        if(null == serverId || null == fipId || slbService.isFipInUse(serverId)){
            return ActionResponse.actionSuccess();
        }

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
        Server server = osClientV3.compute().servers().get(serverId);
        if (server == null) {
            log.info("Can not found serverid:{}", serverId);
        }else {
            ActionResponse actionResponse = osClientV3.compute().floatingIps().removeFloatingIP(server, floatingIp);
            log.info("Remove fip from server: serverid:{}, fip:{}, result:{}",
                    serverId, floatingIp, actionResponse.toString());
        }

        boolean result = osClientV3.networking().floatingip().delete(fipId).isSuccess();
        log.info("disassociate and delete fip:{}, fipid:{}, serverid:{}, deleteResult:{}",
                floatingIp, fipId, serverId, result);

        return  ActionResponse.actionSuccess();
    }

    public List<? extends Server> listServer(String region) throws KeycloakTokenException {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
        Map<String, String> filteringParams = new HashMap<>();
        filteringParams.put("tenant_id", CommonUtil.getProjectId(region, osClientV3));
        return osClientV3.compute().servers().list(filteringParams);
    }

    /**
     * 关联port和floatingIp
     * @param floatingIpId
     * @param portId
     * @param region
     * @return
     * @throws KeycloakTokenException
     */
    public synchronized NetFloatingIP associaPortWithFloatingIp(String floatingIpId, String portId, String region) throws KeycloakTokenException {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);

        return osClientV3.networking().floatingip().associateToPort(floatingIpId, portId);
    }


    public NetFloatingIP getFloatingIpAddrByPortId(String serverPortId,String region ) throws KeycloakTokenException {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
        Map<String, String> filteringParams = new HashMap<>(4);
        filteringParams.put("port_id", serverPortId);
        List<? extends NetFloatingIP> list = osClientV3.networking().floatingip().list(filteringParams);
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    public synchronized String getserverIpByServerId(Eip eip, String serverId) throws KeycloakTokenException {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(eip.getRegion());
        Server server = osClientV3.compute().servers().get(serverId);
        Map<String, List<? extends Address>> novaAddresses = server.getAddresses().getAddresses();
        log.info(novaAddresses.toString());
        Set<String> keySet = novaAddresses.keySet();
        for (String netname : keySet) {
            List<? extends Address> address = novaAddresses.get(netname);
            log.info(address.toString());
            for (Address addr : address) {
                log.debug(server.getId() + server.getName() + "   " + addr.getType());
                if (addr.getType().equals("fixed")) {
                    eip.setPrivateIpAddress(addr.getAddr());
                }
            }
        }
        return eip.getPrivateIpAddress();
    }

    /**
     * 通过portId获取FloatingIp 地址
     * @return
     * @throws KeycloakTokenException
     */
    private NetFloatingIP getFloatingIpAddrByPortId(OSClientV3 osClientV3, String portId) {

        Map<String, String> filteringParams = new HashMap<>(4);
        filteringParams.put("port_id", portId);
        List<? extends NetFloatingIP> list = osClientV3.networking().floatingip().list(filteringParams);
        if (list.isEmpty()) {
            Port port = osClientV3.networking().port().get(portId);
            if (port == null) {
                String msg = String.format("Can not find this port, port_id : %s", portId);
                log.info(msg);
            }
            return null;
        }
        log.info("Get fip for port:{}, fip:{}", portId, list.get(0).getFloatingIpAddress());
        return list.get(0);
    }

    public List<String> getPortIdByServerId( String serverId, OSClientV3 osClientV3) {

        List<? extends Port> list = osClientV3.networking().port().list(PortListOptions.create().deviceId(serverId));
        List<String> ports = new ArrayList<>();
        if (!list.isEmpty()) {
            for (Port port : list) {
                log.debug("Get portId for server:{}, portIs:{}", serverId, list.get(0).getId());
                ports.add(port.getId());
            }
        }
        return ports;
    }
}
