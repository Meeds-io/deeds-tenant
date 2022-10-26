<!--

 This file is part of the Meeds project (https://meeds.io/).
 
 Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

-->
<template>
  <extension-registry-components
    v-if="enabled"
    :params="{params}"
    name="Register"
    type="register-extension"
    parent-element="div"
    element="div">
    <template #separator>
      <div class="d-flex my-5">
        <v-divider class="my-auto secondary login-separator" />
        <span class="mx-3 text-uppercase">
          {{ $t('UILoginForm.label.or') }}
        </span>
        <v-divider class="my-auto secondary login-separator" />
      </div>
    </template>
  </extension-registry-components>
</template>
<script>
export default {
  props: {
    params: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    enabled: false,
    deedRegisterExtension: Vue.options.components['deed-register-onboarding'],
  }),
  created() {
    this.extendOriginalOnboarding();
    document.addEventListener('component-Register-register-extension-updated', () => this.extendOriginalOnboarding());
  },
  mounted() {
    if (!this.enabled) {
      this.extendOriginalOnboarding();
      this.enabled = true;
    }
  },
  methods: {
    extendOriginalOnboarding() {
      const registeredComponents = extensionRegistry.loadComponents('Register');
      const componentIndex = registeredComponents.findIndex(component => component?.componentOptions?.id === 'onboarding');
      if (componentIndex && registeredComponents[componentIndex].vueComponent !== this.deedRegisterExtension) {
        registeredComponents.splice(componentIndex, 1);
        extensionRegistry.registerComponent('Register', 'register-extension', {
          id: 'onboarding',
          vueComponent: this.deedRegisterExtension,
          isEnabled: params => params && params.params && params.params.onboardingRegisterEnabled,
          rank: 80,
        });
        this.enabled = false;
        this.$nextTick(() => this.enabled = true);
      }
    }
  },
};
</script>