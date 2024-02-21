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
    <template v-if="connecting && !synched" #title>
      {{ $t('wom.setup.drawer.title.bridging') }}
    </template>
    <template v-else-if="connecting || synched && !connected" #title>
      <div class="d-flex flex-row align-center">
        <v-btn
          class="me-2 ms-n2"
          icon
          @click="$refs.womConnection.clearDeed()">
          <v-icon size="20">
            {{ $vuetify.rtl && 'fa fa-arrow-right' || 'fa fa-arrow-left' }}
          </v-icon>
        </v-btn>
        <span class="align-start text-header-title text-truncate">
          {{ $t('wom.setup.drawer.title.connecting') }}
        </span>
      </div>
    </template>
    <template v-else-if="edit" #title>
      {{ $t('wom.setup.drawer.title.editConnection') }}
    </template>
    <template v-else-if="connected" #title>
      {{ $t('wom.setup.drawer.title.hubCard') }}
    </template>
    <template v-else #title>
      {{ $t('wom.setup.drawer.title.connectToWoM') }}
    </template>
    <template v-if="initialized" #content>
      <v-card
        v-show="!loading"
        class="pa-4"
        flat>
        <template v-if="!connected || edit">
          <wom-disconnected-label
            v-if="disconnected && !reconnect && !modified && !edit"
            class="mb-5" />
          <wom-not-connected-label
            v-if="!disconnected && !modified && !edit" />
        </template>
        <wom-connected-label v-else />
        <wom-setup-hub-card
          v-if="(connected && !edit) || (disconnected && !reconnect)"
          :hub="hub"
          :disabled="disconnected"
          @edit="edit = true" />
        <template v-if="!connected || edit">
          <wom-setup-connection-stepper
            v-if="!disconnected || reconnect"
            ref="womConnection"
            :hub="hub"
            :edit="edit"
            :disabled="connecting || disconnecting"
            :transaction-in-progress="connecting && synched"
            @modified="modified = $event"
            @validated="canConnect = $event"
            @connecting="connecting = $event"
            @connected="synched = $event"
            @select="selectDeed"
            @reconnect="reconnect = false" />
          <div
            v-if="synched && deed"
            class="d-flex">
            <v-slide-y-transition>
              <wom-setup-connect-button
                v-show="!connecting"
                :wom-connection-params="womConnectionParams"
                :deed-manager-address="deedManagerAddress"
                :deed="deed"
                class="mx-auto my-2"
                @connecting="connecting = $event" />
            </v-slide-y-transition>
          </div>
          <wom-setup-disconnection-stepper
            v-if="connected && !deed && !connecting"
            ref="womDisconnection"
            :hub="hub"
            @disconnecting="disconnecting = $event" />
          <div v-if="disconnected && !reconnect" class="d-flex">
            <v-btn
              color="primary"
              elevation="0"
              class="mx-auto my-5"
              @click="reconnect = true">
              {{ $t('wom.update') }}
            </v-btn>
          </div>
        </template>
      </v-card>
    </template>
    <template v-if="!loading && !connecting && !synched && (modified || edit)" #footer>
      <div class="d-flex">
        <template v-if="modified">
          <v-btn
            v-if="connected && !deed && !connecting"
            :disabled="loading"
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
            :disabled="connecting || disconnecting || loading"
            class="btn"
            @click="close">
            {{ $t('wom.cancel') }}
          </v-btn>
          <v-btn
            v-if="!deed && !synched"
            :disabled="!canConnect"
            :loading="connecting"
            class="btn btn-primary ms-4"
            @click="$refs.womConnection.connect()">
            {{ $t('wom.select') }}
          </v-btn>
        </template>
        <template v-else>
          <v-spacer />
          <v-btn
            :disabled="connecting || disconnecting || loading"
            class="btn"
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
    reconnect: false,
    connecting: false,
    disconnecting: false,
    synched: false,
    edit: false,
    deed: null,
    womConnectionParams: null,
    deedManagerAddress: null,
    hub: null,
  }),
  computed: {
    hubDeedId() {
      return this.hub?.deedId;
    },
    connected() {
      return this.hub?.connected && !!this.hub?.address && this.hubDeedId > 0;
    },
    disconnected() {
      return this.hubDeedId && !this.connected;
    },
    confirmCloseLabels() {
      return {
        title: this.$t('wom.confirmCancelConnect'),
        message: this.$t('wom.confirmCancelConnectMessage'),
        ok: this.$t('wom.confirm'),
        cancel: this.$t('wom.cancel'),
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
    hubDeedId() {
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
      this.reconnect = false;
      this.disconnecting = false;
      this.synched = false;
      this.womConnectionParams = null;
      this.deedManagerAddress = null;
      this.deed = null;
    },
    selectDeed(deedManagerAddress, deed, womConnectionParams) {
      if (!deedManagerAddress || !deed || !womConnectionParams) {
        this.deedManagerAddress = null;
        this.deed = null;
        this.womConnectionParams = null;
      } else {
        this.deedManagerAddress = deedManagerAddress;
        this.deed = deed;
        this.womConnectionParams = womConnectionParams;
      }
    },
    open() {
      this.reset();
      this.edit = false;
      this.$refs.drawer.open();
      this.refresh();
    },
    close() {
      this.$refs.drawer.close();
    },
    refreshFromWoM() {
      this.reset();
      this.refresh(true);
    },
    refresh(forceRefresh) {
      this.loading = true;
      return this.$hubService.getHub(forceRefresh)
        .then(hub => this.hub = hub)
        .finally(() => this.loading = false);
    },
  },
};
</script>
