package com.wkken.rainanalysis;

import cn.hutool.core.lang.Console;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class InitConfig implements InitializingBean {
@Value("${isDebug}")
private  boolean isDebug=false;

@Value("${hours}")
private int hours=2;

@Override
public void afterPropertiesSet() throws Exception {
    ResultHolder.INSTANCE.setDebug(isDebug);
    ResultHolder.INSTANCE.setHours(hours);
}
}
