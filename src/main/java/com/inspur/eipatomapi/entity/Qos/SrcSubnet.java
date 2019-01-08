package com.inspur.eipatomapi.entity.Qos;

import lombok.Data;

import java.io.Serializable;
@Data
public class SrcSubnet implements Serializable {

    private String ip;

    private Integer netMask;
}
