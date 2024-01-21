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
package io.meeds.tenant.hub.rest.client;

import static io.meeds.wom.api.utils.JsonUtils.fromJsonString;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Component;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.tenant.hub.model.WomResponse;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.WomErrorMessage;

import lombok.SneakyThrows;

@Component
public class WomConnectionService {

  private static final Log LOG                  = ExoLogger.getLogger(WomConnectionService.class);

  private static final int MAX_POOL_CONNECTIONS = Integer.parseInt(System.getProperty("meeds.http.clientPool.max", "5"));

  private HttpClient       client;

  public String processGet(URI uri) throws WomException {
    return processRequest(new HttpGet(uri));
  }

  public String processPost(URI uri, String jsonString) throws WomException {
    HttpPost request = new HttpPost(uri);
    StringEntity entity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
    request.setEntity(entity);
    return processRequest(request);
  }

  public String processDelete(URI uri, String jsonString) throws WomException {
    HttpDelete request = new HttpDelete(uri);
    StringEntity entity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
    request.setEntity(entity);
    return processRequest(request);
  }

  @SneakyThrows
  public String processRequest(HttpUriRequestBase request) throws WomException {
    WomResponse response = getHttpClient().execute(request, this::handleHttpResponse);
    boolean isSuccess = response != null
                        && (response.getCode() >= 200 && response.getCode() < 300);
    if (isSuccess) {
      return processSuccessResponse(response);
    } else if (response != null && response.getCode() == 404) {
      return null;
    } else {
      processErrorResponse(response, request);
      return null;
    }
  }

  private String processSuccessResponse(WomResponse response) {
    if (response.getCode() == HttpStatus.SC_OK
        && StringUtils.isNotBlank(response.getEntity())) {
      return response.getEntity();
    } else {
      return null;
    }
  }

  @SneakyThrows
  private void processErrorResponse(WomResponse response, HttpUriRequestBase request) throws WomException { // NOSONAR
    if (response == null) {
      throw new WomException("wom.noResponse");
    } else if (StringUtils.isNotBlank(response.getEntity())) {
      String errorMessage = response.getEntity();
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
                                             request.getUri()));
      }
    } else {
      throw new WomException("wom.errorResponse:" + response.getCode());
    }
  }

  private HttpClient getHttpClient() {
    if (client == null) {
      HttpClientConnectionManager clientConnectionManager = getClientConnectionManager();
      HttpClientBuilder httpClientBuilder = HttpClients.custom()
                                                       .setConnectionManager(clientConnectionManager)
                                                       .setConnectionReuseStrategy(new DefaultConnectionReuseStrategy());
      client = httpClientBuilder.build();
    }
    return client;
  }

  private HttpClientConnectionManager getClientConnectionManager() {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setDefaultMaxPerRoute(MAX_POOL_CONNECTIONS);
    return connectionManager;
  }

  @SneakyThrows
  private WomResponse handleHttpResponse(ClassicHttpResponse httpResponse) {
    HttpEntity entity = httpResponse.getEntity();
    return new WomResponse(httpResponse.getCode(), entity == null ? null : EntityUtils.toString(entity));
  }

}
