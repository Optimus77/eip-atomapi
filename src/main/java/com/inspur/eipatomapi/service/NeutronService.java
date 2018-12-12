package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.KeycloakTokenException;
import org.apache.http.HttpStatus;
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.options.PortListOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public  class NeutronService {

    @Autowired
    private  SlbService slbService;
    public final static Logger log = LoggerFactory.getLogger(NeutronService.class);


    public synchronized NetFloatingIP createFloatingIp(String region, String networkId, String portId) throws Exception {

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

    public synchronized Boolean deleteFloatingIp(String region, String fipId, String instanceId) throws Exception{
        if(slbService.isFipInUse(instanceId)){
            return true;
        }
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
        return osClientV3.networking().floatingip().delete(fipId).isSuccess();
    }

    public  synchronized  NetFloatingIP createAndAssociateWithFip(String region, String networkId, String portId,
                                                                   Eip eip, String serverId) throws  Exception{
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


        Server server = osClientV3.compute().servers().get(serverId);
        if(null == server) {
            throw new ResponseException("Can not find server.", 500);
        }
        ActionResponse result = osClientV3.compute().floatingIps().addFloatingIP(server, netFloatingIP.getId());

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

        return netFloatingIP;
    }

    public synchronized ActionResponse associaInstanceWithFloatingIp(Eip eip, String serverId, String networkId,
                                                                     String portId, String region) throws Exception  {

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

    public synchronized ActionResponse disassociateInstanceWithFloatingIp(String floatingIp, String serverId,
                                                                          String region) throws Exception {

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

    public synchronized ActionResponse disassociateAndDeleteFloatingIp(String floatingIp, String fipId, String serverId,
                                                                          String region) throws Exception {

        if(slbService.isFipInUse(serverId)){
            return ActionResponse.actionSuccess();
        }
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
        Server server = osClientV3.compute().servers().get(serverId);
        if (server == null) {
            log.info("Can not found serverid:{}", server);
        }

        ActionResponse actionResponse = osClientV3.compute().floatingIps().removeFloatingIP(server, floatingIp);
        boolean result = false;
        if(actionResponse.isSuccess()) {
            result = osClientV3.networking().floatingip().delete(fipId).isSuccess();
        }
        log.info("disassociate and delete fip:{}, fipid:{}, serverid:{} result:{}",
                floatingIp, fipId, serverId, result);
        return actionResponse;
    }

    public List<? extends Server> listServer(String region) throws Exception {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
        Map<String, String> filteringParams = new HashMap<>();
        filteringParams.put("tenant_id", CommonUtil.getProjectId(region));
        return osClientV3.compute().servers().list(filteringParams);
    }

    public synchronized NetFloatingIP associaPortWithFloatingIp(String floatingIpId, String portId, String region)
            throws Exception {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);

        return osClientV3.networking().floatingip().associateToPort(floatingIpId, portId);
    }

    public NetFloatingIP getFloatingIpAddrByPortId(String serverPortId,String region ) throws Exception {

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

    public synchronized String getserverIpByServerId(Eip eip, String serverId) throws Exception {

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
}
