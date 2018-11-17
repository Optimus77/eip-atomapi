package com.inspur.eipatomapi.entity.fw;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class FwQuery {
    private List<String> fields;
    private List<FwCondition> conditions;
    private List<String> sorts;
    private int start = 0;
    private int limit = 100000;
    private int page = 1;

}
