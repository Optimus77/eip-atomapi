package com.inspur.eipatomapi.util;

import com.inspur.cloud.cloudmonitormetric.entity.MetricEntity;
import com.inspur.cloud.cloudmonitormetric.handler.ProducerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EipScheduledTasks {

    @Autowired
    private ProducerHandler producerHandler;

    @Scheduled(fixedRate = 30*1000)
    public void run(){
        List<MetricEntity> list = new ArrayList();
        MetricEntity metricEntity = new MetricEntity();
        metricEntity.setTimestamp(System.currentTimeMillis());
        metricEntity.setAccount("");
        metricEntity.setMetricName("");
        list.add(metricEntity);
        producerHandler.sendMetrics(list);
    }

}
