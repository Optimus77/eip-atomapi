package com.inspur.eipatomapi.entity.Qos;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.Serializable;
@Data
public class IpContent implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("src_addr")
    private SrcAddr srcAddr= new SrcAddr();

    @SerializedName("dst_range")
    private IpRange ipRange = new IpRange();
}
