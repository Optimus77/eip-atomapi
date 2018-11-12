package com.inspur.eipatomapi.entity.eip;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EipReturnDetail implements Serializable {

    @JsonProperty("eipid")
    private String eipId;

    @Column(name="eip_address")
    @JsonProperty("eip_address")
    private String eipAddress;

    @JsonProperty("billType")
    private String billType;

    @JsonProperty("chargemode")
    private String chargeMode;

    @JsonProperty("bandwidth")
    private int bandWidth;

    @JsonProperty("duration")
    private String duration;

    @JsonProperty("iptype")
    private String ipType;

    @JsonProperty("sharedbandwidth_id")
    private String sharedBandWidthId;

    //Todo: add or not ????
    @JsonProperty("floatingip")
    private String floatingIp;
    //    private String floating_ipId;

    @JsonProperty("private_ip_address")
    private String privateIpAddress;

    @JsonProperty("resourceset")
    private Resourceset resourceset;

    @JsonProperty("status")
    private String status;

    @JsonProperty("create_at")
    @JsonFormat(shape= JsonFormat.Shape.STRING, timezone = "GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
    private Date createTime;

}
