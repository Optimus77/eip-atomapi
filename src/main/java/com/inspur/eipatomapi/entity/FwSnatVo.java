package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FwSnatVo extends FwBaseObject {
    private static final long serialVersionUID = 1L;
    private String objectid;
    private String snatid;
    private String securityid;
    private String vrid;
    private String vrname;
    private String saddrtype;
    private String saddr;
    private String daddrtype;
    private String daddr;
    private String outflowtype;
    private String outflow;
    private String servicename;
    private String servicecontent;
    private String transfertype;
    private String transferaddrtype;
    private String transferaddr;
    private String mode;
    private String snatlog;
    private String snatorder;
    private String snatstat;
    private String description;
    private String sticky;
    private String ha;
    private String mtime;
    private String muserid;
    private String eif;
    private String posFlag;
    private String flag;
    private String protocal;
    private String sport;

}
