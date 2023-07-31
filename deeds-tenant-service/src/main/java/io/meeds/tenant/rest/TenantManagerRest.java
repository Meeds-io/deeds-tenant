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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.exoplatform.services.rest.resource.ResourceContainer;

import io.meeds.tenant.model.DeedTenantConfiguration;
import io.meeds.tenant.model.DeedTenantHub;
import io.meeds.tenant.service.TenantManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/deed/tenant")
@Tag(name = "/deed/tenant", description = "An endpoint to manage current Hub as a WoM member")
public class TenantManagerRest implements ResourceContainer {

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
  })
  public Response getDeedTenant(
                                @Parameter(description = "Deed NFT identifier", required = true)
                                @QueryParam("nftId")
                                long nftId) {
    DeedTenantHub deedTenant = tenantManagerService.getDeedTenant(nftId);
    if (deedTenant == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(new DeedTenantHub(deedTenant.getNftId(),
                                         deedTenant.getCity(),
                                         deedTenant.getType()))
                   .build();
  }

  @GET
  @Path("configuration")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(summary = "Retrieves Deed NFT properties", method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public Response getDeedTenantConfiguration() {
    DeedTenantConfiguration deedTenantConfiguration = tenantManagerService.getDeedTenantConfiguration();
    return Response.ok(deedTenantConfiguration)
                   .build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("manager")
  @RolesAllowed("rewarding")
  @Operation(summary = "Checks whether a wallet is the provisioning manager of a Deed or not", method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public Response isTenantManager(
                                  @Parameter(description = "Wallet address", required = true)
                                  @QueryParam("address")
                                  String address,
                                  @Parameter(description = "Deed NFT identifier", required = true)
                                  @QueryParam("nftId")
                                  long nftId) {

    boolean isTenantManager = tenantManagerService.isTenantManager(address, nftId);
    return Response.ok(String.valueOf(isTenantManager))
                   .build();
  }

}
