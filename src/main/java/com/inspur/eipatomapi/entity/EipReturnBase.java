package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class EipReturnBase implements Serializable {

    private String eipid;

    private String eip_address;

    private String chargetype;

    private String chargemode;

    private String purchasetime;

    private int banwidth;

    private String iptype;

    private String sharedbandwidth_id;

    private String status;

    @JsonFormat(shape= JsonFormat.Shape.STRING, timezone = "GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    private Date createtime;

}
