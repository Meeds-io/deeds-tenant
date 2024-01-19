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
    :close-confirm="modified"
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
        v-if="!connected"
        class="pa-5"
        flat>
        <template v-if="!modified">
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
          :hub="hub"
          @modified="modified = $event" />
      </v-card>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    drawer: false,
    modified: false,
    loading: false,
    initialized: false,
    expanded: false,
    hub: null,
  }),
  computed: {
    connected() {
      return !!this.hub?.address && this.hub.deedId >= 0;
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
      if (!this.loading && !this.initialized) {
        this.initialized = true;
      }
    },
  },
  created() {
    this.$root.$on('wom-connection-success', this.refresh);
    this.$root.$on('wom-disconnection-success', this.refresh);
  },
  methods: {
    open() {
      this.$refs.drawer.open();
      this.refresh();
    },
    close() {
      this.$refs.drawer.close();
    },
    refresh() {
      this.loading = true;
      return this.$womService.getHub()
        .then(hub => this.hub = hub)
        .finally(() => this.loading = false);
    },
  },
};
</script>
