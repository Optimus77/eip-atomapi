package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.*;

import java.util.List;

public interface INatService {

    List<FwSnatVo> getSnat(FwQuery var1, FwBaseObject var2);

    FwResponseBody addDnat(FwDnatVo var1);


    FwResponseBody delDnat(FwDnatVo var1);

    FwResponseBody addSnat(FwSnatVo var1);

    FwResponseBody delSnat(FwSnatVo var1);

}
