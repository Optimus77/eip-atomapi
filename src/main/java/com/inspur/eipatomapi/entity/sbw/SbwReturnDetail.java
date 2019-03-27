package com.inspur.eipatomapi.entity.sbw;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

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
    private String chargeMode ="Bandwidth";

    @JsonProperty("bandwidth")
    private int bandWidth;

    @JsonProperty("duration")
    private String duration;

    @JsonProperty("ipcount")
    private int ipCount = 0;

    @JsonProperty("create_at")
    @JsonFormat(shape= JsonFormat.Shape.STRING, timezone = "UTC", pattern="yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonProperty("status")
    private String status;


}
