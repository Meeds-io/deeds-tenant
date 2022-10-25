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
import Register from './components/Register.vue';
import MetamaskRegister from './components/MetamaskRegister.vue';
import DeedOnboardingRegister from './components/OnboardingRegister.vue';
import DeedRegister from './components/tenants/DeedRegister.vue';
import DeedRegisterMain from './components/tenants/DeedRegisterMain.vue';
import DeedRegisterExtensions from './components/tenants/DeedRegisterExtensions.vue';

const components = {
  'portal-register': Register,
  'portal-original-register': Vue.options.components['portal-register'],
  'portal-original-onboarding-register': Vue.options.components['portal-register-onboarding'],
  'portal-register-metamask': MetamaskRegister,
  'deed-register': DeedRegister,
  'deed-register-main': DeedRegisterMain,
  'deed-register-extensions': DeedRegisterExtensions,
  'deed-register-onboarding': DeedOnboardingRegister,
};

for (const key in components) {
  Vue.component(key, components[key]);
}

let initialized = !!components['portal-original-register'];
function initComponents() {
  if (initialized || !Vue.options.components['portal-register']) {
    return;
  }
  initialized = true;
  // eslint-disable-next-line vue/component-definition-name-casing
  Vue.component('portal-original-register', Vue.options.components['portal-register']);
  // eslint-disable-next-line vue/component-definition-name-casing
  Vue.component('portal-register', Register);
}

// If Metamask Extension JS has been loaded before register
// We should register the old and new components
// after initializing original Register Module
// to access to it 
const loadComponents = extensionRegistry.loadComponents;
extensionRegistry.loadComponents = (app) => {
  if (app === 'Register') {
    initComponents();
    return loadComponents(app);
  } else {
    return loadComponents(app);
  }
};