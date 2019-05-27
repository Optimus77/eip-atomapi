package com.inspur.eipatomapi.util;

public class HsConstants {
	public static final String USER = "InnetAdmin";

	public static final String PAYSUCCESS = "paySuccess";
    public static final String HOURLYSETTLEMENT = "hourlySettlement";
    public static final String MONTHLY = "monthly";
    public static final String EIP = "EIP";
    public static final String PROVIDER = "provider";
    public static final String IMPACTFACTOR = "impactFactor";
    public static final String NET = "net";
    public static final String M = "M";
    public static final String BANDWIDTH = "Bandwidth";
    public static final String SHAREDBANDWIDTH = "SharedBandwidth";
    public static final String FAIL = "fail";
	public static final String FORBIDEN = "Forbiden";
	public static final String EXCEPTION = "exception";
	public static final String ACTIVE = "ACTIVE";
	public static final String DOWN = "DOWN";
	public static final String STOP = "STOP";
    public static final String ERROR = "ERROR";
	public static final String BINDING = "BINDING";
    public static final String CREATING = "CREATING";
	public static final String SCHEDULETIME = "scheduleTime";

    public static final String BGP = "BGP";
    public static final String BILLINGITEM = "billingItem";
    public static final String TRANSFER = "transfer";
    public static final String REGION = "region";
    public static final String IPTYPE = "iptype";
    public static final String DURATION = "duration";
    public static final String UNSUBSCRIBE = "unsubscribe";
    public static final String SUCCESS = "success";
    public static final String DELETE = "DELETE";
    public static final String POST = "post";
    public static final String AUTHORIZATION = "Authorization";




	public static final String VSYS = "0";
	public static final String VRID = "1";
	public static final String LANG = "zh_CN";
	public static final String HILLTONE_LANGUAGE = "Hillstone-language";
	public static final String HTTP = "http://";
	public static final String HTTPS = "https://";
	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE_TEXT_JSON = "text/json";
	public static final String FROM_ROOT_SYS = "fromrootvsys=true;";
	public static final String REST_GET = "?isDynamic=0&";
	public static final String REST_GET1 = "?idfield=id&isDynamic=1&";
	public static final String REST_QUERY = "query";
	public static final String REST_ID = "id";
	public static final String REST_LOGIN = "/rest/login";
	public static final String REST_ADDR = "/rest/addrbook_address";
	public static final String REST_ADDR_UPDATE = "?isTransaction=1&idfield=is_ipv6,type,name";
	public static final String REST_ZONE = "/rest/zone_zone";
	public static final String REST_ZONE_INFO = "/rest/zone_Inf?";
	public static final String REST_ZONE_AD = "/rest/zone_ad";
	public static final String REST_ZONE_UPDATE = "?isTransaction=1&idfield=is_ipv6,type,name";
	public static final String REST_ZONE_WLIST = "/rest/zone_whitelist?target=whitelist";

	public static final String REST_IDPRULE = "/rest/idp_ruleset";
	public static final String REST_IDPRULE_AD = "?target=http.virtual_host";
	public static final String REST_IDPRULE_UPDATE = "/rest/ips_webserver?target=http.virtual_host";

	public static final String REST_INTERFACE = "/rest/if_interface";
	public static final String REST_INTERFACE_UPDATE = "/rest/Interface_WebUi?idfield=name";

	public static final String REST_IPS = "/rest/ips_profile";
	public static final String REST_IPS_UPDATE = "?idfield=name,ruleset";

	public static final String REST_SESSIONLIMIT = "/rest/sessionLimit?target=sess_limit_entry";
	public static final String REST_SESSIONLIMIT_UPDATE = "&isPartial=1&idfield=name,sess_limit_entry.id";

	public static final String REST_POLICY = "/rest/policy_rule";
	public static final String REST_POLICY_PROCESS = "?isTransaction=1&idfield=id";
	public static final String REST_POLICY_UPDATE = "?idfield=id";
	
