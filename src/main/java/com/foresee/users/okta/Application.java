package com.foresee.users.okta;

import com.foresee.users.okta.service.UserService;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(value = "com.foresee.users.okta")
@Slf4j
public class Application {

    public static void main(String[] args) throws Exception  {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Application.class)
                .web(false)
                .application()
                .run(args);

        log.debug("Starting...");
        Stopwatch stopwatch = Stopwatch.createStarted();

        ctx.getBean(UserService.class).execute();

        log.debug( "Took {} to complete", stopwatch);
        ctx.close();
        System.exit(0);
    }

}
