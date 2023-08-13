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
package io.meeds.tenant.rest;

import static io.meeds.deeds.api.utils.JsonUtils.toJsonString;

import java.io.IOException;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.meeds.deeds.api.constant.WomException;
import io.meeds.deeds.api.constant.WomParsingException;
import io.meeds.deeds.api.model.Hub;
import io.meeds.deeds.api.model.WomConnectionRequest;
import io.meeds.deeds.api.model.WomDisconnectionRequest;
import io.meeds.tenant.service.HubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/deed/tenant")
@Tag(name = "/deed/tenant", description = "An endpoint to manage current Hub as a WoM member")
public class HubRest implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(HubRest.class);

  private HubService       hubService;

  public HubRest(HubService hubService) {
    this.hubService = hubService;
  }

  @GET
  @Path("configuration")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(summary = "Retrieves Hub configuration properties", method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public Response getConfiguration() {
    return Response.ok(hubService.getConfiguration()).build();
  }

  @GET
  @Path("token")
  @Produces(MediaType.TEXT_PLAIN)
  @RolesAllowed("rewarding")
  @Operation(summary = "Retrieves Deed NFT properties", method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "503", description = "Service unavailable"),
  })
  public Response generateWoMToken() {
    try {
      return Response.ok(hubService.generateWoMToken()).build();
    } catch (WomException e) {
      LOG.debug("Error connecting to WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(summary = "Retrieves Deed NFT properties", method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "404", description = "Not found"),
      @ApiResponse(responseCode = "503", description = "Service unavailable"),
  })
  public Response getHub(
                         @Parameter(description = "Deed NFT identifier", required = false)
                         @QueryParam("nftId")
                         String nftId) throws WomParsingException {
    Hub hub;
    if (StringUtils.isBlank(nftId)) {
      hub = hubService.getHub();
    } else {
      try {
        hub = hubService.getHub(Long.parseLong(nftId));
      } catch (WomException e) {
        LOG.debug("Error connecting to WoM Server", e);
        return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
      }
    }
    if (hub == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(toJsonString(hub)).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("manager")
  @RolesAllowed("rewarding")
  @Operation(summary = "Checks whether a wallet is the provisioning manager of a Deed or not", method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "503", description = "Service unavailable"),
  })
  public Response isDeedManager(
                                @Parameter(description = "Wallet address", required = true)
                                @QueryParam("address")
                                String address,
                                @Parameter(description = "Deed NFT identifier", required = true)
                                @QueryParam("nftId")
                                long nftId) {
    try {
      boolean isTenantManager = hubService.isDeedManager(address, nftId);
      return Response.ok(String.valueOf(isTenantManager)).build();
    } catch (WomException e) {
      LOG.debug("Error connecting to WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  @Path("connect")
  @RolesAllowed("rewarding")
  @Operation(summary = "Connect current Hub to the WoM", method = "POST")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "503", description = "Service Unavailable"),
  })
  public Response connectToWoM(
                               @Parameter(description = "WoM connection request data", required = true)
                               WomConnectionRequest connectionRequest) {
    if (connectionRequest == null) {
      return Response.status(Status.BAD_REQUEST).entity("wom.emptyConnectionRequest").build();
    } else if (connectionRequest.getDeedId() < 0) {
      return Response.status(Status.BAD_REQUEST).entity("wom.emptyDeedId").build();
    } else if (connectionRequest.getDeedManagerAddress() == null) {
      return Response.status(Status.BAD_REQUEST).entity("wom.emptyDeedManagerAddress").build();
    } else if (connectionRequest.getEarnerAddress() == null) {
      return Response.status(Status.BAD_REQUEST).entity("wom.emptyEarnerAddress").build();
    } else if (connectionRequest.getSignedMessage() == null) {
      return Response.status(Status.BAD_REQUEST).entity("wom.emptySignedMessage").build();
    } else if (connectionRequest.getToken() == null) {
      return Response.status(Status.BAD_REQUEST).entity("wom.emptyTokenForSignedMessage").build();
    }
    try {
      String hubAddress = hubService.connectToWoM(connectionRequest);
      return Response.ok(hubAddress).build();
    } catch (WomException e) {
      LOG.debug("Error connecting to WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("avatar")
  @RolesAllowed("rewarding")
  @Operation(summary = "Changes Hub Avatar in the WoM", method = "POST")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "503", description = "Service Unavailable"),
  })
  public Response saveHubAvatar(
                                @Parameter(description = "Signed Message by Hub Manager", required = true)
                                @FormParam("signedMessage")
                                String signedMessage,
                                @Parameter(description = "Raw Message used to sign by Hub Manager", required = true)
                                @FormParam("rawMessage")
                                String rawMessage,
                                @Parameter(description = "WoM generated token used to sign by Hub Manager", required = true)
                                @FormParam("token")
                                String token,
                                @Parameter(description = "Uploaded file resource identifier", required = true)
                                @FormParam("uploadId")
                                String uploadId) {
    try {
      hubService.saveHubAvatar(uploadId, signedMessage, rawMessage, token);
      return Response.noContent().build();
    } catch (WomException e) {
      LOG.debug("Error connecting to WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    } catch (ObjectNotFoundException e) {
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
    } catch (IOException e) {
      LOG.warn("Error reading file", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("banner")
  @RolesAllowed("rewarding")
  @Operation(summary = "Changes Hub Banner in the WoM", method = "POST")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "503", description = "Service Unavailable"),
  })
  public Response saveHubBanner(
                                @Parameter(description = "Signed Message by Hub Manager", required = true)
                                @FormParam("signedMessage")
                                String signedMessage,
                                @Parameter(description = "Raw Message used to sign by Hub Manager", required = true)
                                @FormParam("rawMessage")
                                String rawMessage,
                                @Parameter(description = "WoM generated token used to sign by Hub Manager", required = true)
                                @FormParam("token")
                                String token,
                                @Parameter(description = "Uploaded file resource identifier", required = true)
                                @FormParam("uploadId")
                                String uploadId) {
    try {
      hubService.saveHubBanner(uploadId, signedMessage, rawMessage, token);
      return Response.noContent().build();
    } catch (WomException e) {
      LOG.debug("Error connecting to WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    } catch (ObjectNotFoundException e) {
      return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
    } catch (IOException e) {
      LOG.warn("Error reading file", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("disconnect")
  @RolesAllowed("rewarding")
  @Operation(summary = "Disconnect current Hub from the WoM", method = "POST")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "503", description = "Service Unavailable"),
  })
  public Response disconnectFromWoM(
                                    @Parameter(description = "WoM disconnection request data", required = true)
                                    WomDisconnectionRequest disconnectionRequest) {
    if (disconnectionRequest == null) {
      return Response.status(Status.BAD_REQUEST).entity("wom.emptyConnectionRequest").build();
    } else if (disconnectionRequest.getDeedManagerAddress() == null) {
      return Response.status(Status.BAD_REQUEST).entity("wom.emptyDeedManagerAddress").build();
    } else if (disconnectionRequest.getSignedMessage() == null) {
      return Response.status(Status.BAD_REQUEST).entity("wom.emptySignedMessage").build();
    } else if (disconnectionRequest.getToken() == null) {
      return Response.status(Status.BAD_REQUEST).entity("wom.emptyTokenForSignedMessage").build();
    }
    try {
      hubService.disconnectFromWoM(disconnectionRequest);
      return Response.noContent().build();
    } catch (WomException e) {
      LOG.debug("Error disconnecting to WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    }
  }

}
