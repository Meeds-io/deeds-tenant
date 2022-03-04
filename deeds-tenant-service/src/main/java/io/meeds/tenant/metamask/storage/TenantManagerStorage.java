/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
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
package io.meeds.tenant.metamask.storage;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.search.es.client.*;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

/**
 * A service that allows to detect Deed Tenant Manager address
 */
public class TenantManagerStorage extends ElasticClient {

  private static final String      DEFAULT_ES_INDEX_NAME                      = "deed_tenant_manager";

  private static final String      ES_INDEX_CLIENT_PROPERTY_NAME              = "es.url";

  private static final String      ES_INDEX_CLIENT_PROPERTY_USERNAME          = "es.username";

  private static final String      ES_INDEX_CLIENT_PROPERTY_PASSWORD          = "es.password";

  private static final String      ES_INDEX_CLIENT_PROPERTY_INDEX             = "es.index";

  private static final String      FALLBACK_ES_INDEX_CLIENT_PROPERTY_NAME     = "exo.es.index.server.url";

  private static final String      FALLBACK_ES_INDEX_CLIENT_PROPERTY_USERNAME = "exo.es.index.server.username";

  private static final String      FALLBACK_ES_INDEX_CLIENT_PROPERTY_PASSWORD = "exo.es.index.server.password";

  private static final String      STATUS_CODE_PARAM                          = "status";

  private String                   esIndexName;

  private String                   esUsername;

  private String                   esPassword;

  private ExoCache<String, String> deedPropertiesCache;

  public TenantManagerStorage(CacheService cacheService,
                              ElasticIndexingAuditTrail elasticIndexingAuditTrail,
                              InitParams params) {
    super(elasticIndexingAuditTrail);
    this.esIndexName = getParamValue(params, ES_INDEX_CLIENT_PROPERTY_INDEX);
    if (StringUtils.isBlank(this.esIndexName)) {
      this.esIndexName = DEFAULT_ES_INDEX_NAME;
    }
    this.esUsername = getParamValue(params, ES_INDEX_CLIENT_PROPERTY_USERNAME);
    this.esPassword = getParamValue(params, ES_INDEX_CLIENT_PROPERTY_PASSWORD);

    String esUrl = getParamValue(params, ES_INDEX_CLIENT_PROPERTY_NAME);
    if (StringUtils.isBlank(esUrl)) {
      // Fallback to Default ES configuration
      esUrl = PropertyManager.getProperty(FALLBACK_ES_INDEX_CLIENT_PROPERTY_NAME);
      this.esUsername = PropertyManager.getProperty(FALLBACK_ES_INDEX_CLIENT_PROPERTY_USERNAME);
      this.esPassword = PropertyManager.getProperty(FALLBACK_ES_INDEX_CLIENT_PROPERTY_PASSWORD);
    }
    if (StringUtils.isNotBlank(esUrl)) {
      this.urlClient = esUrl;
    }

    this.deedPropertiesCache = cacheService.getCacheInstance("deed.properties");
    initHttpClient();
  }

  @Override
  protected String getEsUsernameProperty() {
    return esUsername;
  }

  @Override
  protected String getEsPasswordProperty() {
    return esPassword;
  }

  @Override
  protected HttpClientConnectionManager getClientConnectionManager() {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setDefaultMaxPerRoute(getMaxConnections());
    return connectionManager;
  }

  public String getManagerAddress(String nftId) {
    return computeDeedProperties(nftId, "managerAddress");
  }

  public String getCityIndex(String nftId) {
    return computeDeedProperties(nftId, "cityIndex");
  }

  public String getCardType(String nftId) {
    return computeDeedProperties(nftId, "cardType");
  }

  private String computeDeedProperties(String nftId, String key) {
    String cacheKey = key + "-" + nftId;
    if (deedPropertiesCache.get(cacheKey) == null) {
      Map<String, String> deedProperties = getDeedProperties(nftId);
      deedProperties.forEach((propKey, propValue) -> deedPropertiesCache.put(propKey + "-" + nftId, propValue));
    }
    return deedPropertiesCache.get(cacheKey);
  }

