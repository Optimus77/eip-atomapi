package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class EipReturnDetail implements Serializable {

    private String eipid;

    private String eip_address;

    private String chargetype;

    private String chargemode;

    private String purchasetime;

    private int banwidth;

    private String iptype;

    private String sharedbandwidth_id;

    private String status;
//Todo: add or not ????
    private String floating_ip;
//    private String floating_ipId;

    private String private_ip_address;

    private Resourceset resourceset;

    @JsonFormat(shape= JsonFormat.Shape.STRING, timezone = "GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    private Date createtime;

}
