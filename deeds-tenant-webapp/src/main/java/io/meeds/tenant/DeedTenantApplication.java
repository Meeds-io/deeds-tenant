/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.tenant;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.PropertySource;

import io.meeds.spring.AvailableIntegration;
import io.meeds.spring.kernel.PortalApplicationContextInitializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

@SpringBootApplication(scanBasePackages = {
    "io.meeds.deeds",
    "io.meeds.tenant",
    AvailableIntegration.KERNEL_MODULE,
    AvailableIntegration.WEB_SECURITY_MODULE,
    AvailableIntegration.WEB_TRANSACTION_MODULE,
  }, exclude = {
    RedisAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@EnableCaching
@PropertySource("classpath:tenant.properties")
@PropertySource("classpath:application.properties")
public class DeedTenantApplication extends PortalApplicationContextInitializer {

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    // Avoid creating Deed Tenants Indexes in Deed Tenant Elasticsearch
    // When the ES is misconfigured
    System.setProperty("meeds.elasticsearch.autoCreateIndex", "false");
    // Disable ListenerService until verifying whether the tenant is included in
    // WoM or not
    System.setProperty("meeds.listenerService.enabled", "false");

    super.onStartup(servletContext);
  }

}
