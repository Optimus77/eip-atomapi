package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class EipAllocateParam implements Serializable {

    private String region;

    private String iptype;

    private String chargetype = "PrePaid";

    private String chargemode = "BandWidth";

    private String purchasetime;

    private int bandwidth;

    @JsonProperty("sharedbandwidthid")
    private String sharedBandWidthId;
}
