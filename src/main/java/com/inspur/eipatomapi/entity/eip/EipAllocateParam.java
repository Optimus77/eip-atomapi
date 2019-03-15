package com.inspur.eipatomapi.entity.eip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inspur.eipatomapi.util.TypeConstraint;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class EipAllocateParam implements Serializable {

    @NotBlank(message = "can not be blank.")
    private String region;

    @TypeConstraint(allowedValues = {"monthly","hourlySettlement", "other"}, message = "Only monthly,hourlySettlement is allowed. ")
    private String billType = "hourlySettlement";

    //@Pattern(regexp="[0-9-]{1,2}", message="param duration time error.")
    private String duration;

    private String ipv6 = "no";

    @NotBlank(message = "can not be blank.")
    @TypeConstraint(allowedValues = {"5_bgp","5_sbgp", "5_telcom", "5_union", "BGP"}, message = "Only 5_bgp,5_sbgp, 5_telcom, 5_union, BGP is allowed. ")
    private String iptype;

    @TypeConstraint(allowedValues = {"Bandwidth","SharedBandwidth"}, message = "Only Bandwidth,SharedBandwidth is allowed. ")
    private String chargemode = "Bandwidth";

    @Range(min=1,max=500,message = "value must be 1-500.")
    private int bandwidth;

    private String sharedBandWidthId;
}
