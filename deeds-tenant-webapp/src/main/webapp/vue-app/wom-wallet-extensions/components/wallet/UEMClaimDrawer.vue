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
    :loading="loading || claiming"
    :confirm-close="modified"
    :confirm-close-labels="confirmCloseLabels"
    allow-expand
    right
    @expand-updated="expanded = $event"
    @closed="modified = false">
    <template #title>
      {{ $t('uem.claimRewards') }}
    </template>
    <template #content>
      <div class="ma-5">
        <div v-if="!connected">
          <div
            v-sanitized-html="notConnectedLabel1"
            class="text-color text-start mb-2"></div>
          <div
            v-sanitized-html="notConnectedLabel2"
            class="text-color text-start mb-2"></div>
        </div>
        <div v-else-if="!loading && !claimableAmount">
          <div
            v-sanitized-html="noRewardsLabel1"
            class="text-color text-start mb-2"></div>
          <div
            v-sanitized-html="noRewardsLabel2"
            class="text-color text-start mb-2"></div>
        </div>
        <div v-else-if="!loading">
          <div
            v-sanitized-html="claimRewardsLabel1"
            class="text-color text-start mb-2"></div>
          <div
            v-sanitized-html="claimRewardsLabel2"
            class="text-color text-start mb-2"></div>
        </div>
      </div>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-spacer />
        <v-btn
          :disabled="claiming"
          class="btn me-4"
          @click="close">
          {{ $t('wom.cancel') }}
        </v-btn>
        <template v-if="connected">
          <uem-claim-button
            v-if="claimableAmount"
            :hub="hub"
            class="btn btn-primary"
            @connecting="connecting = $event" />
          <v-btn
            v-else
            :loading="loading"
            class="primary-border-color"
            color="primary"
            elevation="0"
            outlined
            @click="$emit('refresh', true)">
            {{ $t('uem.refresh') }}
          </v-btn>
        </template>
        <v-btn
          v-else
          :href="womConnectionUri"
          class="btn btn-primary">
          {{ $t('uem.connect') }}
        </v-btn>
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
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    womConnectionUri: '/portal/administration/home/recognition/setup#wom',
    rewardingUri: '/portal/administration/home/recognition/reward',
    dayInSeconds: 86400000,
    drawer: false,
    claiming: false,
    modified: false,
    claimedAmount: 0,
    lastRefreshTime: null,
    address: null,
    signature: 0,
    language: eXo.env.portal.language,
    token: parseInt(Math.random() * 100000),
  }),
  computed: {
    isMobile() {
      return this.$vuetify.breakpoint.mobile;
    },
    claimableAmountFormatted() {
      return new Intl.NumberFormat(this.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      }).format(this.claimableAmount || 0);
    },
    hubDeedId() {
      return this.hub?.deedId;
    },
    shouldRefresh() {
      return !this.lastRefreshTime || (Date.now() - parseInt(this.lastRefreshTime)) > this.dayInSeconds;
    },
    connected() {
      return this.hub?.connected && this.hub?.address && this.hubDeedId > 0;
    },
    claimableAmount() {
      if (!this.connected) {
        return 0;
      } else if (this.deedOwnerAddress === this.deedManagerAddress) {
        return this.hub?.ownerClaimableAmount || 0;
      } else {
        return this.hub?.managerClaimableAmount || 0;
      }
    },
    deedManagerAddress() {
      return this.hub?.deedManagerAddress?.toLowerCase?.();
    },
    deedOwnerAddress() {
      return this.hub?.deedOwnerAddress?.toLowerCase?.();
    },
    noRewardsLabel1() {
      return this.$t('uem.noRewardsToClaim.part1');
    },
    noRewardsLabel2() {
      return this.$t('uem.noRewardsToClaim.part2', {
        0: `<a href="${this.rewardingUri}" target="_blank">`,
        1: '</a>',
        2: '<strong>',
        3: '</strong>',
      });
    },
    notConnectedLabel1() {
      return this.$t('uem.connectToWoMToGetRewards.part1');
    },
    notConnectedLabel2() {
      return this.$t('uem.connectToWoMToGetRewards.part2', {
        0: `<a href="${this.womConnectionUri}" target="_blank">`,
        1: '<a href="https://www.meeds.io/whitepaper" target="_blank">',
        2: '</a>',
        3: '<strong>',
        4: '</strong>',
      });
    },
    claimRewardsLabel1() {
      return this.$t('uem.claimRewardsLabel.part1', {
        0: '<strong>',
        1: this.hubDeedId,
        2: '</strong>',
      });
    },
    claimRewardsLabel2() {
      return this.$t('uem.claimRewardsLabel.part2', {
        0: '<strong>',
        1: this.claimableAmountFormatted,
        2: '</strong>',
      });
    },
  },
  created() {
    this.$root.$on('uem-claim-success', this.close);
  },
  methods: {
    open() {
      this.lastRefreshTime = localStorage.getItem('uem-claimable-refresh-time');
      this.$emit('refresh', this.shouldRefresh);
      this.$refs.drawer.open();
      if (this.shouldrefresh) {
        this.lastRefreshTime = String(Date.now());
        localStorage.setItem('uem-claimable-refresh-time', this.lastRefreshTime);
      }
    },
    close() {
      this.$refs.drawer.close();
    },
  },
};
</script>
