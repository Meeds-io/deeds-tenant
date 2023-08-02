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
  <v-card
    :loading="loading"
    min-height="200"
    flat>
    <template v-if="!loading">
      <template v-if="connected">
        <v-list-item-title class="text-subtitle-1 font-weight-bold">{{ $t('wom.connectedDeedToWoM') }}</v-list-item-title>
        <wom-integration-hub-card
          :hub="hub"
          class="mx-auto"
          @edit="$refs.connectionDrawer.open()"
          @disconnect="$refs.disconnectionDrawer.open()" />
      </template>
      <v-list-item
        v-else
        three-line>
        <v-list-item-content>
          <v-list-item-title class="text-subtitle-1 font-weight-bold">{{ $t('wom.connectToWoM') }}</v-list-item-title>
          <v-list-item-subtitle v-sanitized-html="$t('wom.connectToWoMSummary1')" />
          <v-list-item-subtitle v-sanitized-html="connectToWoMSummary2" />
        </v-list-item-content>
        <v-list-item-action>
          <v-btn class="btn primary" @click="$refs.connectionDrawer.open()">{{ $t('wom.connect') }}</v-btn>
        </v-list-item-action>
      </v-list-item>
    </template>
    <wom-integration-connection-drawer
      ref="connectionDrawer"
      :hub="hub" />
    <wom-integration-disconnection-drawer
      ref="disconnectionDrawer" />
  </v-card>
</template>
<script>
export default {
  data: () => ({
    hub: null,
    loading: true,
  }),
  computed: {
    connected() {
      return !!this.hub?.address && this.hub.deedId >= 0;
    },
    connectToWoMSummary2() {
      return this.$t('wom.connectToWoMSummary2', {
        0: '<a href="https://www.meeds.io/whitepaper" target="_blank">',
        1: '</a>',
      });
    },
  },
  created() {
    this.$root.$on('wom-connection-success', this.refresh);
    this.$root.$on('wom-disconnection-success', this.refresh);
    this.refresh();
  },
  methods: {
    refresh() {
      this.loading = true;
      return this.$tenantService.getHub()
        .then(hub => this.hub = hub)
        .finally(() => this.loading = false);
    },
  },
};
</script>