package com.inspur.eipatomapi.entity.sbw;

import com.inspur.eipatomapi.util.TypeConstraint;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class SbwAllocateParam implements Serializable {
    @NotBlank(message = "can not be blank.")
    private String region;

    @TypeConstraint(allowedValues = {"monthly", "hourlySettlement"}, message = "Only monthly,hourlySettlement is allowed. ")
    private String billType = "hourlySettlement";

    //    @Pattern(regexp="[0-9-]{1,2}", message="param purchase time error.")
    private String duration;

    @Range(min = 5, max = 500, message = "value must be 5-500.")
    private int bandwidth;

    private String sbwName;
}
