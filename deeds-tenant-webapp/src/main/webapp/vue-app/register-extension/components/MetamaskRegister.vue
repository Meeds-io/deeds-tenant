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
  <div v-if="metamaskRegistrationEnabled" class="d-flex border-box-sizing mt-4">
    <v-btn
      v-if="!isMetamaskInstalled"
      :href="metamaskInstallLinlk"
      :block="!isDeedTenant"
      :color="isDeedTenant && 'primary'"
      :large="isDeedTenant"
      :class="!isDeedTenant && 'rounded-lg'"
      target="_blank"
      rel="noreferrer"
      class="mx-auto white-background d-block max-width-fit"
      outlined>
      <v-img
        src="/deeds-tenant/images/metamask.svg"
        max-height="25px"
        max-width="25px" />
      <span class="py-2 ms-2 text-truncate text-capitalize">{{ $t('portal.login.SignupWithMetamask') }}</span>
    </v-btn>
    <v-btn
      v-else
      :block="!isDeedTenant"
      :color="isDeedTenant && 'primary'"
      :large="isDeedTenant"
      :class="!isDeedTenant && 'rounded-lg'"
      class="mx-auto white-background d-block max-width-fit"
      outlined
      @click="signInWithMetamask()">
      <v-img
        src="/deeds-tenant/images/metamask.svg"
        max-height="25px"
        max-width="25px" />
      <span class="py-2 ms-2 text-truncate">{{ $t('portal.login.SignupWithMetamask') }}</span>
    </v-btn>
    <form
      ref="metamaskLoginForm"
      action="/portal/register"
      method="post">
      <input
        v-if="initialUri"
        type="hidden"
        name="initialURI"
        :value="initialUri">
      <input
        name="username"
        type="hidden"
        :value="address">
      <input
        type="hidden"
        name="password"
        :value="password">
      <input
        name="rememberme"
        type="hidden"
        :value="rememberme">
    </form>
  </div>
</template>
<script>
export default {
  props: {
    params: {
      type: Object,
      default: null,
    },
    rememberme: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    isMetamaskInstalled: false,
    address: null,
    password: null,
  }),
  computed: {
    metamaskRegistrationEnabled() {
      return this.params?.metamaskRegistrationEnabled;
    },
    rawMessage() {
      return this.params?.rawMessage;
    },
    initialUri() {
      return this.params?.initialUri;
    },
    isDeedTenant() {
      return this.params?.isDeedTenant;
    },
    isMobile() {
      return this.$vuetify.breakpoint.mobile;
    },
    currentSiteLink() {
      return `${window.location.host}${window.location.pathname}`;
    },
    metamaskInstallLinlk() {
      return this.isMobile
        && `https://metamask.app.link/dapp/${this.currentSiteLink}`
        || 'https://metamask.io/';
    },
  },
  created() {
    this.isMetamaskInstalled = !this.disabled && this.$metamaskUtils.isMetamaskInstalled();
    if (this.isMetamaskInstalled) {
      this.retrieveAddress();
      window.ethereum.on('accountsChanged', () => this.retrieveAddress());
    }
  },
  methods: {
    signInWithMetamask() {
      if (this.disabled) {
        return;
      }
      return this.$metamaskUtils.signInWithMetamask(this.rawMessage, this.isMobile)
        .then(password => this.password = password)
        .then(() => this.retrieveAddress())
        .then(() => this.$nextTick())
        .then(() => this.$refs.metamaskLoginForm.submit())
        .catch(e => this.$root.$emit('alert-message', e?.message || String(e), 'warning'));
    },
    retrieveAddress() {
      return this.$metamaskUtils.retrieveAddress()
        .then(address => this.address = address);
    },
  },
};
</script>