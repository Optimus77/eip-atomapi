package com.inspur.eipatomapi.entity.fw;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FwResponseBody {
    private boolean success = false;
    private FwResponseException exception;
    private Object object;

}
