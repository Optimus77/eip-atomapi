package com.inspur.eipatomapi.util;

import com.inspur.cloud.cloudmonitormetric.entity.MetricEntity;
import com.inspur.cloud.cloudmonitormetric.handler.ProducerHandler;
import com.inspur.eipatomapi.service.impl.EipServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EipScheduledTasks {

    @Autowired
    private ProducerHandler producerHandler;

    @Autowired
    private EipServiceImpl eipService;

    @Scheduled(fixedRate = 30*1000)
    public void run(){
        List<MetricEntity> list = new ArrayList();
        MetricEntity metricEntity = new MetricEntity();
        metricEntity.setTimestamp(System.currentTimeMillis());
        metricEntity.setAccount("");
        metricEntity.setMetricName("eip_total");
        ResponseEntity totalEipCount = eipService.getTotalEipCount();
        Object body = totalEipCount.getBody();
        metricEntity.setMetricValue(0f);
        metricEntity.setRegion("cn-north-3");
        metricEntity.setResourceId("");
        metricEntity.setResourceName("");
        Map<String,String> dimensionMap = new HashMap();
        dimensionMap.put("service","");
        metricEntity.setDimensions(dimensionMap);
        list.add(metricEntity);
        producerHandler.sendMetrics(list);
    }

}
