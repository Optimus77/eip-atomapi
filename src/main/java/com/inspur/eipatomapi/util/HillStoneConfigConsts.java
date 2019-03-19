package com.inspur.eipatomapi.util;

public class HillStoneConfigConsts {
    //Enter commond
    public static final String SSH_ENTER = "\r";
    public static final String CONFIGURE_MODEL_ENTER = "configure\r";
    public static final String POLICY_GLOBAL_MODEL_ENTER = "policy-global\r";
    public static final String RULE_MODEL_ENTER = "rule\r";
    public static final String SRC_ADDR_ANY_ENTER = "src-addr any\r";
    public static final String EXIT_ENTER = "exit\r";
    public static final String ENTER_END = "\rend";
    public static final String D_TYPE_ADDRESS_ENTER = "/32\r";
    public static final String C_TYPE_ADDRESS_ENTER = "/24\r";
    public static final String B_TYPE_ADDRESS_ENTER = "/16\r";
    public static final String A_TYPE_ADDRESS_ENTER = "/8\r";
    public static final String QOS_ENGINE_FIRST_ENTER = "qos-engine first\r";
    public static final String QOS_ENGINE_SECOND_ENTER = "qos-engine second\r";


    //Space commond
    public static final String SSH_SPACE = " ";
    public static final String SERVICE_MODEL_SPACE = "service ";
    public static final String DST_PORT_SPACE = "dst-port  ";
    public static final String DST_IP_SPACE = "dst-ip ";
    public static final String ACTION_SPACE = "action ";
    public static final String NO_RULE_ID_SPACE = "no rule id ";
    public static final String ID_SPACE = "id ";
    public static final String MOVE_SPACE = "move ";
    public static final String SPACE_TOP = " top";
    public static final String SPACE_BOTTOM = " bottom";
    public static final String ROOT_PIPE_SPACE = "root-pipe ";


    //commond
    public static final String ACTION_DENY = "deny";
    public static final String ACTION_PERMIT = "permit";
    public static final String TCP_PROTO_TYPE = "TCP";
    public static final String UDP_PROTO_TYPE = "UDP";
    public static final String ICMP_PROTO_TYPE = "ICMP";
    public static final String ANY_PROTO_TYPE = "Any";
    //    禁用
    public static final String DISABLE = "disable";
    //    启用
    public static final String NO_DISABLE = "no disable";


}
