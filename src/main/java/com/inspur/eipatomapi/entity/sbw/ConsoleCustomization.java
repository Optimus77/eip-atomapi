package com.inspur.eipatomapi.entity.sbw;

import lombok.Data;

@Data
public class ConsoleCustomization {
    private String region;

    private String billType;

    private String chargeMode;

    private Integer bandWidth;

    private String duration;

    private String sharedBandWidthName;
}
