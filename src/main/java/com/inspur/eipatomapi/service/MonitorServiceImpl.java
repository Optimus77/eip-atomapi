package com.inspur.eipatomapi.service;

import com.alibaba.fastjson.JSONObject;
import com.inspur.cloud.cloudmonitormetric.entity.MetricEntity;
import com.inspur.cloud.cloudmonitormetric.handler.ProducerHandler;
import com.inspur.eipatomapi.entity.fw.Firewall;
import com.inspur.eipatomapi.repository.FirewallRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class MonitorServiceImpl implements MonitorService {

    private static final String ADMIN = "admin";
    private static final String EIP_TOTAL = "eip_total";
    private static final String EIP_POD_DETAIL = "eip_pod_detail";
    private static final String STATUS = "status";
    private static final String EIP_COUNT = "eip_count";
    private static final String FREE_EIP_COUNT = "free_eip_count";
    private static final String USING_EIP_COUNT = "using_eip_count";
    @Value("${regionCode}")
    private String regionCode;

    private final FirewallService firewallService;
    private final ProducerHandler producerHandler;
    private final FirewallRepository firewallRepository;

    @Autowired
    public MonitorServiceImpl(FirewallService firewallService,
                              ProducerHandler producerHandler,
                              FirewallRepository firewallRepository) {
        this.firewallService = firewallService;
        this.producerHandler = producerHandler;
        this.firewallRepository = firewallRepository;
    }


    @Override
    public void scheculeTask() {

        log.info("**************************启动定时任务1 : 检查eip统计数据**************************");
        List<MetricEntity> podMonitorMetric = Collections.synchronizedList(new ArrayList<>());

        Long timestamp = System.currentTimeMillis();

        MetricEntity metricEntity = new MetricEntity();
        metricEntity.setMetricName(EIP_TOTAL);
        metricEntity.setMetricValue(Float.parseFloat("0"));//0-正常//1-管理网不通//2-业务网不通//3-本地网不通
        metricEntity.setTimestamp(timestamp);
        metricEntity.setAccount(ADMIN);
        metricEntity.setResourceId("123");
        metricEntity.setResourceName("eip_service");
        metricEntity.setRegion(regionCode);

        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(EIP_COUNT, "44");//layer4/layer7/layerall
        dimensions.put(FREE_EIP_COUNT, "33");//0:独享/1:共享
        dimensions.put(USING_EIP_COUNT, "11");
        dimensions.put("service", "eip");
        metricEntity.setDimensions(dimensions);
        podMonitorMetric.add(metricEntity);

        log.info("定时任务1结果 : " + JSONObject.toJSONString(podMonitorMetric));
        producerHandler.sendMetrics(podMonitorMetric);
        log.info("**************************定时任务1结果发送成功**************************");

        log.info("**************************启动定时任务2 : 检查firewall状态**************************");
        Map<String, MetricEntity> monitorMap = podMonitorMetric.stream()
                .collect(Collectors.toConcurrentMap(MetricEntity::getResourceId, Function.identity()));

        List<MetricEntity> eipMonitorMetric = Collections.synchronizedList(new ArrayList<>());

        List<Firewall> fireWallBeans = firewallRepository.findAll();
        fireWallBeans.parallelStream().forEach(firewall -> {

            String id = firewall.getId();
            MetricEntity fireWallMetricEntity = new MetricEntity();
            fireWallMetricEntity.setMetricName(EIP_POD_DETAIL);
            fireWallMetricEntity.setMetricValue(Float.parseFloat("0"));
            fireWallMetricEntity.setTimestamp(timestamp);
            fireWallMetricEntity.setAccount("Admin");
            fireWallMetricEntity.setResourceId(id);
            fireWallMetricEntity.setResourceName("firewall");
            fireWallMetricEntity.setRegion(regionCode);

            Map<String, String> fireWallDimensions = new HashMap<>();
            fireWallDimensions.put("service", "eip");
            fireWallDimensions.put("eip_server_status", "active");
            fireWallDimensions.put("firewall_status", "active");
            fireWallMetricEntity.setDimensions(fireWallDimensions);
            eipMonitorMetric.add(fireWallMetricEntity);

        });
        log.info("定时任务2结果 : " + JSONObject.toJSONString(eipMonitorMetric));
        producerHandler.sendMetrics(eipMonitorMetric);
        log.info("**************************定时任务2结果发送成功**************************");
    }


}
