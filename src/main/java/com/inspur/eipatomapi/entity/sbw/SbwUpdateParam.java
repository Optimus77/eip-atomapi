package com.inspur.eipatomapi.entity.sbw;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inspur.eipatomapi.util.TypeConstraint;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;

@Data
public class SbwUpdateParam implements Serializable {
    @Range(min=5,max=2000,message = "value must be 5-2000.")
    @JsonProperty(value = "bandwidth")
    private int bandWidth;

    @TypeConstraint(allowedValues = {"monthly","hourlySettlement"}, message = "Only monthly,hourlySettlement is allowed. ")
    private String billType = "hourlySettlement";

    private String sbwName;
//    @NotBlank(message = "can not be blank.")
    private String region;

}
