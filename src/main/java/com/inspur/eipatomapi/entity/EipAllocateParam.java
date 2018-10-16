package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class EipAllocateParam implements Serializable {

    @NotBlank(message = "can not be blank.")
    private String region;

    @NotBlank(message = "can not be blank.")
    private String iptype;

    private String chargetype = "PrePaid";

    private String chargemode = "BandWidth";

    private String purchasetime;

    @Range(min=1,max=2000)
    private int bandwidth;

    @JsonProperty("sharedbandwidthid")
    private String sharedBandWidthId;
}
