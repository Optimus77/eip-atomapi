package com.inspur.eipatomapi.entity;



import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class FwAddAndDelDnat {
    private String vrName;
    List<FwDnatRule> dnatRule = new ArrayList<>();

}
