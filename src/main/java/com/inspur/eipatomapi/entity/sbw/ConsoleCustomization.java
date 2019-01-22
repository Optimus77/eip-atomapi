package com.inspur.eipatomapi.entity.sbw;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.Serializable;

@Data
public class ConsoleCustomization implements Serializable {
    private String region;

    private String billType;

    private String chargemode;

    private Integer bandwidth;

    private String duration;

    private String sharedbandwidthname;

    private String method;
}
