package com.inspur.eipatomapi.entity.fw;

import lombok.Data;


@Data
public class FwResponseBody {
    private boolean success = false;
    private FwResponseException exception;
    private Object object;

}
