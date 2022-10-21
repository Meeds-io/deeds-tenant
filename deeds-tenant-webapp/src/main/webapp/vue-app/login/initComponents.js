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
import Login from './components/Login.vue';
import LoginMetamask from './components/LoginMetamask.vue';
import DeedLogin from './components/tenants/DeedLogin.vue';
import DeedLoginMain from './components/tenants/DeedLoginMain.vue';
import DeedLoginMainTopExtensions from './components/tenants/DeedLoginMainTopExtensions.vue';
import DeedLoginMainBottomExtensions from './components/tenants/DeedLoginMainBottomExtensions.vue';
import DeedLoginIntroduction from './components/tenants/DeedLoginIntroduction.vue';
import DeedLoginBrandingImage from './components/tenants/DeedLoginBrandingImage.vue';

const components = {
  'portal-login': Login,
  'portal-login-metamask': LoginMetamask,
  'portal-original-login': Vue.options.components['portal-login'],
  'portal-deed-login': DeedLogin,
  'portal-deed-login-main': DeedLoginMain,
  'portal-deed-login-main-top-extensions': DeedLoginMainTopExtensions,
  'portal-deed-login-main-bottom-extensions': DeedLoginMainBottomExtensions,
  'portal-deed-login-introduction': DeedLoginIntroduction,
  'portal-deed-login-branding-image': DeedLoginBrandingImage,
};

for (const key in components) {
  Vue.component(key, components[key]);
}

let initialized = !!components['portal-original-login'];
function initComponents() {
  if (initialized || !Vue.options.components['portal-login']) {
    return;
  }
  initialized = true;
  // eslint-disable-next-line vue/component-definition-name-casing
  Vue.component('portal-original-login', Vue.options.components['portal-login']);
  // eslint-disable-next-line vue/component-definition-name-casing
  Vue.component('portal-login', Login);
}

// If Metamask Extension JS has been loaded before login
// We should register the old and new components
// after initializing original Login Module
// to access to it 
const loadComponents = extensionRegistry.loadComponents;
extensionRegistry.loadComponents = (app) => {
  if (app === 'Login') {
    initComponents();
    return loadComponents(app);
  } else {
    return loadComponents(app);
  }
};