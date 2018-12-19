package com.inspur.eipatomapi.config;

import com.inspur.eipatomapi.service.EipDaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;


@Service
@Order(3)
@Slf4j
public class DatabaseCheckService implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args)  {
        log.info("***************************init finish***********************");

    }

}
