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
import static io.meeds.wom.api.utils.JsonUtils.toJsonString;

import java.io.InputStream;
import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.Hub;
import io.meeds.wom.api.model.HubReport;
import io.meeds.wom.api.model.HubReportVerifiableData;
import io.meeds.wom.api.model.HubUpdateRequest;
import io.meeds.wom.api.model.WomConnectionRequest;
import io.meeds.wom.api.model.WomConnectionResponse;
import io.meeds.wom.api.model.WomDisconnectionRequest;

@Component
public class WomClientService {

  private static final String  WOM_HUBS_URI                  = "/api/hubs";

  private static final String  WOM_REPORTS_URI               = "/api/hub/reports";

  private static final String  HUB_ADDRESS_PARAM             = "{hubAddress}";

  private static final String  HUB_FORCE_REFRESH_PARAM       = "{forceRefresh}";

  private static final String  MANAGER_ADDRESS_PARAM         = "{address}";

  private static final String  NFT_ID_PARAM                  = "{nftId}";

  private static final String  HASH_PARAM                    = "{hash}";

  private static final String  WOM_CONNECT_URI               = WOM_HUBS_URI;

  private static final String  WOM_DISCONNECT_URI            = WOM_HUBS_URI;

  private static final String  WOM_UPDATE_HUB_URI            = WOM_HUBS_URI;

  private static final String  HUB_TENANT_BY_ADDRESS_URI     = WOM_HUBS_URI + "/" + HUB_ADDRESS_PARAM + "?forceRefresh=" +
      HUB_FORCE_REFRESH_PARAM;

  private static final String  HUB_AVATAR_BY_ADDRESS_URI     = WOM_HUBS_URI + "/" + HUB_ADDRESS_PARAM + "/avatar";

  private static final String  HUB_BANNER_BY_ADDRESS_URI     = WOM_HUBS_URI + "/" + HUB_ADDRESS_PARAM + "/banner";

  private static final String  HUB_MANAGER_CHEK_URI          = WOM_HUBS_URI + "/manager?nftId=" + NFT_ID_PARAM + "&address=" +
      MANAGER_ADDRESS_PARAM;

  private static final String  TOKEN_GENERATION_URI          = WOM_HUBS_URI + "/token";

  private static final String  WOM_REWARD_REPORT_URI         = WOM_REPORTS_URI;

  private static final String  WOM_REWARD_REPORT_BY_HASH_URI = WOM_REPORTS_URI + "/" + HASH_PARAM;

  private static final String  WOM_URL                       = System.getProperty("meeds.wom.url", "https://wom.meeds.io");

  @Autowired
  private WomConnectionService womConnectionService;

  public boolean isDeedManager(String address, long nftId) throws WomException {
    String responseText = womConnectionService.processGet(getIsHubManagerUri(address, nftId));
    return StringUtils.equals("true", responseText);
  }

  public Hub getHub(String hubAddress, boolean forceRefresh) throws WomException {
    String responseText = womConnectionService.processGet(getDeedHubTenantUri(hubAddress, forceRefresh));
    return fromJsonString(responseText, Hub.class);
  }

  public String generateToken() throws WomException {
    return womConnectionService.processGet(getTokenGenerationUri());
  }

  public WomConnectionResponse connectToWom(WomConnectionRequest connectionRequest) throws WomException {
    String responseText = womConnectionService.processPost(getWoMConnectionUri(), toJsonString(connectionRequest));
    return fromJsonString(responseText, WomConnectionResponse.class);
  }

  public String disconnectFromWom(WomDisconnectionRequest disconnectionRequest) throws WomException {
    return womConnectionService.processDelete(getWoMDisonnectionUri(), toJsonString(disconnectionRequest));
  }

  public HubReport saveReport(HubReportVerifiableData reportRequest) throws WomException {
    String responseText = womConnectionService.processPost(getWoMReportUri(), toJsonString(reportRequest));
    return fromJsonString(responseText, HubReport.class);
  }

  public HubReport retrieveReport(long reportId) throws WomException {
    String responseText = womConnectionService.processGet(getWoMReportUri(reportId));
    return fromJsonString(responseText, HubReport.class);
  }

