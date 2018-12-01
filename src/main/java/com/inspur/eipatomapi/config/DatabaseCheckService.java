package com.inspur.eipatomapi.config;

import com.inspur.eipatomapi.service.EipDaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Order(3)
public class DatabaseCheckService implements ApplicationRunner {
    public final static Logger log = LoggerFactory.getLogger(DatabaseCheckService.class);
    @Autowired
    EipDaoService eipDaoService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.error("***************************databse check***********************");

    }

}
