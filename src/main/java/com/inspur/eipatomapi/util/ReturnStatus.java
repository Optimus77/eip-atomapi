package com.inspur.eipatomapi.util;


public interface ReturnStatus {

    // --- 1xx Informational ---

    String SC_OK = "200";

    String SC_PARAM_ERROR       = "006.999400";
    String SC_PARAM_NOTFOUND    = "006.998400";
    String SC_PARAM_UNKONWERROR = "006.997400";
    String SC_RESOURCE_ERROR    = "006.994400";
    String SC_RESOURCE_NOTENOUGH = "006.993400";
    String EIP_BIND_HAS_BAND     ="006.991.400";

    String SC_NOT_FOUND                  = "006.994404";
    String SC_OPENSTACK_FIP_UNAVAILABLE  = "006.101404";
    String SC_FIREWALL_SNAT_UNAVAILABLE  = "006.202404";
    String SC_FIREWALL_DNAT_UNAVAILABLE  = "006.202404";
    String SC_FIREWALL_QOS_UNAVAILABLE   = "006.203404";
    String SC_NOT_SUPPORT                = "006.999405";


    String SC_FORBIDDEN="006.001403" ;

    String SC_INTERNAL_SERVER_ERROR   = "006.999500";
    String SC_OPENSTACK_UNAVAILABLE   = "006.999503";
    String SC_FIREWALL_UNAVAILABLE    = "006.999503";

    String SC_OPENSTACK_SERVER_ERROR  = "006.101503";
    String SC_OPENSTACK_FIPCREATE_ERROR  = "006.102503";
    String SC_FIREWALL_SERVER_ERROR   = "006.201503";

}
