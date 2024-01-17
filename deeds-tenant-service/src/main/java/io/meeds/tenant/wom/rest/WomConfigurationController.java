/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
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
package io.meeds.tenant.wom.rest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.Hub;
import io.meeds.wom.api.model.WomConnectionRequest;
import io.meeds.wom.api.model.WomDisconnectionRequest;
import io.meeds.tenant.wom.model.HubConfiguration;
import io.meeds.tenant.wom.service.WomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("wom")
@Tag(name = "wom", description = "An endpoint to manage current Hub as a WoM member")
public class WomConfigurationController {

  private static final Log LOG = ExoLogger.getLogger(WomConfigurationController.class);

  @Autowired
  private WomService       womService;

  @GetMapping
  @Secured("rewarding")
  @Operation(summary = "Retrieves Deed NFT properties",
             method = "GET")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  @ApiResponse(responseCode = "404", description = "Not found")
  @ApiResponse(responseCode = "503", description = "Service unavailable")
  public Hub getHub(
                    @Parameter(description = "Deed NFT identifier", required = false)
                    @RequestParam(name = "nftId", required = false)
                    String nftId) {
    try {
      Hub hub = StringUtils.isBlank(nftId) ? womService.getHub() : womService.getHub(Long.parseLong(nftId));
      if (hub == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      } else {
        return hub;
      }
    } catch (WomException e) {
      logWomException(e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
  }

  @GetMapping("configuration")
  @Secured("rewarding")
  @Operation(summary = "Retrieves Hub configuration properties", method = "GET")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  public HubConfiguration getConfiguration() {
    return womService.getConfiguration();
  }

  @GetMapping("token")
  @Secured("rewarding")
  @Operation(summary = "Retrieves Deed NFT properties", method = "GET")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  @ApiResponse(responseCode = "503", description = "Service unavailable")
  public String generateWoMToken() {
    try {
      return womService.generateWoMToken();
    } catch (WomException e) {
      logWomException(e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
  }

  @GetMapping("manager")
  @Secured("rewarding")
  @Operation(summary = "Checks whether a wallet is the provisioning manager of a Deed or not", method = "GET")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  @ApiResponse(responseCode = "503", description = "Service unavailable")
  public boolean isDeedManager(
                               @Parameter(description = "Wallet address", required = true)
                               @RequestParam("address")
                               String address,
                               @Parameter(description = "Deed NFT identifier", required = true)
                               @RequestParam("nftId")
                               long nftId) {
    try {
      return womService.isDeedManager(address, nftId);
    } catch (WomException e) {
      logWomException(e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
  }

  @PostMapping("connect")
  @Secured("rewarding")
  @Operation(summary = "Connect current Hub to the WoM", method = "POST")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  @ApiResponse(responseCode = "400", description = "Bad request")
  @ApiResponse(responseCode = "503", description = "Service Unavailable")
  public String connectToWoM(
                             @Parameter(description = "WoM connection request data", required = true)
                             @RequestBody
                             WomConnectionRequest connectionRequest) {
    if (connectionRequest == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wom.emptyConnectionRequest");
    } else if (connectionRequest.getDeedId() < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wom.emptyDeedId");
    } else if (connectionRequest.getDeedManagerAddress() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wom.emptyDeedManagerAddress");
    } else if (connectionRequest.getEarnerAddress() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wom.emptyEarnerAddress");
    } else if (connectionRequest.getSignedMessage() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wom.emptySignedMessage");
    } else if (connectionRequest.getToken() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wom.emptyTokenForSignedMessage");
    }
    try {
      return womService.connectToWoM(connectionRequest);
    } catch (WomException e) {
      logWomException(e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
  }

  @PostMapping("disconnect")
  @Secured("rewarding")
  @Operation(summary = "Disconnect current Hub from the WoM", method = "POST")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  @ApiResponse(responseCode = "400", description = "Bad request")
  @ApiResponse(responseCode = "503", description = "Service Unavailable")
  public void disconnectFromWoM(
                                @Parameter(description = "WoM disconnection request data", required = true)
                                @RequestBody
                                WomDisconnectionRequest disconnectionRequest) {
    if (disconnectionRequest == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wom.emptyConnectionRequest");
    } else if (disconnectionRequest.getDeedManagerAddress() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wom.emptyDeedManagerAddress");
    } else if (disconnectionRequest.getSignedMessage() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wom.emptySignedMessage");
    } else if (disconnectionRequest.getToken() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wom.emptyTokenForSignedMessage");
    }
    try {
      womService.disconnectFromWoM(disconnectionRequest);
    } catch (WomException e) {
      logWomException(e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
  }

  @PostMapping("avatar")
  @Secured("rewarding")
  @Operation(summary = "Changes Hub Avatar in the WoM", method = "POST")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  @ApiResponse(responseCode = "400", description = "Bad request")
  @ApiResponse(responseCode = "503", description = "Service Unavailable")
  public void saveHubAvatar(
                            @Parameter(description = "Signed Message by Hub Manager", required = true)
                            @RequestParam("signedMessage")
                            String signedMessage,
                            @Parameter(description = "Raw Message used to sign by Hub Manager", required = true)
                            @RequestParam("rawMessage")
                            String rawMessage,
                            @Parameter(description = "WoM generated token used to sign by Hub Manager", required = true)
                            @RequestParam("token")
                            String token,
                            @Parameter(description = "Uploaded file resource identifier", required = true)
                            @RequestParam("uploadId")
                            String uploadId) {
    try {
      womService.saveHubAvatar(uploadId, signedMessage, rawMessage, token);
    } catch (WomException e) {
      logWomException(e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @PostMapping("banner")
  @Secured("rewarding")
  @Operation(summary = "Changes Hub Banner in the WoM", method = "POST")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  @ApiResponse(responseCode = "400", description = "Bad request")
  @ApiResponse(responseCode = "503", description = "Service Unavailable")
  public void saveHubBanner(
                            @Parameter(description = "Signed Message by Hub Manager", required = true)
                            @RequestParam("signedMessage")
                            String signedMessage,
                            @Parameter(description = "Raw Message used to sign by Hub Manager", required = true)
                            @RequestParam("rawMessage")
                            String rawMessage,
                            @Parameter(description = "WoM generated token used to sign by Hub Manager", required = true)
                            @RequestParam("token")
                            String token,
                            @Parameter(description = "Uploaded file resource identifier", required = true)
                            @RequestParam("uploadId")
                            String uploadId) {
    try {
      womService.saveHubBanner(uploadId, signedMessage, rawMessage, token);
    } catch (WomException e) {
      logWomException(e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  private void logWomException(WomException e) {
    LOG.debug(e.getErrorCode().getMessageKey(), e);
  }

}
