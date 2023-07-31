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
  <div>
    <v-list-item three-line>
      <v-list-item-content>
        <v-list-item-title>{{ $t('wom.connectToWoM') }}</v-list-item-title>
        <v-list-item-subtitle v-sanitized-html="$t('wom.connectToWoMSummary1')" />
        <v-list-item-subtitle v-sanitized-html="connectToWoMSummary2" />
      </v-list-item-content>
      <v-list-item-action>
        <v-btn class="btn primary" @click="$refs.connectionDrawer.open()">{{ $t('wom.connect') }}</v-btn>
      </v-list-item-action>
    </v-list-item>
    <wom-integration-connection-drawer
      ref="connectionDrawer" />
  </div>
</template>
<script>
export default {
  data: () => ({
    connected: false,
    loading: true,
  }),
  computed: {
    connectToWoMSummary2() {
      return this.$t('wom.connectToWoMSummary2', {
        0: '<a href="https://www.meeds.io/whitepaper" target="_blank">',
        1: '</a>',
      });
    },
  },
};
</script>