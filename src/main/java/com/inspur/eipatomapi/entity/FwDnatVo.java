package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class FwDnatVo extends FwBaseObject{

    private static final long serialVersionUID = 1L;
    private String objectid;
    private String dnatid;
    private String securityid;
    private String saddrtype;
    private String saddr;
    private String daddrtype;
    private String daddr;
    private String servicename;
    private String servicecontent;
    private String transfer;
    private String transferaddrtype;
    private String transferaddr;
    private String istransferport;
    private String transferport;
    private String dnatlog;
    private String dnatorder;
    private String appnettype;
    private String dnatstat;
    private String description;
    private String mtime;
    private String muserid;
    private String vrid;
    private String vrname;
    private String ha;
    private String protocal;
    private String dport;

}
