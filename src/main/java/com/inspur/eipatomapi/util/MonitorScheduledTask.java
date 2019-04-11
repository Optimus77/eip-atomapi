package com.inspur.eipatomapi.util;

import com.inspur.eipatomapi.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Map;

@Configuration
@Slf4j
@EnableScheduling
public class MonitorScheduledTask implements SchedulingConfigurer {

    private final MonitorService monitorService;

    private static final String SCHEDULE_TASK_TIME = "SCHEDULE_TASK_TIME";

    @Autowired
    public MonitorScheduledTask(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.addTriggerTask(() -> {
            try {
                monitorService.scheculeTask();
            } catch (Exception e) {
                log.error("Schecule task execute error!", e);
            }
        }, triggerContext -> {
            String cron = getCron();
            if (cron == null || "".equals(cron)) {
                cron = "0 0/1 0/1 * * ? ";
            }
            //定时任务触发,可修改定时任务的执行周期
            CronTrigger trigger = new CronTrigger(cron);
            return trigger.nextExecutionTime(triggerContext);
        });
    }

    private String getCron() {
        Map<String,String > config =  CommonUtil.getUserConfig();
        if( config.containsKey(HsConstants.SCHEDULETIME)){
            String timeDely = config.get(HsConstants.SCHEDULETIME);
            if(null != timeDely) {
                return timeDely;
            }
        }
        return "0 0/1 0/1 * * ? ";
    }
}
