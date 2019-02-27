package com.inspur.eipatomapi.util;


public class ReturnStatus {
    public static final String SC_OK = "200";

    public static final String SC_PARAM_ERROR       = "106.999400";
    public static final String SC_PARAM_NOTFOUND    = "106.998400";
    public static final String SC_PARAM_UNKONWERROR = "106.997400";
    public static final String SC_RESOURCE_ERROR    = "106.994400";
    public static final String SC_RESOURCE_NOTENOUGH = "106.993400";
    public static final String EIP_BIND_HAS_BAND     ="106.991400";

    public static final String SC_NOT_FOUND                  = "106.994404";
    public static final String SC_OPENSTACK_FIP_UNAVAILABLE  = "106.101404";
    public static final String SC_FIREWALL_SNAT_UNAVAILABLE  = "106.202404";
    public static final String SC_FIREWALL_DNAT_UNAVAILABLE  = "106.202404";
    public static final String SC_FIREWALL_QOS_UNAVAILABLE   = "106.203404";
    public static final String SC_NOT_SUPPORT                = "106.999405";
    public static final String SC_FIREWALL_NAT_UNAVAILABLE   = "106.202404";


    public static final String SC_FORBIDDEN="106.001403" ;

    public static final String SC_INTERNAL_SERVER_ERROR   = "106.999500";
    public static final String SC_OPENSTACK_UNAVAILABLE   = "106.999503";
    public static final String SC_FIREWALL_UNAVAILABLE    = "106.999503";

    public static final String SC_OPENSTACK_SERVER_ERROR  = "106.101503";
    public static final String SC_OPENSTACK_FIPCREATE_ERROR  = "106.102503";
    public static final String SC_FIREWALL_SERVER_ERROR   = "106.201503";

}
