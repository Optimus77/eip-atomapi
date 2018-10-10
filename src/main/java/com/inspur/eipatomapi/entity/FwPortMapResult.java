package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FwPortMapResult {
    private String port;
    private String to;
    private String transTo;
    private String ruleId;
    private String transToIsIp;
    private String enable;
    private String service;
    private String groupId;
    private String from;
    private String toIsIp;
    private String fromIsIp;

}
