package com.inspur.eipatomapi.entity.sbw;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inspur.eipatomapi.entity.eip.Eip;
import com.inspur.eipatomapi.entity.eip.Resourceset;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SbwReturnDetail implements Serializable {
    @JsonProperty("sbwid")
    private String sbwId;

    @JsonProperty("sharedbandwidthname")
    private String sharedbandwidthName;

    @JsonProperty("billType")
    private String billType;

    @JsonProperty("chargemode")
    private String chargeMode;

    @JsonProperty("bandwidth")
    private int bandWidth;

    @JsonProperty("duration")
    private String duration;

    @JsonProperty("ipcount")
    private int ipcount;

    @JsonProperty("create_at")
    @JsonFormat(shape= JsonFormat.Shape.STRING, timezone = "UTC", pattern="yyyy-MM-dd HH:mm:ss")
    private Date createTime;

}
