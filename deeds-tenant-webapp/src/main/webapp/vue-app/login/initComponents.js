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
import DeedLogin from './components/DeedLogin.vue';
import DeedLoginMain from './components/DeedLoginMain.vue';
import DeedLoginIntroduction from './components/DeedLoginIntroduction.vue';
import DeedLoginBrandingImage from './components/DeedLoginBrandingImage.vue';

const originalLoginComponent = Vue.options.components['portal-login'];

const components = {
  'portal-login': Login,
  'portal-login-metamask': LoginMetamask,
  'portal-original-login': originalLoginComponent,
  'portal-deed-login': DeedLogin,
  'portal-deed-login-main': DeedLoginMain,
  'portal-deed-login-introduction': DeedLoginIntroduction,
  'portal-deed-login-branding-image': DeedLoginBrandingImage,
};

for (const key in components) {
  Vue.component(key, components[key]);
}