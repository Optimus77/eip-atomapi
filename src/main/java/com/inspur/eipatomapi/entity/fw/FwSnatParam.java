package com.inspur.eipatomapi.entity.fw;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Setter
@Getter
public class FwSnatParam {
    private String vr_name;
    private List<FwSnat> snat_rule = new ArrayList<>();
}
