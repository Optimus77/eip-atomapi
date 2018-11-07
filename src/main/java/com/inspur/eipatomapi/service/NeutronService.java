package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.util.CommonUtil;
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

/**
 * @Auther: jiasirui
 * @Date: 2018/9/14 09:32
 * @Description:  the class support data of openstack rest api
 */

@Service
public  class NeutronService {

    public final static Logger log = LoggerFactory.getLogger(NeutronService.class);

    /**
     *  get the floatingip detail
     * @param id   id
     * @return NetFloatingIP entity
     */
    public NetFloatingIP getFloatingIp(String id) throws Exception {
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        return osClientV3.networking().floatingip().get(id);
    }

    public synchronized NetFloatingIP createFloatingIp(String region, String networkId, String portId) throws Exception   {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        //osClientV3.networking().router().get().getExternalGatewayInfo().getNetworkId();
        NetFloatingIPBuilder builder = new NeutronFloatingIP.FloatingIPConcreteBuilder();
        builder.floatingNetworkId(networkId);
        if (null != portId) {
            builder.portId(portId);
        }
        NetFloatingIP netFloatingIP = osClientV3.networking().floatingip().create(builder.build());
        if (netFloatingIP != null) {
            log.info("Allocated Floating ip: " + netFloatingIP.getId());
        } else {
            String message = String.format(
                    "Cannot create floating ip under network: %s in region: %s",
                    networkId, region);
            log.error(message);
            throw new ResponseException(message, 500);
        }

        return netFloatingIP;
    }

    public synchronized Boolean deleteFloatingIp(String name, String eipId) throws Exception{
        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        return osClientV3.networking().floatingip().delete(eipId).isSuccess();
    }

    public synchronized ActionResponse associaInstanceWithFloatingIp(Eip eip, String serverId)
            throws Exception  {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        Server server = osClientV3.compute().servers().get(serverId);
        ActionResponse result =  osClientV3.compute().floatingIps().addFloatingIP(server, eip.getFloatingIp());

        if(result.isSuccess()){
            Map<String, List<? extends Address>> novaAddresses = server.getAddresses().getAddresses();
            log.info(novaAddresses.toString());
            Set<String> keySet =novaAddresses.keySet();
            for (String netname:keySet) {
                List<? extends Address> address=novaAddresses.get(netname);
                log.info(address.toString());
                for (Address addr : address) {
                    log.debug(server.getId() + server.getName() + "   " + addr.getType());
                    if (addr.getType().equals("fixed")) {
                        eip.setPrivateIpAddress(addr.getAddr());
                    }
                }
            }
        }else{
            log.error("openstack api return faild when bind instance to eip.");
        }
        return result;
    }

    public synchronized ActionResponse disassociateInstanceWithFloatingIp( String floatingIp, String serverId)
            throws Exception {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        Server server = osClientV3.compute().servers().get(serverId);
        if (server == null){
            log.info("Not found serverid",server);
        }
        log.info("get serverinfo  {}",server);
        return osClientV3.compute().floatingIps().removeFloatingIP(server, floatingIp);
    }


    public List<? extends NetFloatingIP> listFloatingIps() throws Exception{

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        //Map<String, String> filteringParams = new HashMap<>();
        //filteringParams.put("tenant_id",CommonUtil.getTokenInfo().getString("project"));
        return  osClientV3.networking().floatingip().list();
    }



    public List<? extends Server> listServer()throws Exception{

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();
        Map<String, String> filteringParams = new HashMap<>();
        filteringParams.put("tenant_id",CommonUtil.getProjectId());
        return osClientV3.compute().servers().list(filteringParams);
    }

    public synchronized NetFloatingIP associaPortWithFloatingIp(String floatingIpId, String portId)
            throws Exception  {

        OSClientV3 osClientV3 = CommonUtil.getOsClientV3Util();

        return osClientV3.networking().floatingip().associateToPort(floatingIpId, portId);
    }
}
