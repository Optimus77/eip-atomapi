package com.inspur.eipatomapi.service;

import com.inspur.eip.util.CommonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.builder.NetFloatingIPBuilder;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @Auther: jiasirui
 * @Date: 2018/9/14 09:32
 * @Description:  the class support data of openstack rest api
 */

@Service
public  class NeutronService {

    private final static Log log = LogFactory.getLog(NeutronService.class);


    /**
     *  get the floatingip detail
     * @param id   id
     * @return NetFloatingIP entity
     */
    NetFloatingIP getFloatingIp(String id) throws Exception {
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        return osClientV3.networking().floatingip().get(id);
    }

    synchronized NetFloatingIP createFloatingIp(String region, String networkId, String portId) throws Exception   {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        //osClientV3.networking().router().get().getExternalGatewayInfo().getNetworkId();
        NetFloatingIPBuilder builder = new NeutronFloatingIP.FloatingIPConcreteBuilder();
        builder.floatingNetworkId(networkId);
        if (null != portId) {
            builder.portId(portId);
        }
        NetFloatingIP netFloatingIP = osClientV3.networking().floatingip().create(builder.build());
        if (netFloatingIP != null) {
            log.info("Allocated Floating ip: " + netFloatingIP.getId() + " To server with Id: ");
        } else {
            String message = String.format(
                    "Cannot create floating ip under network: %s in region: %s",
                    networkId, region);
            log.warn(message);
            throw new ResponseException(message, 500);
        }

        return netFloatingIP;
    }

    synchronized Boolean deleteFloatingIp(String name, String eipId) throws Exception{
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        return osClientV3.networking().floatingip().delete(eipId).isSuccess();
    }

    synchronized NetFloatingIP associatePortWithFloatingIp(String netFloatingIpId, String portId) throws Exception  {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        return osClientV3.networking().floatingip().associateToPort(netFloatingIpId, portId);
    }

    synchronized NetFloatingIP disassociateFloatingIpFromPort( String netFloatingIpId) throws Exception {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        return osClientV3.networking().floatingip().disassociateFromPort(netFloatingIpId);
    }


    List<? extends NetFloatingIP> listFloatingIps() throws Exception{

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        //Map<String, String> filteringParams = new HashMap<>();
        //filteringParams.put("tenant_id",CommonUtil.getTokenInfo().getString("project"));
        return  osClientV3.networking().floatingip().list();
    }

}
