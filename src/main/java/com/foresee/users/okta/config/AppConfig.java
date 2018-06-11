package com.foresee.users.okta.config;

import com.foresee.okta.util.OktaUserUtil;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = {
        "com.foresee.users.okta.client"})
public class AppConfig {

    @Bean
    public OktaUserUtil oktaUserUtil() {
        return new OktaUserUtil();
    }
}
