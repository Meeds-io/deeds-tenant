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
    :loading="disconnecting"
    go-back-button
    right>
    <template #title>
      {{ $t('uem.womDisconnection') }}
    </template>
    <template #content>
      <div class="d-flex flex-column pa-5">
        <div class="text-subtitle-1 font-weight-bold mx-auto mb-5">{{ $t('uem.disconnectQuestion') }}</div>
        <div class="mb-5">{{ $t('uem.disconnectDescription') }}</div>
        <wom-setup-disconnect-button :hub="hub" class="mx-auto" />
      </div>
    </template>
  </exo-drawer>
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
    disconnecting: false,
    disconnectAbi: [
      'function disconnect(address _hubAddress)',
    ],
  }),
  computed: {
    hubAddress() {
      return this.hub?.address;
    },
    womAddress() {
      return this.hub?.womAddress;
    },
  },
  watch: {
    disconnecting() {
      this.$emit('disconnecting', this.loading);
    },
  },
  methods: {
    open() {
      this.$refs.drawer.open();
    },
    close() {
      this.$refs.drawer.close();
    },
  },
};
</script>