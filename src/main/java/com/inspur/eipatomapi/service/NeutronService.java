package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.util.CommonUtil;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.builder.NetFloatingIPBuilder;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;

/**
 * @Auther: jiasirui
 * @Date: 2018/9/14 09:32
 * @Description:  the class support data of openstack rest api
 */

@Service
public  class NeutronService {

    private final Logger log = Logger.getLogger(NeutronService.class.getName());


    /**
     *  get the floatingip detail
     * @param id
     * @return NetFloatingIP entity
     */
    protected NetFloatingIP getFloatingIp(String id) throws Exception {
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        NetFloatingIP netFloatingIP = osClientV3.networking().floatingip().get(id);
        return netFloatingIP;
    }

    protected synchronized NetFloatingIP createFloatingIp(String region, String networkId, String portId) throws Exception   {

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
            log.warning(message);
            throw new ResponseException(message, 500);
        }

        return netFloatingIP;
    }

    protected synchronized Boolean deleteFloatingIp(String name, String eipId) throws Exception{
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        return osClientV3.networking().floatingip().delete(eipId).isSuccess();
    }

    protected synchronized NetFloatingIP associatePortWithFloatingIp(String netFloatingIpId, String portId) throws Exception  {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        return osClientV3.networking().floatingip().associateToPort(netFloatingIpId, portId);
    }

    protected synchronized NetFloatingIP disassociateFloatingIpFromPort( String netFloatingIpId) throws Exception {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        return osClientV3.networking().floatingip().disassociateFromPort(netFloatingIpId);
    }


    protected List<? extends NetFloatingIP> listFloatingIps() throws Exception{

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        //Map<String, String> filteringParams = new HashMap<>();
        //filteringParams.put("tenant_id",CommonUtil.getTokenInfo().getString("project"));
        return  osClientV3.networking().floatingip().list();
    }

}
