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
package io.meeds.tenant.hub.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.tenant.hub.model.HubReportLocalStatus;
import io.meeds.tenant.hub.service.HubReportService;
import io.meeds.wom.api.constant.WomException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("reports")
@Tag(name = "reports", description = "An endpoint to manage current Hub as a WoM member")
public class HubReportController {

  private static final Log LOG = ExoLogger.getLogger(HubReportController.class);

  @Autowired
  private HubReportService reportService;

  @GetMapping
  @Secured("rewarding")
  @Operation(summary = "Retrieves the list of Hub Reward reports", method = "GET")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  @ApiResponse(responseCode = "400", description = "Bad request")
  public List<HubReportLocalStatus> getReports(
                                               @Parameter(description = "Offset of query results", required = true)
                                               @RequestParam("offset")
                                               int offset,
                                               @Parameter(description = "Limit of query results", required = true)
                                               @RequestParam("limit")
                                               int limit) {
    return reportService.getReports(offset, limit);
  }

  @GetMapping("{periodId}")
  @Secured("rewarding")
  @Operation(summary = "Retrieve a Hub Reward report identified by its id", method = "GET")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  @ApiResponse(responseCode = "400", description = "Bad request")
  public HubReportLocalStatus getReport(
                                        @Parameter(description = "Report period identifier key", required = true)
                                        @PathVariable("periodId")
                                        long periodId,
                                        @Parameter(description = "Whether refresh from the WoM or not", required = false)
                                        @RequestParam("refresh")
                                        boolean refresh) {
    try {
      return reportService.getReport(periodId, refresh);
    } catch (WomException e) {
      logWomException(e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
  }

  @PutMapping("{periodId}")
  @Secured("rewarding")
  @Operation(summary = "Send manually Hub reward report to the UEM engine", method = "GET")
  @ApiResponse(responseCode = "200", description = "Request fulfilled")
  @ApiResponse(responseCode = "400", description = "Bad request")
  public HubReportLocalStatus sendReport(
                                         @Parameter(description = "Report period identifier key", required = true)
                                         @PathVariable("periodId")
                                         long periodId) {
    try {
      return reportService.sendReport(periodId);
    } catch (WomException e) {
      logWomException(e);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
  }

  private void logWomException(WomException e) {
    LOG.debug(e.getErrorCode().getMessageKey(), e);
  }

}
