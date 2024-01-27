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
  <v-alert
    v-if="periodEndDate && (!loading || !hub)"
    type="information"
    border="left"
    elevation="2"
    outlined
    colored-border>
    <div class="text-color text-start my-2">
      <template v-if="!connected">
        <div v-sanitized-html="notConnectedLabel1"></div>
        <div v-sanitized-html="notConnectedLabel2"></div>
      </template>
      <template v-else-if="!loading && periodNonEligible">
        {{ $t('uem.periodNonEligible') }}
      </template>
      <template v-else-if="!loading && periodNotEnded">
        {{ $t('uem.periodNotEnded') }}
      </template>
      <template v-else-if="!loading && periodUpcoming">
        {{ $t('uem.periodUpcoming') }}
      </template>
      <template v-else-if="!loading && periodWithoutRewards">
        {{ $t('uem.periodWithoutRewards') }}
      </template>
      <template v-else-if="!loading && sendingWalletReward">
        {{ $t('uem.sendingWalletRewards') }}
      </template>
      <template v-else-if="!loading && reportTransactionSending">
        {{ $t('uem.reportTransactionSending') }}
      </template>
      <template v-else-if="!loading && reportTransactionSent">
        {{ $t('uem.reportTransactionSent') }}
      </template>
      <template v-else-if="!loading && reportTransactionError">
        <div class="mb-4">{{ $t('uem.reportTransactionError') }}</div>
        <div class="error--text mb-4">{{ reportError }}</div>
        <v-btn
          :loading="sending"
          class="primary-border-color"
          color="primary"
          elevation="0"
          outlined
          @click="resendReport">
          {{ $t('uem.retry') }}
        </v-btn>
      </template>
    </div>
  </v-alert>
</template>
<script>
export default {
  props: {
    rewardReport: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    womConnectionUri: '/portal/administration/home/recognition/setup#wom',
    rewardingUri: '/portal/administration/home/recognition/reward',
    hub: null,
    report: null,
    loading: true,
    sending: false,
  }),
  computed: {
    periodId() {
      return this.rewardReport?.period?.id;
    },
    hubDeedId() {
      return this.hub?.deedId;
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
    connected() {
      return this.hub?.connected && this.hub?.address && this.hubDeedId > 0;
    },
    completelyProceeded() {
      return this.rewardReport?.completelyProceeded;
    },
    hubJoinDate() {
      return this.hub?.joinDate && new Date(this.hub.joinDate).getTime();
    },
    periodStartDate() {
      return this.rewardReport?.period?.startDateInSeconds && this.rewardReport?.period?.startDateInSeconds * 1000;
    },
    periodEndDate() {
      return this.rewardReport?.period?.endDateInSeconds && this.rewardReport?.period?.endDateInSeconds * 1000;
    },
    periodNonEligible() {
      return this.hubJoinDate > this.periodEndDate;
    },
    periodNotEnded() {
      return this.periodStartDate <= Date.now() && this.periodEndDate > Date.now();
    },
    periodUpcoming() {
      return this.periodStartDate > Date.now();
    },
    periodWithoutRewards() {
      return !this.rewardReport?.validRewardCount;
    },
    sendingWalletReward() {
      return this.rewardReport?.pendingTransactionCount;
    },
    shouldRetry() {
      if (this.report?.error && this.report.error.indexOf('{') === 0) {
        const error = JSON.parse(this.report.error);
        return error?.shouldRetry;
      }
      return false;
    },
    reportError() {
      if (!this.report?.error) {
        return null;
      } else if (this.report.error.indexOf('{') === 0) {
        const error = JSON.parse(this.report.error);
        return error?.messageKey && this.$te(error?.messageKey) && this.$t(error?.messageKey) || error?.message || this.$t('uem.unknownErrorSendingReport');
      } else {
        return this.report.error;
      }
    },
    reportTransactionError() {
      return this.reportError;
    },
    reportTransactionSent() {
      return this.report?.status === 'SENT';
    },
    reportTransactionSending() {
      return this.report?.status === 'SENDING';
    },
  },
  watch: {
    connected() {
      console.warn('connected', this.periodId, this.connected);
      if (this.periodId && this.connected) {
        this.getReport();
      }
    },
    periodId() {
      console.warn('periodId', this.periodId, this.connected);
      if (this.periodId && this.connected) {
        this.getReport();
      }
    },
  },
  created() {
    this.getHub();
  },
  methods: {
    resendReport() {
      this.sending = true;
      return this.$hubReportService.sendReport(this.periodId)
        .then(() => this.$hubReportService.getReport(this.periodId, true))
        .finally(() => this.sending = false);
    },
    getReport() {
      this.loading = true;
      return this.$hubReportService.getReport(this.periodId)
        .then(report => this.report = report)
        .finally(() => this.loading = false);
    },
    getHub() {
      this.loading = true;
      return this.$hubService.getHub()
        .then(hub => {
          this.hub = hub;
          return this.$nextTick();
        })
        .finally(() => this.loading = false);
    },
  },
};
</script>
