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

import static io.meeds.deeds.utils.JsonUtils.fromJsonString;
import static io.meeds.deeds.utils.JsonUtils.toJsonString;

import java.net.URI;

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
import org.springframework.stereotype.Component;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.deeds.constant.WomException;
import io.meeds.deeds.model.Hub;
import io.meeds.deeds.model.HubRewardReportRequest;
import io.meeds.deeds.model.HubRewardReportStatus;
import io.meeds.deeds.model.WomConnectionRequest;
import io.meeds.deeds.model.WomDisconnectionRequest;

@Component
public class WoMServiceClient {

  private static final String  WOM_CONNECT_URI               = "/api/hubs";

  private static final String  WOM_DISCONNECT_URI            = "/api/hubs";

  private static final String  HUB_TENANT_BY_ADDRESS_URI     = "/api/hubs/{hubAddress}";

  private static final String  HUB_TENANT_BY_NFT_URI         = "/api/hubs/byNftId/{nftId}";

  private static final String  HUB_MANAGER_CHEK_URI          = "/api/hubs/manager?nftId={nftId}&address={address}";

  private static final String  TOKEN_GENERATION_URI          = "/api/hubs/token";

  private static final String  WOM_REWARD_REPORT_URI         = "/api/hub/reports";

  private static final String  WOM_REWARD_REPORT_BY_HASH_URI = "/api/hub/reports/{hash}";

  private static final String  WOM_URL                       = System.getProperty("meeds.wom.url", "https://wom.meeds.io");

  private WomConnectionService womConnectionService;

  public WoMServiceClient(WomConnectionService womConnectionService) {
    this.womConnectionService = womConnectionService;
  }

  public boolean isDeedManager(String address, long nftId) throws WomException {
    String responseText = womConnectionService.processGet(getIsHubManagerUri(address, nftId));
    return StringUtils.equals("true", responseText);
  }

  public Hub getHub(long nftId) throws WomException {
    String responseText = womConnectionService.processGet(getDeedHubTenantUri(nftId));
    return fromJsonString(responseText, Hub.class);
  }

  public Hub getHub(String hubAddress) throws WomException {
    String responseText = womConnectionService.processGet(getDeedHubTenantUri(hubAddress));
    return fromJsonString(responseText, Hub.class);
  }

  public String generateToken() throws WomException {
    return womConnectionService.processGet(getTokenGenerationUri());
  }

  public String connectToWoM(WomConnectionRequest connectionRequest) throws WomException {
    return womConnectionService.processPost(getWoMConnectionUri(), toJsonString(connectionRequest));
  }

  public String disconnectFromWoM(WomDisconnectionRequest disconnectionRequest) throws WomException {
    return womConnectionService.processDelete(getWoMDisonnectionUri(), toJsonString(disconnectionRequest));
  }

  public HubRewardReportStatus sendReportToWoM(HubRewardReportRequest hubRewardReportRequest) throws WomException {
    String responseText = womConnectionService.processPost(getWoMRewardReportUri(), toJsonString(hubRewardReportRequest));
    return fromJsonString(responseText, HubRewardReportStatus.class);
  }

  public HubRewardReportStatus getRewardReportStatus(String hash) throws WomException {
    String responseText = womConnectionService.processGet(getWoMRewardReportUri(hash));
    return fromJsonString(responseText, HubRewardReportStatus.class);
  }

  private URI getWoMConnectionUri() {
    String uri = WOM_URL + WOM_CONNECT_URI;
    return URI.create(uri.replace("//", "/")
                         .replace(":/", "://"));
  }

  private URI getWoMRewardReportUri() {
    String uri = WOM_URL + WOM_REWARD_REPORT_URI;
    return URI.create(uri.replace("//", "/")
                         .replace(":/", "://"));
  }

  private URI getWoMRewardReportUri(String hash) {
    String uri = WOM_URL + WOM_REWARD_REPORT_BY_HASH_URI;
    return URI.create(uri.replace("//", "/")
                         .replace(":/", "://")
                         .replace("{hash}", hash));
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

}
