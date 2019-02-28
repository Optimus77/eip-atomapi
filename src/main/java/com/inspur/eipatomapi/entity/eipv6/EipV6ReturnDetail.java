package com.inspur.eipatomapi.entity.eipv6;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EipV6ReturnDetail implements Serializable {

    @JsonProperty("id")
    private String eipV6Id;

    @Column(name="ipv6")
    @JsonProperty("ipv6")
    private String ipv6;

    @JsonProperty("status")
    private String status;

    @JsonProperty("create_at")
    @JsonFormat(shape= JsonFormat.Shape.STRING, timezone = "UTC", pattern="yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonProperty("region")
    private String region;

    @JsonProperty("eipbandwidth")
    private int eipBandWidth;


    @JsonProperty("eipbillType")
    private String eipChargeType;


    @JsonProperty("eip_id")
    private String eipId;


    @Column(name="ipv4")
    @JsonProperty("ipv4")
    private String ipv4;

}
