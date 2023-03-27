package com.pocketcombats.admin.conf;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableConfigurationProperties(JpaAdminProperties.class)
@ComponentScan("com.pocketcombats.admin")
public class JpaAdminAutoConfiguration {
}
