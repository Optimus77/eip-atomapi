package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FwDnatRule {
    private String rule_id;
    private String group_id;
    private String service;
    private String port;
    private String description;
    private String track_tcp_port;
    private String pos_flag;
    private String log;
    private String from_is_ip;
    private String from;
    private String to_is_ip;
    private String to;
    private String trans_to_is_ip;
    private String trans_to;
    private String flag;
    private String enable;
}
