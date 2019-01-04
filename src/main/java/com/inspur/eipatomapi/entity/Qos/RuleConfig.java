package com.inspur.eipatomapi.entity.Qos;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class RuleConfig implements Serializable {
    private int id =1;
    @SerializedName("src_addr")
    private List<SrcAddr> srcAddr =new ArrayList<>();

    @SerializedName("src_host")
    private  List<SrcSubnet> srcHost =new ArrayList<>();

    @SerializedName("src_subnet")
    private List<SrcSubnet> srcSubnet =new ArrayList<>();

    @SerializedName("src_range")
    private Set<IpRange> srcRange =new HashSet<>();

    @SerializedName("dst_addr")
    private List<String> dstAddr =new ArrayList<>();

    @SerializedName("dst_host")
    private List<String> dstHost =new ArrayList<>();

    @SerializedName("dst_subnet")
    private List<SrcSubnet> dstSubnet =new ArrayList<>();

    @SerializedName("dst_range")
    private Set<IpRange> dstRange =new HashSet<>();

    private List<String> user =new ArrayList<>();

    @SerializedName("usergroup")
    private List<String> userGroup =new ArrayList<>();

    private List<String> service =new ArrayList<>();

    private List<String> application =new ArrayList<>();

    @SerializedName("src_zone")
    private List<String> srcZone =new ArrayList<>();

    @SerializedName("ingress_if")
    private List<String> ingressIf =new ArrayList<>();

    @SerializedName("dst_zone")
    private List<String> dstZone =new ArrayList<>();

    @SerializedName("egress_if")
    private List<String> egressIf =new ArrayList<>();

    private List<String> vlan =new ArrayList<>();

    private List<String> tos =new ArrayList<>();
}
