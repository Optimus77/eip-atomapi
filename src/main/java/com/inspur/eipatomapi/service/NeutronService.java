package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.util.CommonUtil;
import com.inspur.eipatomapi.util.KeycloakTokenException;
import org.apache.http.HttpStatus;
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
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public  class NeutronService {

    public final static Logger log = LoggerFactory.getLogger(NeutronService.class);


    public synchronized NetFloatingIP createFloatingIp(String region, String networkId, String portId) throws Exception {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);

        NetFloatingIPBuilder builder = new NeutronFloatingIP.FloatingIPConcreteBuilder();
        builder.floatingNetworkId(networkId);
//        if (null != portId) {
//            builder.portId(portId);
//        }
        NetFloatingIP netFloatingIP = osClientV3.networking().floatingip().create(builder.build());
        if (netFloatingIP != null) {
            log.info("Allocated Floating ip: {}", netFloatingIP.getId());
        } else {
            String message = String.format(
                    "Cannot create floating ip under network: %s in region: %s",
                    networkId, region);
            log.error(message);
            throw new ResponseException(message, 500);
        }

        return netFloatingIP;
    }

    public synchronized Boolean deleteFloatingIp(String region, String eipId) throws Exception {
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
        return osClientV3.networking().floatingip().delete(eipId).isSuccess();
    }

    public synchronized ActionResponse associaInstanceWithFloatingIp(Eip eip, String serverId)
            throws Exception {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(eip.getRegion());
        Server server = osClientV3.compute().servers().get(serverId);
        if (null == server) {
            return ActionResponse.actionFailed("Can not find server by id" + serverId, HttpStatus.SC_NOT_FOUND);
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

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util(region);
        Server server = osClientV3.compute().servers().get(serverId);
        if (server == null) {
            log.info("Can not found serverid:{}", server);
        }

        return osClientV3.compute().floatingIps().removeFloatingIP(server, floatingIp);
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

    public String getserverPortIdByIpAddr(String serverId, String serverIp, String region) {
        String serverPortId = null;
        OSClientV3 osClientV3 = null;
        try {
            osClientV3 = CommonUtil.getOsClientV3Util(region);
        } catch (KeycloakTokenException e) {
            e.printStackTrace();
        }
        PortListOptions options = PortListOptions.create().deviceId(serverId);
        List<? extends Port> ports = osClientV3.networking().port().list(options);
        for (Port port : ports) {
            Set<? extends IP> fixedIps = port.getFixedIps();
            long count = fixedIps.stream()
                    .filter(ip -> serverIp.equals(ip.getIpAddress()))
                    .count();
            if (count > 0) {
                return port.getId();
            }
        }
        return serverPortId;
    }

}
