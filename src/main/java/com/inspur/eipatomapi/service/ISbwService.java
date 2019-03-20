package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.sbw.SbwUpdateParam;
import com.inspur.eipatomapi.entity.sbw.SbwUpdateParamWrapper;
import org.springframework.http.ResponseEntity;


public interface ISbwService {

    ResponseEntity atomDeleteSbw(String sbwId);

    ResponseEntity listShareBandWidth(Integer pageIndex, Integer pageSize, String searchValue);

    ResponseEntity getSbwDetail(String sbwId);

    ResponseEntity updateSbwBandWidth(String id, SbwUpdateParam param);

    ResponseEntity getSbwCount();

    ResponseEntity getSbwByProjectId(String projectId);
}