	public static final String REST_SNAT = "/rest/Snat";
	public static final String REST_DNAT = "/rest/Dnat";
	
	public static final String REST_DNAT_UPDATE = "?target=dnat_rule";
	public static final String REST_SNAT_ADD_UPDATE_DELETE = "?target=snat_rule";
	public static final String REST_SNAT_STATUS = "/rest/vr_vrouter?target=snat_rule&subLevel=1";
	public static final String REST_DNAT_CHANGESTATE = "/rest/vr_vrouter?target=dnat_rule&subLevel=1";
	public static final String REST_PDNAT = "/rest/Dnat";
	public static final String REST_SERVICEGROUP = "/rest/servicebook_service_group";

	public static final String REST_SERVICE = "/rest/servicebook_service";
	public static final String REST_SERVICE_UPDATE = "/rest/servicebook_service?idfield=name%2Ctype";

	public static final String REST_NETPARAM = "/rest/NetworkParameters";
	public static final String REST_NETPARAM_UPDATE = "";

	public static final String REST_SYSLOG = "/rest/systemLog";
	public static final String REST_THRLOG = "/rest/threatLog";
	public static final String REST_LOGD_MSG_TBL = "/rest/logd_msg_tbl";
	public static final String REST_GET2 = "?";
	public static final String REST_GET3 = "?%7B%22isDynamic%22:%221%22%7D&isDynamic=1&";
	public static final String REST_GET4 = "?isDynamic=1&";
	public static final String REST_PROCESS1 = "?target=dnat_rule";

	public static final String REST_ROUTE_UPDATE = "/rest/route_edit?target=ribv4&idfield=vr_name";
	
	
	public static final String REST_MONITOR_SNMP_STATUS = "/rest/snmpd_snmp_grp";
	public static final String REST_MONITOR_SNMP_HOST = "/rest/snmpd_snmp_server_host_tbl";
	public static final String REST_MONITOR_CONF = "/rest/predef_statistics_set";
	public static final String REST_MONITOR_LOG = "/rest/logd_syslog_server_tbl";
	public static final String REST_MONITOR_LOG_OF_MOST = "/rest/logd_logging_tbl";
	public static final String REST_MONITOR_LOG_OF_SESSION = "/rest/sessionLog";
	public static final String REST_MONITOR_LOG_OF_CONF = "/rest/conf_log";
	public static final String REST_MONITOR_LOG_OF_NAT = "/rest/nat_log";
	public static final String REST_MONITOR_LOG_OF_URL = "/rest/internet_log";
	public static final String REST_SYS_INFO = "/rest/admind_system_message?isDynamic=1";
	public static final String REST_SYS_DOWNCONFFILE_INIT = "/rest/execution?moduleName=mgmt&operation=export";
	public static final String REST_SYS_DOWNCONFFILE = "/download/bfm";
	public static final String REST_SYS_CONFFILE_SET = "/rest/execution?moduleName=mgmt&operation=import";
	public static final String REST_SYS_UPLOADCONFFILE = "/rest/file";
	
	public static final String REST_SYS_REBOOT = "/rest/execution?moduleName=admind&operation=reboot";
	
	public static final String REST_NTP_CONF = "/rest/ntpServer";
	public static final String REST_NTP_KEY_ENCRYPT_PWD = "/rest/admind_encrypt_password?isDynamic=1";
	public static final String REST_NTP_KEY = "/rest/ntp_authentication_key";
	public static final String REST_DROUTE_CONF = "/rest/vr_vrouter?target=ribv4";
	public static final String REST_DROUTE_EDIT = "/rest/route_edit?target=ribv4&idfield=vr_name";
	
	
	public static final int STATUS_CODE_200 = 200;
	public static final int STATUS_CODE_204 = 204;

