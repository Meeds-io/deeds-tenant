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
    :loading="loading"
    :confirm-close="modified"
    :confirm-close-labels="confirmCloseLabels"
    allow-expand
    right
    @expand-updated="expanded = $event"
    @closed="modified = false">
    <template #title>
      {{ $t('wom.setup.drawer.title') }}
    </template>
    <template v-if="drawer && initialized" #content>
      <v-card
        class="pa-5"
        flat>
        <template v-if="!connected || edit">
          <template v-if="!modified && !edit">
            <p>
              {{ $t('wom.connect.parapgraph1') }}
            </p>
            <p>
              {{ $t('wom.connect.parapgraph2') }}
            </p>
            <p>
              {{ $t('wom.connect.parapgraph3') }}
            </p>
          </template>
          <wom-setup-connection-stepper
            ref="womConnection"
            :hub="hub"
            :edit="edit"
            @modified="modified = $event"
            @validated="canConnect = $event"
            @connecting="connecting = $event"
            @connected="synched = $event"
            @select="selectDeed" />
          <wom-setup-disconnection-stepper
            ref="womDisconnection"
            :hub="hub"
            @disconnecting="disconnecting = $event" />
        </template>
        <template v-else>
          <p class="mb-5 text-center" v-sanitized-html="$t('wom.connected')"></p>
          <p class="my-0">
            {{ $t('wom.connected.part1') }}
          </p>
          <p
            class="my-0"
            v-sanitized-html="$t('wom.connected.part2', {
              0: `<a href='http://www.meeds.io/hubs' target='_blank' rel='nofollow noreferrer noopener'>`,
              1: '</a>'
            })">
          </p>
          <p class="mb-5">
            {{ $t('wom.connected.part3') }}
          </p>
          <wom-setup-hub-card
            :hub="hub"
            @edit="edit = true" />
        </template>
      </v-card>
    </template>
    <template v-if="modified || edit" #footer>
      <div class="d-flex">
        <template v-if="modified">
          <v-btn
            v-if="connected"
            :loading="disconnecting"
            color="error"
            outlined
            elevation="0"
            class="ignore-vuetify-classes"
            @click="$refs.womDisconnection.open()">
            <span class="text-none">{{ $t('wom.disconnect') }}</span>
          </v-btn>
          <v-spacer />
          <v-btn
            :disabled="connecting || disconnecting"
            class="btn me-4"
            @click="close">
            {{ $t('wom.cancel') }}
          </v-btn>
          <wom-setup-connect-button
            v-if="deed"
            :deed-manager-address="deedManagerAddress"
            :deed="deed"
            :wom-connection-params="womConnectionParams"
            class="btn btn-primary" />
          <v-btn
            v-else-if="!synched"
            :disabled="!canConnect"
            :loading="connecting"
            class="btn btn-primary"
            @click="$refs.womConnection.connect()">
            {{ $t('wom.select') }}
          </v-btn>
        </template>
        <template v-else>
          <v-spacer />
          <v-btn
            class="btn me-4"
            @click="edit = false">
            {{ $t('wom.cancel') }}
          </v-btn>
        </template>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    drawer: false,
    initialized: false,
    expanded: false,
    modified: false,
    loading: false,
    canConnect: false,
    connecting: false,
    disconnecting: false,
    synched: false,
    edit: false,
    deed: null,
    womConnectionParams: null,
    deedManagerAddress: null,
    operationSuccess: false,
    hub: null,
  }),
  computed: {
    connected() {
      return this.hub?.enabled && !!this.hub?.address && this.hub.deedId >= 0;
    },
    confirmCloseLabels() {
      return {
        title: this.$t('wom.confirmCancelConnect'),
        message: this.$t('wom.confirmCancelConnectMessage'),
        ok: this.$t('wom.yes'),
        cancel: this.$t('wom.no'),
      };
    },
  },
  watch: {
    hub() {
      this.$root.$emit('wom-hub-changed', this.hub);
    },
    loading() {
      if (!this.loading) {
        this.initialized = true;
        this.edit = false;
      }
    },
    connected() {
      this.reset();
    },
  },
  created() {
    this.$root.$on('wom-connection-success', this.refreshFromWoM);
    this.$root.$on('wom-disconnection-success', this.refreshFromWoM);
  },
  methods: {
    reset() {
      this.modified = false;
      this.loading = false;
      this.canConnect = false;
      this.connecting = false;
      this.synched = false;
      this.womConnectionParams = null;
      this.deedManagerAddress = null;
      this.deed = null;
    },
    selectDeed(deedManagerAddress, deed, womConnectionParams) {
      this.deedManagerAddress = deedManagerAddress;
      this.deed = deed;
      this.womConnectionParams = womConnectionParams;
    },
    open() {
      this.reset();
      this.edit = false;
      this.operationSuccess = false;
      this.$refs.drawer.open();
      this.refresh();
    },
    close() {
      this.$refs.drawer.close();
    },
    refreshFromWoM() {
      this.operationSuccess = true;
      this.refresh(true);
    },
    refresh(forceRefresh) {
      this.loading = true;
      return this.$hubService.getHub(null, forceRefresh)
        .then(hub => this.hub = hub)
        .finally(() => this.loading = false);
    },
  },
};
</script>
