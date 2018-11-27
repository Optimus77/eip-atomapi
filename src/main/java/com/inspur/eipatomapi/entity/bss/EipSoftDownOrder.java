package com.inspur.eipatomapi.entity.bss;

import lombok.Data;

import java.util.List;

@Data
public class EipSoftDownOrder {
    private String flowId;

    private List<EipSoftDownInstance> instanceList;

}
