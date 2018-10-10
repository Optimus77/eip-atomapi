package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FwSnat {
    private String ruleId;
    private int enable;
    private String groupId;
    private String service;
    private String description;
    private String posFlag;
    private boolean log;
    private String fromIsIp;
    private String from;
    private String toIsIp;
    private String to;
    private String flag;
    private String eif;
    private String transTo;
    private String transToIsIp;
    private String evr;
}
