package com.inspur.eipatomapi.entity.fw;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
@Getter
@Setter
public class FwBaseObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private String manageIP;
    private String managePort = "";
    private String manageUser = "";
    private String managePwd = "";
    private String instanceid;

}
