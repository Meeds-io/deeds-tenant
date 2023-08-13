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

import static io.meeds.deeds.api.utils.JsonUtils.fromJsonString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.deeds.api.constant.WomException;
import io.meeds.deeds.api.model.WomErrorMessage;

public class WomConnectionService {

  private static final Log LOG                  = ExoLogger.getLogger(WomConnectionService.class);

  private static final int MAX_POOL_CONNECTIONS = Integer.parseInt(System.getProperty("meeds.http.clientPool.max", "5"));

  private HttpClient       client;

  public String processGet(URI uri) throws WomException {
    try {
      return processRequest(new HttpGet(uri));
    } catch (IOException e) {
      throw new WomException("wom.connectionError", e);
    }
  }

  public String processPost(URI uri, String jsonString) throws WomException {
    HttpPost request = new HttpPost(uri);
    StringEntity entity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
    try {
      request.setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
      request.setEntity(entity);
      return processRequest(request);
    } catch (IOException e) {
      throw new WomException("wom.connectionError", e);
    }
  }

  public String processDelete(URI uri, String jsonString) throws WomException {
    HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() {
      @Override
      public String getMethod() {
        return HttpDelete.METHOD_NAME;
      }
    };
    request.setURI(uri);
    StringEntity entity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
    try {
      request.setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
      request.setEntity(entity);
      return processRequest(request);
    } catch (IOException e) {
      throw new WomException("wom.connectionError", e);
    }
  }

  public String processRequest(HttpRequestBase request) throws IOException, WomException {
    HttpResponse response = getHttpClient().execute(request);
    boolean isSuccess = response != null
        && (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300);
    if (isSuccess) {
      return processSuccessResponse(response);
    } else if (response != null && response.getStatusLine().getStatusCode() == 404) {
      return null;
    } else {
      processErrorResponse(response, request);
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

  private void processErrorResponse(HttpResponse response, HttpRequestBase request) throws WomException, IOException { // NOSONAR
    if (response == null) {
      throw new WomException("wom.noResponse");
    } else if (response.getEntity() != null) {
      try (InputStream is = response.getEntity().getContent()) {
        String errorMessage = IOUtils.toString(is, StandardCharsets.UTF_8);
        if (StringUtils.contains(errorMessage, "wom.")) {
          if (StringUtils.contains(errorMessage, "{")) {
            try {
              throw new WomException(fromJsonString(errorMessage, WomErrorMessage.class));
            } catch (WomException e) {
              throw e;
            } catch (Exception e) {
              LOG.warn("Error parsing message '{}', throw the original error as it is", errorMessage, e);
            }
          }
          throw new WomException(errorMessage);
        } else {
          throw new WomException(String.format("wom.errorResponse: %s. URL = %s %s",
                                               errorMessage,
                                               request.getMethod(),
                                               request.getURI()));
        }
      }
    } else {
      throw new WomException("wom.errorResponse:" + response.getStatusLine().getStatusCode());
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

}
