<!--
  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2023 Meeds Association contact@meeds.io

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
  <exo-drawer
    ref="drawer"
    v-model="drawer"
    :right="!$vuetify.rtl"
    class="WoMDisconnectionDrawer"
    @opened="init"
    @closed="reset">
    <template slot="title">
      <span class="pb-2"> {{ $t('wom.disconnectionDrawerTitle') }} </span>
    </template>
    <template v-if="!loading && drawer" #content>
      <div class="px-6 mt-8">
        <span class="font-weight-bold dark-grey-color text-subtitle-1">
          {{ $t('wom.connectWallet') }}
        </span>
        <wom-integration-deed-manager-selector
          ref="managerSelector"
          :raw-message="rawMessage"
          :address.sync="deedManagerAddress"
          :signature.sync="signedMessage" />
      </div>
    </template>
    <template v-if="!loading && drawer" #footer>
      <div class="d-flex">
        <v-spacer />
        <v-btn
          class="btn me-2"
          @click="close">
          <template>
            {{ $t('wom.cancel') }}
          </template>
        </v-btn>
        <v-btn
          :loading="disconnecting"
          :disabled="!signedMessage"
          class="btn primary"
          @click="disconnect">
          <template>
            {{ $t('wom.disconnect') }}
          </template>
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    drawer: false,
    loading: false,
    disconnecting: false,
    deedManagerAddress: null,
    token: null,
    signedMessage: null,
  }),
  computed: {
    rawMessage() {
      return this.token && this.$t('wom.signDisconnectMessage', {
        0: this.token,
      }).replace(/\\n/g, '\n') || null;
    },
  },
  watch: {
    loading() {
      if (this.loading) {
        this.$refs.drawer.startLoading();
      } else {
        this.$refs.drawer.endLoading();
      }
    },
    disconnecting() {
      if (this.disconnecting) {
        this.$refs.drawer.startLoading();
      } else {
        this.$refs.drawer.endLoading();
      }
    },
  },
  methods: {
    open() {
      this.$refs.drawer.open();
    },
    init() {
      this.loading = true;
      this.$hubService.getConfiguration()
        .then(configuration => this.token = configuration.token)
        .finally(() => this.loading = false);
    },
    reset() {
      this.signedMessage = null;
      this.token = null;

      this.$refs?.managerSelector?.reset();
    },
    close() {
      this.reset();
      this.$nextTick(() => {
        window.setTimeout(() => this.$refs.drawer.close(), 50);
      });
    },
    disconnect() {
      this.disconnecting = true;
      this.$hubService.disconnectFromWoM({
        deedManagerAddress: this.deedManagerAddress,
        signedMessage: this.signedMessage,
        rawMessage: this.rawMessage,
        token: this.token,
      })
        .then(() => {
          this.disconnecting = false;
          this.close();
          this.$root.$emit('alert-message', this.$t('wom.disconnectedFromWoMSuccessfully'), 'success');
          this.$root.$emit('wom-disconnection-success');
        })
        .catch(e => {
          this.disconnecting = false;
          const error = (e?.cause || String(e));
          const errorMessageKey = error.includes('wom.') && `wom.${error.split('wom.')[1]}` || error;
          this.$root.$emit('alert-message', this.$t(errorMessageKey), 'error');
        });
    },
  },
};
</script>