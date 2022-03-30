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
  <div v-if="metamaskRegistrationEnabled" class="border-box-sizing mt-4">
    <v-btn
      v-if="!isMetamaskInstalled"
      :href="metamaskInstallLinlk"
      target="_blank"
      rel="noreferrer"
      class="rounded-lg btn"
      block
      outlined
      text>
      <v-img
        src="/deeds-tenant/images/metamask.svg"
        max-height="25px"
        max-width="25px" />
      <span class="py-2 ms-2 text-capitalize">{{ $t('portal.login.SignupWithMetamask') }}</span>
    </v-btn>
    <v-btn
      v-else
      class="rounded-lg btn"
      block
      outlined
      text
      @click="signInWithMetamask()">
      <v-img
        src="/deeds-tenant/images/metamask.svg"
        max-height="25px"
        max-width="25px" />
      <span class="py-2 ms-2 text-capitalize">{{ $t('portal.login.SignupWithMetamask') }}</span>
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
      return this.params && this.params.metamaskRegistrationEnabled;
    },
    rawMessage() {
      return this.params && this.params.rawMessage;
    },
    initialUri() {
      return this.params && this.params.initialUri;
    },
    isMobile() {
      return this.$vuetify.breakpoint.smAndDown;
    },
    currentSiteLink() {
      return `${window.location.host}`;
    },
    metamaskInstallLinlk() {
      return this.isMobile
        && `https://metamask.app.link/dapp/${this.currentSiteLink}`
        || 'https://metamask.io/';
    },
  },
  mounted() {
    this.init();
    window.addEventListener('ethereum#initialized', () => this.init(), {
      once: true,
    });
    setTimeout(() => this.init(), 3000);
  },
  methods: {
    init() {
      this.isMetamaskInstalled = window.ethereum && window.ethereum.isMetaMask;
      if (this.isMetamaskInstalled) {
        this.retrieveAddress();
        window.ethereum.on('accountsChanged', () => this.retrieveAddress());
      }
    },
    signInWithMetamask(forwarded) {
      if (!this.address) {
        if (forwarded) {
          return;
        } else {
          return this.connectToMetamask();
        }
      }
      return window.ethereum.request({
        method: 'personal_sign',
        params: [this.rawMessage, this.address],
      }).then(signedMessage => {
        this.password = `SIGNED_MESSAGE@${signedMessage}`;
        return this.$nextTick();
      }).then(() => this.$refs.metamaskLoginForm.submit());
    },
    connectToMetamask() {
      return window.ethereum.request({
        method: 'eth_requestAccounts'
      })
        .then(() => this.retrieveAddress())
        .then(() => this.signInWithMetamask(true));
    },
    retrieveAddress() {
      return window.ethereum.request({ method: 'eth_accounts' })
        .then(address => this.address = address && address.length && address[0] || null);
    },
  },
};
</script>