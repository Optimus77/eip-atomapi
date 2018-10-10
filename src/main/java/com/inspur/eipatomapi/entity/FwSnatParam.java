package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Setter
@Getter
public class FwSnatParam {
    private String vrName;
    private List<FwSnat> snatRule = new ArrayList<>();
}
