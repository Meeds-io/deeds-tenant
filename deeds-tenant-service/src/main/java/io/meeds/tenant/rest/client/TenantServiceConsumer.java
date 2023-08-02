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
import java.util.Iterator;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;

import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;
import org.exoplatform.ws.frameworks.json.impl.ObjectBuilder;
import org.exoplatform.ws.frameworks.json.value.JsonValue;

import io.meeds.deeds.model.Hub;
import io.meeds.deeds.model.WomConnectionRequest;
import io.meeds.deeds.model.WomDisconnectionRequest;
import io.meeds.tenant.constant.WomConnectionException;

public class TenantServiceConsumer {

  private static final String HUB_TENANT_BY_ADDRESS_URI = "/api/hubs/{hubAddress}";

  private static final String HUB_TENANT_BY_NFT_URI     = "/api/hubs/byNftId/{nftId}";

  private static final String HUB_MANAGER_CHEK_URI      = "/api/hubs/manager?nftId={nftId}&address={address}";

  private static final String WOM_CONNECT_URI           = "/api/hubs/connect";

  private static final String WOM_DISCONNECT_URI        = "/api/hubs/disconnect";

  private static final String TOKEN_GENERATION_URI      = "/api/hubs/token";

  private static final int    MAX_POOL_CONNECTIONS      = Integer.parseInt(System.getProperty("meeds.http.clientPool.max", "5"));

  private static final String WOM_URL                   = System.getProperty("meeds.wom.url", "https://wom.meeds.io");

  private HttpClient          client;

  public boolean isDeedManager(String address, long nftId) throws WomConnectionException {
    String responseText = processGet(getIsHubManagerUri(address, nftId));
    return StringUtils.equals("true", responseText);
  }

  public Hub getHub(long nftId) throws WomConnectionException {
    String responseText = processGet(getDeedHubTenantUri(nftId));
    return fromJsonString(responseText, Hub.class);
  }

  public Hub getHub(String hubAddress) throws WomConnectionException {
    String responseText = processGet(getDeedHubTenantUri(hubAddress));
    return fromJsonString(responseText, Hub.class);
  }

  public String generateToken() throws WomConnectionException {
    return processGet(getTokenGenerationUri());
  }

  public String connectToWoM(WomConnectionRequest connectionRequest) throws WomConnectionException {
    return processPost(getWoMConnectionUri(), toJsonString(connectionRequest));
  }

  public String disconnectFromWoM(WomDisconnectionRequest disconnectionRequest) throws WomConnectionException {
    return processPost(getWoMDisonnectionUri(), toJsonString(disconnectionRequest));
  }

  private URI getWoMConnectionUri() {
    String uri = WOM_URL + WOM_CONNECT_URI;
    return URI.create(uri.replace("//", "/")
                         .replace(":/", "://"));
  }

  private URI getWoMDisonnectionUri() {
    String uri = WOM_URL + WOM_DISCONNECT_URI;
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
    String uri = WOM_URL + HUB_TENANT_BY_NFT_URI;
    return URI.create(uri.replace("//", "/")
                         .replace(":/", "://")
                         .replace("{nftId}", String.valueOf(nftId)));
  }

  private URI getDeedHubTenantUri(String hubAddress) {
    String uri = WOM_URL + HUB_TENANT_BY_ADDRESS_URI;
    return URI.create(uri.replace("//", "/")
                         .replace(":/", "://")
                         .replace("{hubAddress}", String.valueOf(hubAddress)));
  }

  private URI getTokenGenerationUri() {
    String uri = WOM_URL + TOKEN_GENERATION_URI;
    return URI.create(uri.replace("//", "/")
                         .replace(":/", "://"));
  }

  private String processGet(URI uri) throws WomConnectionException {
    HttpClient httpClient = getHttpClient();
    HttpGet request = new HttpGet(uri);
    try {
      return processRequest(httpClient, request);
    } catch (IOException e) {
      throw new WomConnectionException("wom.connectionError", e);
    }
  }

  private String processPost(URI uri, String jsonString) throws WomConnectionException {
    HttpClient httpClient = getHttpClient();
    HttpPost request = new HttpPost(uri);
    StringEntity entity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
    try {
      request.setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
      request.setEntity(entity);
      return processRequest(httpClient, request);
    } catch (IOException e) {
      throw new WomConnectionException("wom.connectionError", e);
    }
  }

  private String processRequest(HttpClient httpClient, HttpRequestBase request) throws IOException, WomConnectionException {
    HttpResponse response = httpClient.execute(request);
    boolean isSuccess = response != null
        && (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300);
    if (isSuccess) {
      return processSuccessResponse(response);
    } else if (response != null && response.getStatusLine().getStatusCode() == 404) {
      return null;
    } else {
      processErrorResponse(response);
      return null;
    }
  }

  private String processSuccessResponse(HttpResponse response) throws IOException {
    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
        && response.getEntity() != null
        && response.getEntity().getContentLength() != 0) {
      try (InputStream is = response.getEntity().getContent()) {
        return IOUtils.toString(is, StandardCharsets.UTF_8);
      }
    } else {
      return null;
    }
  }

  private void processErrorResponse(HttpResponse response) throws WomConnectionException, IOException {
    if (response == null) {
      throw new WomConnectionException("wom.noResponse");
    } else if (response.getEntity() != null) {
      try (InputStream is = response.getEntity().getContent()) {
        String errorMessage = IOUtils.toString(is, StandardCharsets.UTF_8);
        if (StringUtils.contains(errorMessage, "wom.")) {
          throw new WomConnectionException(errorMessage);
        } else {
          throw new WomConnectionException("wom.errorResponse:" + errorMessage);
        }
      }
    } else {
      throw new WomConnectionException("wom.errorResponse:" + response.getStatusLine().getStatusCode());
    }
  }

  private HttpClient getHttpClient() {
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

  private HttpClientConnectionManager getClientConnectionManager() {
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
      JsonValue object = jsonDefaultHandler.getJsonObject();
      // Workaround for missing java.time.Instant date Type mappings
      Iterator<String> keys = object.getKeys();
      while (keys.hasNext()) {
        String key = keys.next();
        if (StringUtils.containsIgnoreCase(key, "date")) {
          keys.remove();
        }
      }
      return ObjectBuilder.createObject(resultClass, object);
    } catch (JsonException e) {
      throw new IllegalStateException("Error creating object from string : " + value, e);
    }
  }

  private String toJsonString(Object object) {
    try {
      return new JsonGeneratorImpl().createJsonObject(object).toString();
    } catch (JsonException e) {
      throw new IllegalStateException("Error parsing object to string " + object, e);
    }
  }

}