  public String saveHub(Hub hub,
                        String hubSignedMessage,
                        String token) throws WomException {
    return womConnectionService.processPut(getWoMUpdateHubUri(),
                                           toJsonString(new HubUpdateRequest(hub.getAddress(),
                                                                             hub.getName(),
                                                                             hub.getDescription(),
                                                                             hub.getUrl(),
                                                                             hub.getColor(),
                                                                             hubSignedMessage,
                                                                             token)));
  }

  public void saveHubAvatar(String hubAddress,
                            String signedMessage,
                            String token,
                            InputStream inputStream) throws WomException {
    saveHubAttachment(getSaveAvatarUri(hubAddress), hubAddress, signedMessage, token, token, inputStream);
  }

  public void saveHubBanner(String hubAddress,
                            String signedMessage,
                            String rawMessage,
                            String token,
                            InputStream inputStream) throws WomException {
    saveHubAttachment(getSaveBannerUri(hubAddress), hubAddress, signedMessage, rawMessage, token, inputStream);
  }

  public String getWomUrl() {
    return WOM_URL;
  }

  private void saveHubAttachment(URI attachmentUri,
                                 String hubAddress,
                                 String signedMessage,
                                 String rawMessage,
                                 String token,
                                 InputStream inputStream) throws WomException {
    HttpPost httpPost = new HttpPost(attachmentUri);
    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
                                                                 .addBinaryBody("file",
                                                                                inputStream,
                                                                                ContentType.MULTIPART_FORM_DATA,
                                                                                "file")
                                                                 .addTextBody("hubAddress", hubAddress)
                                                                 .addTextBody("signedMessage", signedMessage)
                                                                 .addTextBody("rawMessage", rawMessage)
                                                                 .addTextBody("token", token);
    httpPost.setEntity(entityBuilder.build());
    womConnectionService.processRequest(httpPost);
  }

  private URI getSaveAvatarUri(String hubAddress) {
    String uri = WOM_URL + HUB_AVATAR_BY_ADDRESS_URI;
    return URI.create(fixUri(uri).replace(HUB_ADDRESS_PARAM, hubAddress));
  }

  private URI getSaveBannerUri(String hubAddress) {
    String uri = WOM_URL + HUB_BANNER_BY_ADDRESS_URI;
    return URI.create(fixUri(uri).replace(HUB_ADDRESS_PARAM, hubAddress));
  }

  private URI getWoMConnectionUri() {
    String uri = WOM_URL + WOM_CONNECT_URI;
    return URI.create(fixUri(uri));
  }

  private URI getWoMUpdateHubUri() {
    String uri = WOM_URL + WOM_UPDATE_HUB_URI;
    return URI.create(fixUri(uri));
  }

  private URI getWoMReportUri() {
    String uri = WOM_URL + WOM_REWARD_REPORT_URI;
    return URI.create(fixUri(uri));
  }

  private URI getWoMReportUri(long reportId) {
    String uri = WOM_URL + WOM_REWARD_REPORT_BY_HASH_URI;
    return URI.create(fixUri(uri).replace(HASH_PARAM, String.valueOf(reportId)));
  }

  private URI getWoMDisonnectionUri() {
    String uri = WOM_URL + WOM_DISCONNECT_URI;
    return URI.create(fixUri(uri));
  }

  private URI getIsHubManagerUri(String address, long nftId) {
    String uri = WOM_URL + HUB_MANAGER_CHEK_URI;
    return URI.create(fixUri(uri).replace(NFT_ID_PARAM, String.valueOf(nftId))
                                 .replace(MANAGER_ADDRESS_PARAM, address));
  }

  private URI getDeedHubTenantUri(String hubAddress, boolean forceRefresh) {
    String uri = WOM_URL + HUB_TENANT_BY_ADDRESS_URI;
    return URI.create(fixUri(uri).replace(HUB_ADDRESS_PARAM, String.valueOf(hubAddress))
                                 .replace(HUB_FORCE_REFRESH_PARAM, String.valueOf(forceRefresh)));
  }

  private URI getTokenGenerationUri() {
    String uri = WOM_URL + TOKEN_GENERATION_URI;
    return URI.create(fixUri(uri));
  }

  private String fixUri(String uri) {
    return uri.replace("//", "/")
              .replace(":/", "://");
  }

}
