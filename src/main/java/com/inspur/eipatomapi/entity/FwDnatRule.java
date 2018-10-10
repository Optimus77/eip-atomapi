package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FwDnatRule {
    private String ruleId;
    private String groupId;
    private String service;
    private String port;
    private String description;
    private String trackTcpPort;
    private String posFlag;
    private String log;
    private String fromIsIp;
    private String from;
    private String toIsIp;
    private String to;
    private String transToIsIp;
    private String transTo;
    private String flag;
    private String enable;
}
