package com.inspur.eipatomapi.config.keycloak;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.client.KeycloakRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Profile(value = "dev")
@KeycloakConfiguration
public class SecurityConfigDev extends WebSecurityConfigurerAdapter {

    @Bean
    public KeycloakRestTemplate getKeycloakRestTemplate() {
        return new KeycloakRestTemplate(new KeycloakClientRequestFactoryDev());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.authorizeRequests()
//                .antMatchers("/**").hasRole("USER") // 开启后，只要拥有USER角色，就可以访问所有URL

                .anyRequest().permitAll();
        http.csrf().disable();// gaochuanji 20180330
    }


}
