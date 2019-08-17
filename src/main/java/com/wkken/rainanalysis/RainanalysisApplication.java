package com.wkken.rainanalysis;

import cn.hutool.core.lang.Console;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan("com.wkken")
@EnableScheduling
public class RainanalysisApplication extends SpringBootServletInitializer {

private static final Logger log = Logger.getLogger(RainanalysisApplication.class);


@Override
protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
	return application.sources(RainanalysisApplication.class);
}


	public static void main(String[] args) {
		SpringApplication.run(RainanalysisApplication.class, args);

	}
}
