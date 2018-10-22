package com.inspur.eipatomapi.util;


public interface ReturnStatus {

    // --- 1xx Informational ---

    String SC_OK = "200";

    String SC_PARAM_ERROR       = "106.999400";
    String SC_PARAM_NOTFOUND    = "106.998400";
    String SC_PARAM_UNKONWERROR = "106.997400";
    String SC_RESOURCE_ERROR    = "106.994400";
    String SC_RESOURCE_NOTENOUGH = "106.993400";
    String EIP_BIND_HAS_BAND     ="106.991.400";

    String SC_NOT_FOUND                  = "106.994404";
    String SC_OPENSTACK_FIP_UNAVAILABLE  = "106.101404";
    String SC_FIREWALL_SNAT_UNAVAILABLE  = "106.202404";
    String SC_FIREWALL_DNAT_UNAVAILABLE  = "106.202404";
    String SC_FIREWALL_QOS_UNAVAILABLE   = "106.203404";
    String SC_NOT_SUPPORT                = "106.999405";


    String SC_FORBIDDEN="106.001403" ;

    String SC_INTERNAL_SERVER_ERROR   = "106.999500";
    String SC_OPENSTACK_UNAVAILABLE   = "106.999503";
    String SC_FIREWALL_UNAVAILABLE    = "106.999503";

    String SC_OPENSTACK_SERVER_ERROR  = "106.101503";
    String SC_OPENSTACK_FIPCREATE_ERROR  = "106.102503";
    String SC_FIREWALL_SERVER_ERROR   = "106.201503";

}