  /**
   * Changes the Tenant Status in ES
   * 
   * @param nftId Deed NFT identifier
   * @param tenantStatus Tenant Status: UP or DOWN
   */
  @SuppressWarnings("unchecked")
  public void setTenantStatus(String nftId, String tenantStatus) {
    JSONObject patchProperties = new JSONObject();
    if (StringUtils.isNotBlank(tenantStatus)) {
      patchProperties.put("tenantStatus", tenantStatus);
    }
    JSONObject doc = new JSONObject();
    doc.put("doc", patchProperties);
    updateDeedProperties(nftId, doc.toJSONString());
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> getDeedProperties(String nftId) {
    long startTime = System.currentTimeMillis();
    StringBuilder url = new StringBuilder(urlClient)
                                                    .append("/" + this.esIndexName)
                                                    .append("/_doc")
                                                    .append("/")
                                                    .append(nftId);
    ElasticResponse elasticResponse = sendHttpGetRequest(url.toString());
    String response = elasticResponse.getMessage();
    int statusCode = elasticResponse.getStatusCode();
    if (StringUtils.isBlank(response)) {
      auditTrail.logRejectedSearchOperation(ElasticIndexingAuditTrail.SEARCH_INDEX,
                                            esIndexName,
                                            statusCode,
                                            response,
                                            (System.currentTimeMillis() - startTime));
      return Collections.emptyMap();
    } else {
      JSONParser parser = new JSONParser();
      JSONObject json = null;
      try {
        json = (JSONObject) parser.parse(response);
      } catch (ParseException e) {
        throw new IllegalStateException("Error occured while requesting ES HTTP code: '" + statusCode
            + "', Error parsing response to JSON format, content = '" + response + "'", e);
      }
      Long status = json.get(STATUS_CODE_PARAM) == null ? null : (Long) json.get(STATUS_CODE_PARAM);
      Integer httpStatusCode = status == null ? null : status.intValue();
      if (ElasticIndexingAuditTrail.isError(httpStatusCode)) {
        throw new IllegalStateException("Error occured while requesting ES HTTP error code: '" + statusCode
            + "', HTTP response: '"
            + response + "'");
      }
      if (json.containsKey("found") && Boolean.TRUE.equals(json.get("found"))) {
        JSONObject deedProperties = (JSONObject) json.get("_source");
        Map<String, String> properties = new HashMap<>();
        deedProperties.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
        return properties;
      } else {
        return Collections.emptyMap();
      }
    }
  }

  private void updateDeedProperties(String nftId, String esQuery) {
    long startTime = System.currentTimeMillis();
    StringBuilder url = new StringBuilder(urlClient)
                                                    .append("/" + this.esIndexName)
                                                    .append("/_update")
                                                    .append("/")
                                                    .append(nftId);
    ElasticResponse elasticResponse = sendHttpPostRequest(url.toString(), esQuery);
    String response = elasticResponse.getMessage();
    int statusCode = elasticResponse.getStatusCode();
    if (StringUtils.isBlank(response)) {
      auditTrail.logRejectedSearchOperation(ElasticIndexingAuditTrail.SEARCH_INDEX,
                                            esIndexName,
                                            statusCode,
                                            response,
                                            (System.currentTimeMillis() - startTime));
    } else {
      JSONParser parser = new JSONParser();
      JSONObject json = null;
      try {
        json = (JSONObject) parser.parse(response);
      } catch (ParseException e) {
        throw new IllegalStateException("Error occured while requesting ES HTTP code: '" + statusCode
            + "', Error parsing response to JSON format, content = '" + response + "'", e);
      }
      Long status = json.get(STATUS_CODE_PARAM) == null ? null : (Long) json.get(STATUS_CODE_PARAM);
      Integer httpStatusCode = status == null ? null : status.intValue();
      if (ElasticIndexingAuditTrail.isError(httpStatusCode)) {
        throw new IllegalStateException("Error occured while requesting ES HTTP error code: '" + statusCode
            + "', HTTP response: '"
            + response + "'");
      }
    }
  }

  private String getParamValue(InitParams params, String paramName) {
    if (params != null && params.containsKey(paramName)) {
      return params.getValueParam(paramName).getValue();
    }
    return null;
  }

}
