package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.sbw.SbwUpdateParamWrapper;
import org.springframework.http.ResponseEntity;


public interface ISbwService {

    ResponseEntity atomDeleteSbw(String sbwId);

    ResponseEntity listSbws(Integer pageIndex,Integer pageSize,String searchValue);

    ResponseEntity getSbwDetail(String sbwId);

    ResponseEntity updateSbwBandWidth(String id, SbwUpdateParamWrapper param);

    ResponseEntity getSbwByInstanceId(String instanceId);

    ResponseEntity getSbwCount();

    ResponseEntity getSbwByProjectId(String projectId);
}
