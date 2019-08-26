package com.wkken.rainanalysis;

import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.activerecord.dialect.AnsiSqlDialect;
import com.jfinal.plugin.druid.DruidPlugin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "db")
public class DBConfig {
/**
 * 创建数据库连接池
 */
@Bean
public boolean createDatabaseConnectionPool() {
    DruidPlugin druidPlugin = new DruidPlugin(jdbcUrl, dbUser, dbPwd);
    ActiveRecordPlugin arp = new ActiveRecordPlugin(druidPlugin);
    arp.setDevMode(false);
    arp.setDialect(new AnsiSqlDialect());
    arp.setShowSql(false);
    druidPlugin.start();
    return arp.start();
}

private boolean debug;
private String jdbcUrl;
private String dbUser;
private String dbPwd;


public void setDebug(boolean debug) {
    this.debug = debug;
}

public void setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
}

public void setDbUser(String dbUser) {
    this.dbUser = dbUser;
}

public void setDbPwd(String dbPwd) {
    this.dbPwd = dbPwd;
}
}
