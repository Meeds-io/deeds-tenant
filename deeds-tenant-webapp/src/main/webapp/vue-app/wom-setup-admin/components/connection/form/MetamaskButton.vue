<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io

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
  <div class="d-flex border-box-sizing">
    <v-btn
      v-if="!isMetamaskInstalled"
      :href="metamaskInstallLinlk"
      :class="primary && 'primary' || 'white-background primary-border-color'"
      target="_blank"
      rel="nofollow noreferrer noopener"
      class="mx-auto d-block"
      elevation="0">
      <v-img
        src="/deeds-tenant/images/metamask.svg"
        max-height="25px"
        max-width="25px" />
      <span :class="!primary && 'primary--text'" class="py-2 ms-2 text-truncate text-none">{{ $t('wom.installMetamask') }}</span>
    </v-btn>
    <v-btn
      v-else
      :disabled="disabled"
      :class="primary && 'primary' || 'white-background primary-border-color'"
      class="mx-auto d-block"
      elevation="0"
      @click="signInWithMetamask()">
      <v-img
        src="/deeds-tenant/images/metamask.svg"
        max-height="25px"
        max-width="25px" />
      <span :class="!primary && 'primary--text'" class="py-2 ms-2 text-truncate text-none">{{ label && $t(label) || $t('wom.signWithMetamask') }}</span>
    </v-btn>
  </div>
</template>
<script>
export default {
  props: {
    message: {
      type: String,
      default: null,
    },
    label: {
      type: String,
      default: null,
    },
    allowedAddress: {
      type: String,
      default: null,
    },
    primary: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    isMetamaskInstalled: false,
    currentSiteLink: window.location.host,
    address: null,
    signature: null,
  }),
  computed: {
    isMobile() {
      return this.$vuetify.breakpoint.mobile;
    },
    metamaskInstallLinlk() {
      return this.isMobile
        && `https://metamask.app.link/dapp/${this.currentSiteLink}`
        || 'https://metamask.io/';
    },
  },
  watch: {
    address() {
      this.$emit('update:address', this.address);
    },
    signature() {
      this.$emit('update:signature', this.signature);
    },
  },
  created() {
    this.isMetamaskInstalled = this.$metamaskUtils.isMetamaskInstalled();
  },
  methods: {
    signInWithMetamask() {
      return this.$metamaskUtils.signInWithMetamask(this.message, this.isMobile)
        .then(signature => this.signature = signature.replace('SIGNED_MESSAGE@', ''))
        .then(() => this.$metamaskUtils.retrieveAddress())
        .then(address => {
          if (!this.allowedAddress || !address || this.allowedAddress.toLowerCase() === address.toLowerCase()) {
            this.address = address;
          } else {
            this.$root.$emit('alert-message', this.$t('wom.onlyHubOwnerAddressCanManageWoMConnection', {0: this.allowedAddress}), 'error');
          }
        })
        .catch(console.debug);// eslint-disable-line no-console
    },
  },
};
</script>