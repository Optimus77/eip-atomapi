package com.inspur.eipatomapi.entity.sbw;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inspur.eipatomapi.util.TypeConstraint;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class SbwUpdateParam implements Serializable {
//    @Range(min=5,max=2000,message = "value must be 5-2000.")
    private int bandwidth;

    @JsonProperty
    private List<String> eipIds;

    @JsonProperty("sbwName")
    private String sbwName;

//    @TypeConstraint(allowedValues = {"monthly","hourlySettlement"}, message = "Only monthly,hourlySettlement is allowed. ")
    private String billType = "hourlySettlement";

//    @TypeConstraint(allowedValues = {"Bandwidth","SharedBandwidth"}, message = "Only Bandwidth,SharedBandwidth is allowed. ")
    private String chargemode = "SharedBandwidth";

//    @NotBlank(message = "can not be blank.")
    private String region;

    @JsonProperty("chargemode")
    private String chargeMode;

    @JsonProperty("method")
    private String method;
}
