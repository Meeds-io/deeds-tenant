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
  <div>
    <component
      :provider="provider"
      :rememberme="rememberme"
      :params="params"
      :display-text="displayText"
      :is="isMenu && 'portal-login-provider-menu-link' || 'portal-login-provider-link'"
      :class="isMenu && 'portal-login-provider-menu-link' || 'portal-login-provider-link'"
      @submit="signInWithMetamask()" />
    <form
      ref="metamaskLoginForm"
      action="/portal/login"
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
    provider: {
      type: Object,
      default: null,
    },
    params: {
      type: Object,
      default: null,
    },
    rememberme: {
      type: Boolean,
      default: false,
    },
    displayText: {
      type: Boolean,
      default: false,
    },
    isMenu: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    address: null,
    password: null,
  }),
  computed: {
    rawMessage() {
      return this.params?.rawMessage;
    },
    initialUri() {
      return this.params?.initialUri;
    },
    isMobile() {
      return this.$vuetify.breakpoint.mobile;
    },
  },
  created() {
    this.retrieveAddress();
    window.ethereum.on('accountsChanged', () => this.retrieveAddress());
  },
  methods: {
    signInWithMetamask() {
      return this.$metamaskUtils.signInWithMetamask(this.rawMessage, this.isMobile)
        .then(password => this.password = password)
        .then(() => this.retrieveAddress())
        .then(() => this.$nextTick())
        .then(() => this.$refs.metamaskLoginForm.submit())
        .catch(console.debug);// eslint-disable-line no-console
    },
    retrieveAddress() {
      return this.$metamaskUtils.retrieveAddress()
        .then(address => this.address = address);
    },
  },
};
</script>