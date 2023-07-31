/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
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
package io.meeds.tenant.rest.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;
import org.exoplatform.ws.frameworks.json.impl.ObjectBuilder;

import io.meeds.tenant.model.DeedTenantHub;

public class TenantServiceConsumer {

  private static final Log    LOG                  = ExoLogger.getLogger(TenantServiceConsumer.class);

  private static final String HUB_MANAGER_CHEK_URI = "/api/hubs/{nftId}/{address}/manager";

  private static final String HUB_TENANT_PROPS_URI = "/api/hubs/{nftId}";

  private static final String TOKEN_GENERATION_URI = "/api/hubs/token";

  private static final int    MAX_POOL_CONNECTIONS = Integer.parseInt(System.getProperty("meeds.http.clientPool.max", "5"));

  private static final String WOM_URL              = System.getProperty("meeds.wom.url", "https://wom.meeds.io");

  private HttpClient          client;

  public boolean isDeedManager(String address, long nftId) {
    HttpClient httpClient = getHttpClient();
    URI uri = getIsHubManagerUri(address, nftId);
    HttpGet request = new HttpGet(uri);
    try {
      HttpResponse response = httpClient.execute(request);
      if (response != null
          && response.getEntity() != null
          && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        try (InputStream is = response.getEntity().getContent()) {
          return StringUtils.equals("true", IOUtils.toString(is, StandardCharsets.UTF_8));
        }
      }
    } catch (IOException e) {
      LOG.warn("Error connecting to WoM Server {}", WOM_URL, e);
    }
    return false;
  }

  public DeedTenantHub getDeedTenant(long nftId) {
    HttpClient httpClient = getHttpClient();
    URI uri = getDeedHubTenantUri(nftId);
    HttpGet request = new HttpGet(uri);
    try {
      HttpResponse response = httpClient.execute(request);
      if (response != null
          && response.getEntity() != null
          && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        try (InputStream is = response.getEntity().getContent()) {
          String content = IOUtils.toString(is, StandardCharsets.UTF_8);
          return fromJsonString(content, DeedTenantHub.class);
        }
      }
    } catch (IOException e) {
      LOG.warn("Error connecting to WoM Server {}", WOM_URL, e);
    }
    return null;
  }

  public String generateToken() {
    HttpClient httpClient = getHttpClient();
    URI uri = getTokenGenerationUri();
    HttpGet request = new HttpGet(uri);
    try {
      HttpResponse response = httpClient.execute(request);
      if (response != null
          && response.getEntity() != null
          && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        try (InputStream is = response.getEntity().getContent()) {
          return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
      }
    } catch (IOException e) {
      LOG.warn("Error connecting to WoM Server {}", WOM_URL, e);
    }
    return null;
  }

  private URI getTokenGenerationUri() {
    String uri = WOM_URL + TOKEN_GENERATION_URI;
    return URI.create(uri.replace("//", "/")
                         .replace(":/", "://"));
  }

  private URI getIsHubManagerUri(String address, long nftId) {
    String uri = WOM_URL + HUB_MANAGER_CHEK_URI;
    return URI.create(uri.replace("//", "/")
                         .replace(":/", "://")
                         .replace("{nftId}", String.valueOf(nftId))
                         .replace("{address}", address));
  }

  private URI getDeedHubTenantUri(long nftId) {
     String uri = WOM_URL + HUB_TENANT_PROPS_URI;
    return URI.create(uri.replace("//", "/")
                         .replace(":/", "://")
                         .replace("{nftId}", String.valueOf(nftId)));
  }

  protected HttpClient getHttpClient() {
    if (client == null) {
      HttpClientConnectionManager clientConnectionManager = getClientConnectionManager();
      HttpClientBuilder httpClientBuilder = HttpClients.custom()
                                                       .setConnectionManager(clientConnectionManager)
                                                       .setConnectionReuseStrategy(new DefaultConnectionReuseStrategy())
                                                       .setMaxConnPerRoute(MAX_POOL_CONNECTIONS);
      client = httpClientBuilder.build();
    }
    return client;
  }

  protected HttpClientConnectionManager getClientConnectionManager() {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setDefaultMaxPerRoute(MAX_POOL_CONNECTIONS);
    return connectionManager;
  }

  private <T> T fromJsonString(String value, Class<T> resultClass) {
    try {
      if (StringUtils.isBlank(value)) {
        return null;
      }
      JsonDefaultHandler jsonDefaultHandler = new JsonDefaultHandler();
      new JsonParserImpl().parse(new ByteArrayInputStream(value.getBytes()), jsonDefaultHandler);
      return ObjectBuilder.createObject(resultClass, jsonDefaultHandler.getJsonObject());
    } catch (JsonException e) {
      throw new IllegalStateException("Error creating object from string : " + value, e);
    }
  }

}
