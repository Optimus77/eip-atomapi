package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class EipAllocateParam implements Serializable {

    private String region;

    private String iptype;

    @NotNull
    private String chargetype = "PrePaid";

    @NotNull
    private String chargemode = "BandWidth";

    private String purchasetime;

    @Range(min=1,max=2000)
    private int bandwidth;

    @JsonProperty("sharedbandwidthid")
    private String sharedBandWidthId;
}