	public static final String TH_0 = "0";
	public static final String TH_1 = "1";
	public static final String TH_2 = "2";
	public static final String TH_3 = "3";
	public static final String TH_4 = "4";
	public static final String TH_5 = "5";
	public static final String TH_6 = "6";
	public static final String TH_7 = "7";
	public static final String TH_8 = "8";
	public static final String TH_9 = "9";
	public static final String TH_10 = "10";
	public static final String TH_11 = "11";
	public static final String TH_17 = "17";
	public static final String TH_1500 = "1500";

	public static final String ITEM_0 = "0";
	public static final String ITEM_1 = "1";
	public static final String ITEM_2 = "2";
	public static final String ITEM_3 = "3";
	public static final String ITEM_4 = "4";
	public static final String ITEM_5 = "5";
	public static final String ITEM_6 = "6";
	public static final String ITEM_7 = "7";
	public static final String ITEM_8 = "8";
	public static final String ITEM_9 = "9";
	public static final String ITEM_10 = "10";
	public static final String ITEM_11 = "11";
	public static final String ITEM_12 = "12";
	public static final String ITEM_13 = "13";
	public static final String ITEM_14 = "14";
	public static final String ITEM_15 = "15";
	public static final String ITEM_16 = "16";
	public static final String ITEM_17 = "17";
	public static final String ITEM_18 = "18";
	public static final String ITEM_19 = "19";
	public static final String ITEM_20 = "20";
	public static final String ITEM_21 = "21";
	public static final String ITEM_22 = "22";
	public static final String ITEM_23 = "23";
	public static final String ITEM_24 = "24";

	public static final String OPERATION_100 = "100";
	public static final String OPERATION_101 = "101";
	public static final String OPERATION_102 = "102";
	public static final String SERVICE_TCP = "TCP";
	public static final String SERVICE_UDP = "UDP";
	public static final String SERVICE_ICMP = "ICMP";
	public static final String SERVICE_OTHER = "Other";
	
	public static final String MAN_INAME = "ethernet0/0";
	public static final String FUN_INAME = "ethernet0/1";
	public static final String CONN_INAME = "ethernet0/2";
	public static final String MASK = "24";
	
	public static final String VR_NAME = "trust-vr";
	public static final String DEFAULT_NEXTHOP = "172.23.10.254";
	
	/////////////////////////R2P2 version/////////////////////////
	public static final String REST_URL_INTERFACE = "/rest/doc/interface";
	public static final String REST_URL_POLICY = "/rest/doc/policy";
	public static final String REST_URL_ZONE = "/rest/doc/zone";
	public static final String REST_URL_DNAT = "/rest/doc/dnat";
	public static final String REST_URL_SNAT = "/rest/doc/snat";
	public static final String REST_URL_ADDR = "/rest/doc/addrbook";
	public static final String REST_URL_SERVICE = "/rest/doc/servicebook";

	public static final String COLON = ":";

	public static final String ADD_EIP_TO_SBW_METHOD = "addEIPtoSBW";
	public static final String REMOVE_EIP_FROM_SBW_METHOD = "removeEIPfromSBW";
	public static final String ADJUST_BANDWIDTH_SBW_METHOD = "adjustBandwidthSbw";


	public static final String TOTAL_PAGES = "totalPages";
	public static final String TOTAL_ELEMENTS = "totalElements";
	public static final String CURRENT_PAGE = "currentPage";
	public static final String CURRENT_PAGEPER = "currentPagePer";
	public static final String RESULT = "result";
	public static final String PIPE_NAME= "pipeName";
	public static final String FALSE = "false";
	public static final String TRUE = "true";
	public static final String BAND_WIDTH = "bandWidth";
	public static final String IN_BAND_WIDTH = "inBandWidth";
	public static final String REASON = "reason";
	public static final String HTTP_CODE = "httpCode";
	public static final String INTER_CODE = "interCode";
	public static final String REST_IQOS_ROOT = "/rest/iQos?target=root.rule";
	public static final String FLOATIP = "floatIp: ";


	// --- 1xx Informational ---

}
