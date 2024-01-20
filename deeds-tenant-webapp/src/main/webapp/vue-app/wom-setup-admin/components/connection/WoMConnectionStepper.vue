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
      v-show="stepper > 0 && stepper < 3"
      ref="managerSelector"
      :raw-message="rawMessage"
      :address.sync="deedManagerAddress"
      :signature.sync="signedMessage"
      class="mx-auto" />
    <wom-setup-deed-selector
      v-if="stepper === 2"
      v-model="deedId"
      :address="deedManagerAddress"
      :owner.sync="deedOwnerAddress"
      class="mt-5" />
  </div>
</template>
<script>
export default {
  props: {
    hub: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    drawer: false,
    loading: false,
    connecting: false,
    stepper: 1,
    deedOwnerAddress: null,
    deedManagerAddress: null,
    earnerAddress: null,
    hubUrl: null,
    token: null,
    signedMessage: null,
    deedId: null,
    validateEmpty: false,
  }),
  computed: {
    step1Valid() {
      return this.stepper !== 1 || this.token && this.deedManagerAddress && this.signedMessage;
    },
    step2Valid() {
      return this.stepper !== 2 || this.deedId;
    },
    step3Valid() {
      return this.stepper !== 3 || this.earnerAddress;
    },
    rawMessage() {
      return this.token && this.$t('wom.signConnectMessage', {
        0: this.token,
      }).replace(/\\n/g, '\n') || null;
    },
    modified() {
      return !!this.deedManagerAddress;
    },
    valid() {
      return this.deedId
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
    modified() {
      this.$emit('modified', this.modified);
    },
    deedManagerAddress(newVal, oldVal) {
      if (newVal && !oldVal) {
        this.stepper = 2;
      } else if (!newVal && oldVal) {
        this.stepper = 1;
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
      return this.$womService.generateToken()
        .then(token => this.token = token)
        .finally(() => this.loading = false);
    },
    reset() {
      this.stepper = 1;
      this.deedId = null;
      this.earnerAddress = this.hub?.earnerAddress || this.$root?.configuration?.adminWallet;
      this.hubUrl = window.location.origin;
      this.validateEmpty = false;
      this.deedOwnerAddress = null;
      this.deedManagerAddress = null;
      this.signedMessage = null;
      this.token = null;
      this.$refs?.managerSelector?.reset();
    },
    connect() {
      if (!this.valid) {
        return;
      }

      this.connecting = true;
      this.$womService.connectToWoM({
        deedId: this.deedId,
        deedOwnerAddress: this.deedOwnerAddress,
        deedManagerAddress: this.deedManagerAddress,
        url: this.hubUrl,
        earnerAddress: this.earnerAddress,
        signedMessage: this.signedMessage,
        rawMessage: this.rawMessage,
        token: this.token,
      })
        .then(() => {
          this.connecting = false;
          this.close();
          if (this.hub) {
            this.$root.$emit('alert-message', this.$t('wom.hubDetailsModifiedSuccessfully'), 'success');
          } else {
            this.$root.$emit('alert-message-html-confeti', this.$t('wom.connectedToWoMSuccessfully'), 'success');
          }
          this.$root.$emit('wom-connection-success');
        })
        .catch(e => {
          this.connecting = false;
          const error = (e?.cause || String(e));
          const errorMessageKey = error.includes('wom.') && `wom.${error.split('wom.')[1]}` || error;
          this.$root.$emit('alert-message', this.$t(errorMessageKey), 'error');
        });
    },
  },
};
</script>