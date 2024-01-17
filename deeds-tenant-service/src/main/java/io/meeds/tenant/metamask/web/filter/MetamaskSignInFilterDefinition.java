
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
package io.meeds.tenant.metamask.web.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.filter.ExtensibleFilter;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.web.filter.FilterDefinition;
import org.exoplatform.web.filter.FilterDefinitionPlugin;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

import io.meeds.tenant.metamask.service.MetamaskLoginService;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

/**
 * A Login extension to submit Login parameters to UI for used network, contract
 * adresses ...
 */
@Component
public class MetamaskSignInFilterDefinition extends FilterDefinitionPlugin {

  @Autowired
  private ExtensibleFilter extensibleFilter;

  @PostConstruct
  public void init() {
    extensibleFilter.addFilterDefinitions(this);
  }

  @Getter
  private Filter           filter;

  public MetamaskSignInFilterDefinition(PortalContainer container, // NOSONAR
                                        RemindPasswordTokenService remindPasswordTokenService,
                                        WebAppController webAppController,
                                        LocaleConfigService localeConfigService,
                                        BrandingService brandingService,
                                        JavascriptConfigService javascriptConfigService,
                                        SkinService skinService,
                                        MetamaskLoginService metamaskLoginService) {
    super(null);
    this.filter = new MetamaskSignInFilter(container,
                                           remindPasswordTokenService,
                                           webAppController,
                                           localeConfigService,
                                           brandingService,
                                           javascriptConfigService,
                                           skinService,
                                           metamaskLoginService);
  }

  @Override
  public List<FilterDefinition> getFilterDefinitions() {
    return Collections.singletonList(new FilterDefinition(filter, Arrays.asList("/login", "/register", "/tenantSetup")));
  }

}
