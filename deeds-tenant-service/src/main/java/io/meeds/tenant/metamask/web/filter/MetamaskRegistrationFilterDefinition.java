
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.tenant.metamask.web.filter;

import java.util.*;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.filter.*;

import io.meeds.tenant.metamask.service.MetamaskLoginService;

/**
 * A Login extension to submit Login parameters to UI for used network, contract
 * adresses ...
 */
public class MetamaskRegistrationFilterDefinition extends FilterDefinitionPlugin {

  private Filter filter;

  public MetamaskRegistrationFilterDefinition(PortalContainer container, // NOSONAR
                                              WebAppController webAppController,
                                              LocaleConfigService localeConfigService,
                                              BrandingService brandingService,
                                              JavascriptConfigService javascriptConfigService,
                                              SkinService skinService,
                                              MetamaskLoginService metamaskLoginService,
                                              InitParams params) {
    super(params);
    this.filter = new MetamaskRegistrationFilter(container,
                                                 webAppController,
                                                 localeConfigService,
                                                 brandingService,
                                                 javascriptConfigService,
                                                 skinService,
                                                 metamaskLoginService);
  }

  @Override
  public List<FilterDefinition> getFilterDefinitions() {
    return Collections.singletonList(new FilterDefinition(filter, Arrays.asList("/login", "/register")));
  }

}
