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
import DeedTenantSetup from './components/DeedTenantSetup.vue';
import DeedTenantSetupMain from './components/DeedTenantSetupMain.vue';
import CompanyBranding from './components/branding/CompanyBranding.vue';
import ColorPicker from './components/branding/form/ColorPicker.vue';
import LoginPreview from './components/branding/form/LoginPreview.vue';
import CompanyLogo from './components/branding/form/CompanyLogo.vue';

const components = {
  'deed-tenant-setup': DeedTenantSetup,
  'deed-tenant-setup-main': DeedTenantSetupMain,
  'deed-tenant-setup-branding': CompanyBranding,
  'deed-tenant-setup-color-picker': ColorPicker,
  'deed-tenant-setup-company-logo': CompanyLogo,
  'deed-tenant-setup-login-preview': LoginPreview,
};

for (const key in components) {
  Vue.component(key, components[key]);
}