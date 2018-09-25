package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FwSnat {
    private String rule_id;
    private int enable;
    private String group_id;
    private String service;
    private String description;
    private String pos_flag;
    private boolean log;
    private String from_is_ip;
    private String from;
    private String to_is_ip;
    private String to;
    private String flag;
    private String eif;
    private String trans_to;
    private String trans_to_is_ip;
    private String evr;
}
