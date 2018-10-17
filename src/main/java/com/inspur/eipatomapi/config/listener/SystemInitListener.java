package com.inspur.eipatomapi.config.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * @author: jiasirui
 * @date: 2018/9/25 11:08
 * @description:
 */
@WebListener
public class SystemInitListener implements ServletContextListener {

    private  static Log log = LogFactory.getLog(SystemInitListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("*********************SystemInitListener contextInitialized");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("*********************SystemInitListener contextDestroyed");
    }
}
