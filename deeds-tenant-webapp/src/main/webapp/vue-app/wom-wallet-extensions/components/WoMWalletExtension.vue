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
    <v-btn
      color="primary"
      elevation="0"
      @click="open">
      {{ $t('uem.claim') }}
    </v-btn>
    <uem-claim-drawer
      ref="drawer"
      :hub="hub"
      :loading="loading"
      @refresh="refresh" />
  </div>
</template>
<script>
export default {
  data: () => ({
    hub: null,
    loading: false,
  }),
  created() {
    this.$root.$on('uem-claim-success', this.refreshAfterClaim);
  },
  mounted() {
    this.refresh();
  },
  methods: {
    open() {
      this.$refs.drawer.open();
    },
    refreshAfterClaim() {
      document.dispatchEventListener(new CustomEvent('wallet-admin-refresh'));
      this.refresh(true);
    },
    refresh(forceRefresh) {
      this.loading = true;
      return this.$hubService.getHub(null, forceRefresh)
        .then(hub => this.hub = hub)
        .finally(() => {
          this.loading = false;
          if (forceRefresh) {
            localStorage.setItem('uem-claimable-refresh-time', String(Date.now()));
          }
        });
    },
  },
};
</script>
