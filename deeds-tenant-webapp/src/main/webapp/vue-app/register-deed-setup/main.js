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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import './initComponents.js';

// get overrided components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('MetamaskTenantSetup');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

//getting language of the PLF
const lang = window.eXo && eXo.env.portal.language || 'en';

const appId = 'metamaskTenantSetupApplication';

const urls = [
  `/social-portlet/i18n/locale.portlet.Login?lang=${lang}`,
  `/social-portlet/i18n/locale.portal.login?lang=${lang}`,
  `/social-portlet/i18n/locale.portlet.Portlets?lang=${lang}`,
  `/social-portlet/i18n/locale.portlet.Branding?lang=${lang}`,
  `/social-portlet/i18n/locale.portlet.GeneralSettings?lang=${lang}`,
];

export function init(params) {
  exoi18n.loadLanguageAsync(lang, urls).then(i18n => {
    // init Vue app when locale ressources are ready
    Vue.createApp({
      data: {
        params: params,
      },
      template: `<deed-tenant-setup id="${appId}" :params="params" />`,
      vuetify: Vue.prototype.vuetifyOptions,
      i18n
    }, `#${appId}`, 'Metamask Setup');
  });
}
