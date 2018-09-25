package com.inspur.eipatomapi.entity;



import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class FwAddAndDelDnat {
    private String vr_name;
    List<FwDnatRule> dnat_rule = new ArrayList<>();

}
