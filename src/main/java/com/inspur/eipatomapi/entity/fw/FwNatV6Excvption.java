package com.inspur.eipatomapi.entity.fw;

import lombok.Data;


public class FwNatV6Excvption extends Exception {

    private String message;

    public FwNatV6Excvption(String message) {
        super(message);
    }

}
