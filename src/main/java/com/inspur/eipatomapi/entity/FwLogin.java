package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FwLogin {
    private String userName = "InnetAdmin";
    private String password = "innetadmin";
    private String ifVsysId = "0";
    private String vrId = "1";
    private String lang = "zh_CN";

}
