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
package io.meeds.tenant.hub.listener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.common.ContainerTransactional;
import io.meeds.tenant.hub.service.HubService;

import jakarta.annotation.PostConstruct;

@Asynchronous
@Component
public class HubPublicSiteVisibilityUpdateListener extends Listener<Object, PortalConfig> {

  @Autowired
  private HubService      hubService;

  @Autowired
  private ListenerService listenerService;

  @Override
  @ContainerTransactional
  public void onEvent(Event<Object, PortalConfig> event) throws Exception {
    PortalConfig portalConfig = event.getData();
    if (portalConfig == null
        || !StringUtils.equalsIgnoreCase(portalConfig.getName(), HubService.PUBLIC_SITE_NAME)) {
      return;
    }
    hubService.updateHubCard();
  }

  @PostConstruct
  public void init() {
    listenerService.addListener(LayoutService.PORTAL_CONFIG_UPDATED, this);
  }

}
