package com.inspur.eipatomapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication(scanBasePackages = {"com"})
@ServletComponentScan
public class EipAtomApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EipAtomApiApplication.class, args);
    }
}
