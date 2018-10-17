package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FwPortMapResult {
    private String port;
    private String to;
    private String trans_to;
    private String rule_id;
    private String trans_to_is_ip;
    private String enable;
    private String service;
    private String group_id;
    private String from;
    private String to_is_ip;
    private String from_is_ip;

}
