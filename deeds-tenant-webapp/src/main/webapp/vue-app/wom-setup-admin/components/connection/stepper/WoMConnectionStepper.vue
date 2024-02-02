<!--

  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io

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
  <div class="flex-grow-1 flex-shrink-0">
    <wom-setup-deed-manager-selector
      v-show="!connecting && stepper <= 2"
      ref="managerSelector"
      :hub="hub"
      :edit="edit"
      :raw-message="rawMessage"
      :address.sync="deedManagerAddress"
      :signature.sync="signedMessage"
      :disabled="disabled"
      class="mx-auto" />
    <wom-setup-deed-selector
      v-if="stepper === 2"
      v-show="!connecting"
      v-model="deed"
      :hub="hub"
      :edit="edit"
      :disabled="disabled"
      :address="deedManagerAddress"
      :owner.sync="deedOwnerAddress"
      class="mt-5" />
    <template v-if="stepper > 2 || connecting">
      <v-list-item class="pa-0" dense>
        <v-list-item-avatar
          color="secondary"
          class="my-0 white--text"
          size="20">
          1
        </v-list-item-avatar>
        <v-list-item-content>
          <v-list-item-title class="subtitle-1 font-weight-bold">
            {{ $t('wom.crossChainBridging') }}
          </v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-slide-y-transition>
        <div v-show="!connected && connecting">
          <wom-bridging-label :deed="deed" />
        </div>
      </v-slide-y-transition>
      <v-list-item class="pa-0" dense>
        <v-list-item-avatar
          :color="connected && 'secondary' || 'grey'"
          class="my-0 white--text"
          size="20">
          2
        </v-list-item-avatar>
        <v-list-item-content>
          <v-list-item-title class="subtitle-1 font-weight-bold">
            {{ $t('wom.connection') }}
          </v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-slide-y-transition>
        <div v-show="transactionInProgress">
          <wom-connecting-label />
        </div>
      </v-slide-y-transition>
      <v-slide-y-transition>
        <div v-show="connected && !transactionInProgress">
          <wom-connection-label :deed="deed" />
        </div>
      </v-slide-y-transition>
    </template>
  </div>
</template>
<script>
export default {
  props: {
    hub: {
      type: Object,
      default: null,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    transactionInProgress: {
      type: Boolean,
      default: false,
    },
    edit: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    drawer: false,
    loading: false,
    connecting: false,
    connected: false,
    stepper: 1,
    deedOwnerAddress: null,
    deedManagerAddress: null,
    hubUrl: null,
    token: null,
    signedMessage: null,
    deed: null,
    validateEmpty: false,
  }),
  computed: {
    rawMessage() {
      return this.token && this.$t('wom.signConnectMessage', {
        0: this.token,
      }).replace(/\\n/g, '\n') || null;
    },
    modified() {
      return !!this.deedManagerAddress;
    },
    valid() {
      return this.deed
        && this.deedOwnerAddress
        && this.deedManagerAddress
        && this.signedMessage
        && this.rawMessage
        && this.token;
    },
  },
  watch: {
    loading() {
      this.$emit('loading', this.loading);
    },
    connecting() {
      this.$emit('connecting', this.connecting);
    },
    connected() {
      this.$emit('connected', this.connected);
    },
    modified() {
      this.$emit('modified', this.modified);
    },
    deed() {
      if (!this.deed) {
        this.$emit('select');
      }
    },
    deedManagerAddress(newVal, oldVal) {
      if (newVal && !oldVal) {
        this.stepper = 2;
      } else if (!newVal && oldVal) {
        this.stepper = 1;
        this.$emit('select');
      }
    },
    valid() {
      this.$emit('validated', this.valid);
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.reset();

      this.loading = true;
      return this.$hubService.generateToken()
        .then(token => this.token = token)
        .finally(() => this.loading = false);
    },
    reset() {
      this.stepper = 1;
      this.deed = null;
      this.hubUrl = window.location.origin;
      this.validateEmpty = false;
      this.deedOwnerAddress = null;
      this.deedManagerAddress = null;
      this.signedMessage = null;
      this.token = null;
      this.$refs?.managerSelector?.reset();
    },
    clearDeed() {
      this.deed = null;
      this.connected = false;
      this.stepper = 2;
    },
    connect() {
      if (!this.valid) {
        return;
      }

      this.connecting = true;
      this.$hubService.connectToWoM({
        deedId: this.deed?.nftId,
        deedOwnerAddress: this.deedOwnerAddress,
        deedManagerAddress: this.deedManagerAddress,
        url: this.hubUrl,
        signedMessage: this.signedMessage,
        rawMessage: this.rawMessage,
        token: this.token,
      })
        .then(data => {
          this.connecting = false;
          this.connected = true;
          this.stepper = 3;
          this.$emit('select', this.deedManagerAddress, this.deed, data);
        })
        .catch(e => {
          this.connecting = false;
          const error = (e?.data?.message || e?.message || e?.cause || String(e));
          let errorMessageKey = error.includes('wom.') && `wom.${error.split('wom.')[1].split(/[^A-Za-z0-9]/g)[0]}` || error;
          if (!this.$te(errorMessageKey)) {
            errorMessageKey = 'wom.connectionError';
          }
          this.$root.$emit('alert-message', this.$t(errorMessageKey), 'error');
        });
    },
  },
};
</script>