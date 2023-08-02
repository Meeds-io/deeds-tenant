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

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.meeds.deeds.model.Hub;
import io.meeds.deeds.model.WomConnectionRequest;
import io.meeds.deeds.model.WomDisconnectionRequest;
import io.meeds.tenant.constant.WomConnectionException;
import io.meeds.tenant.model.HubConfiguration;
import io.meeds.tenant.service.TenantManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/deed/tenant")
@Tag(name = "/deed/tenant", description = "An endpoint to manage current Hub as a WoM member")
public class TenantManagerRest implements ResourceContainer {

  private static final Log     LOG = ExoLogger.getLogger(TenantManagerRest.class);

  private TenantManagerService tenantManagerService;

  public TenantManagerRest(TenantManagerService tenantManagerService) {
    this.tenantManagerService = tenantManagerService;
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
                         String nftId) {
    Hub hub;
    if (StringUtils.isBlank(nftId)) {
      hub = tenantManagerService.getHub();
    } else {
      try {
        hub = tenantManagerService.getHub(Long.parseLong(nftId));
      } catch (WomConnectionException e) {
        LOG.debug("Error connecting to WoM Server", e);
        return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
      }
    }
    if (hub == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(hub).build();
  }

  @GET
  @Path("configuration")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(summary = "Retrieves Deed NFT properties", method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "503", description = "Service unavailable"),
  })
  public Response getDeedTenantConfiguration() {
    try {
      HubConfiguration deedTenantConfiguration = tenantManagerService.getDeedTenantConfiguration();
      return Response.ok(deedTenantConfiguration)
                     .build();
    } catch (WomConnectionException e) {
      LOG.debug("Error connecting to WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    }
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
  public Response isTenantManager(
                                  @Parameter(description = "Wallet address", required = true)
                                  @QueryParam("address")
                                  String address,
                                  @Parameter(description = "Deed NFT identifier", required = true)
                                  @QueryParam("nftId")
                                  long nftId) {
    try {
      boolean isTenantManager = tenantManagerService.isTenantManager(address, nftId);
      return Response.ok(String.valueOf(isTenantManager))
                     .build();
    } catch (WomConnectionException e) {
      LOG.debug("Error connecting to WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
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
      tenantManagerService.connectToWoM(connectionRequest);
      return Response.noContent().build();
    } catch (WomConnectionException e) {
      LOG.debug("Error connecting to WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
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
      tenantManagerService.disconnectFromWoM(disconnectionRequest);
      return Response.noContent().build();
    } catch (WomConnectionException e) {
      LOG.debug("Error disconnecting to WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    }
  }

}
