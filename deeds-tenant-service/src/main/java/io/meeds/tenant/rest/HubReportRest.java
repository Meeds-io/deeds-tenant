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

import static io.meeds.deeds.utils.JsonUtils.toJsonString;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.meeds.deeds.constant.WomException;
import io.meeds.deeds.constant.WomParsingException;
import io.meeds.tenant.model.HubRewardReportLocalStatus;
import io.meeds.tenant.service.HubReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/deed/reports")
@Tag(name = "/deed/reports", description = "An endpoint to manage current Hub as a WoM member")
public class HubReportRest implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(HubReportRest.class);

  private HubReportService hubReportService;

  public HubReportRest(HubReportService hubReportService) {
    this.hubReportService = hubReportService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(summary = "Retrieves the list of Hub Reward reports", method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
  })
  public Response getReports(
                             @Parameter(description = "Offset of query results", required = true)
                             @QueryParam("offset")
                             int offset,
                             @Parameter(description = "Limit of query results", required = true)
                             @QueryParam("limit")
                             int limit) throws WomParsingException {
    if (offset < 0) {
      return Response.status(Status.BAD_REQUEST).entity("Offset must be positive").build();
    }
    if (limit < 0) {
      return Response.status(Status.BAD_REQUEST).entity("Limit must be strictly positive").build();
    }
    List<HubRewardReportLocalStatus> hubRewardReports = hubReportService.getHubRewardReports(offset, limit);
    return Response.ok(toJsonString(hubRewardReports)).build();
  }

  @GET
  @Path("{periodId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(summary = "Retrieve a Hub Reward report identified by its id", method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
  })
  public Response getReport(
                            @Parameter(description = "Report period identifier key", required = true)
                            @PathParam("periodId")
                            long periodId,
                            @Parameter(description = "Whether refresh from the WoM or not", required = false)
                            @QueryParam("refresh")
                            boolean refresh) throws WomParsingException {
    try {
      HubRewardReportLocalStatus hubRewardReport = hubReportService.getHubRewardReport(periodId, refresh);
      return Response.ok(toJsonString(hubRewardReport)).build();
    } catch (WomException e) {
      LOG.debug("Error communicating with WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    }
  }

  @PUT
  @Path("{periodId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(summary = "Resent", method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
  })
  public Response sendReportToWoM(
                                  @Parameter(description = "Report period identifier key", required = true)
                                  @PathParam("periodId")
                                  long periodId) {
    try {
      HubRewardReportLocalStatus hubRewardReport = hubReportService.sendReportToWoM(periodId);
      return Response.ok(toJsonString(hubRewardReport)).build();
    } catch (WomException e) {
      LOG.debug("Error communicating with WoM Server", e);
      return Response.status(Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
    }
  }

}
