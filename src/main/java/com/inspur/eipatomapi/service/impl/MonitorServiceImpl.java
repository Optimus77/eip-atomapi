package com.inspur.eipatomapi.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.inspur.cloud.cloudmonitormetric.entity.MetricEntity;
import com.inspur.cloud.cloudmonitormetric.handler.ProducerHandler;
import com.inspur.eipatomapi.controller.EipController;
import com.inspur.eipatomapi.entity.ReturnMsg;
import com.inspur.eipatomapi.entity.fw.Firewall;
import com.inspur.eipatomapi.repository.FirewallRepository;
import com.inspur.eipatomapi.service.EipDaoService;
import com.inspur.eipatomapi.service.FirewallService;
import com.inspur.eipatomapi.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class MonitorServiceImpl implements MonitorService {

    private static final String ADMIN = "admin";
    private static final String EIP_IPS_STATUS = "eip_ips_status";
    private static final String EIP_POD_STATUS = "eip_pod_status";
    private static final String STATUS = "status";
    private static final String EIP_COUNT = "eip_count";
    private static final String FREE_EIP_COUNT = "free_eip_count";
    private static final String USING_EIP_COUNT = "using_eip_count";
    private static final String ERROR_EIP_COUNT = "error_eip_count";
    private String eipSta="ACTIVE";

    @Value("${regionCode}")
    private String regionCode;

    private final FirewallService firewallService;
    private final ProducerHandler producerHandler;
    private final FirewallRepository firewallRepository;
    private final EipDaoService eipDaoService;

    @Autowired
    public MonitorServiceImpl(EipDaoService eipDaoService,
                              FirewallService firewallService,
                              ProducerHandler producerHandler,
                              FirewallRepository firewallRepository) {
        this.firewallService = firewallService;
        this.producerHandler = producerHandler;
        this.firewallRepository = firewallRepository;
        this.eipDaoService = eipDaoService;
    }


    @Override
    public void scheculeTask() {

        log.info("**************************start timed task 1 : eip num check**************************");
        List<MetricEntity> podMonitorMetric = Collections.synchronizedList(new ArrayList<>());

        Long timestamp = System.currentTimeMillis();

        MetricEntity metricEntity = new MetricEntity();
        metricEntity.setMetricName(EIP_IPS_STATUS);

        metricEntity.setTimestamp(timestamp);
        metricEntity.setAccount(ADMIN);
        metricEntity.setResourceId("default");
        metricEntity.setResourceName("default");
        metricEntity.setRegion(regionCode);
        int free_count = eipDaoService.getFreeEipCount();
        int erro_count = eipDaoService.getUsingEipCountByStatus("ERROR");
        int using_count = eipDaoService.getUsingEipCount();
        String metricValue = "0";
        if(free_count < 100){
            metricValue = "1";
        }
        if(erro_count >= 1){
            if(free_count < 100) {
                metricValue = "3";
            }else{
                metricValue = "2";
            }
        }
        metricEntity.setMetricValue(Float.valueOf(metricValue));//0-正常//1-资源不够//2-有错误状态的EIP//3-资源告警状态错误

        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(FREE_EIP_COUNT, String.valueOf(free_count));
        dimensions.put(USING_EIP_COUNT, String.valueOf(using_count));
        dimensions.put(ERROR_EIP_COUNT, String.valueOf(erro_count));
        dimensions.put(EIP_COUNT, String.valueOf(free_count + using_count));
        dimensions.put("service", "eip");
        metricEntity.setDimensions(dimensions);
        podMonitorMetric.add(metricEntity);

        log.info("task 1 result: " + JSONObject.toJSONString(podMonitorMetric));
        producerHandler.sendMetrics(podMonitorMetric);
        log.info("**************************eip num check success**************************");

        log.info("***************start timed task 1 : firewall status check******************");
        List<MetricEntity> eipMonitorMetric = Collections.synchronizedList(new ArrayList<>());
        List<Firewall> fireWallBeans = firewallRepository.findAll();
        fireWallBeans.parallelStream().forEach(firewall -> {
            String firewallSta="ACTIVE";
            float firewallMetricValue = 0;
            if(!firewallService.ping(firewall.getIp())){
                firewallSta="DOWN";
                firewallMetricValue = 1;
            }
            String id = firewall.getId();
            MetricEntity fireWallMetricEntity = new MetricEntity();
            fireWallMetricEntity.setMetricName(EIP_POD_STATUS);
            fireWallMetricEntity.setMetricValue(firewallMetricValue);//0-正常//1-异常
            fireWallMetricEntity.setTimestamp(timestamp);
            fireWallMetricEntity.setAccount("Admin");
            fireWallMetricEntity.setResourceId(id);
            fireWallMetricEntity.setResourceName("firewall");
            fireWallMetricEntity.setRegion(firewall.getRegion());

            Map<String, String> fireWallDimensions = new HashMap<>();
            fireWallDimensions.put("service", "eip");
            fireWallDimensions.put("eip_server_status", eipSta);
            fireWallDimensions.put("eip_firewall_status", firewallSta);
            fireWallMetricEntity.setDimensions(fireWallDimensions);
            eipMonitorMetric.add(fireWallMetricEntity);

        });
        log.info("task 2 result : " + JSONObject.toJSONString(eipMonitorMetric));
        producerHandler.sendMetrics(eipMonitorMetric);
        log.info("*************************end of task 2**************************");
    }


}
