package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class EipReturnBase implements Serializable {
    @JsonProperty("eipid")
    private String eipId;

    @Column(name="eip_address")
    @JsonProperty("eip_address")
    private String eipAddress;

    @JsonProperty("chargetype")
    private String chargeType;

    @JsonProperty("chargemode")
    private String chargeMode;

    @JsonProperty("purchasetime")
    private String purchaseTime;

    @JsonProperty("bandwidth")
    private int bandWidth;

    @JsonProperty("iptype")
    private String ipType;

    @JsonProperty("sharedbandwidth_id")
    private String sharedBandWidthId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("create_at")
    @JsonFormat(shape= JsonFormat.Shape.STRING, timezone = "GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    private Date createTime;

}
