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
    v-if="periodEndDate
      && !periodUpcoming
      && (!loading || !hub)"
    :type="alertType"
    border="left"
    elevation="2"
    class="ma-0"
    outlined
    colored-border>
    <div class="text-color text-start">
      <template v-if="!connected">
        <div v-sanitized-html="notConnectedLabel1" class="mb-2"></div>
        <div v-sanitized-html="notConnectedLabel2"></div>
      </template>
      <template v-else-if="!loading && periodNonEligible">
        {{ $t('uem.periodNonEligible') }}
        <strong class="text-center d-inline-flex">
          <date-format :value="hubJoinDate" />
        </strong>
      </template>
      <template v-else-if="!loading && periodNotEnded">
        {{ $t('uem.periodNotEnded') }}
      </template>
      <template v-else-if="!loading && periodWithoutRewards">
        {{ $t('uem.periodWithoutRewards') }}
      </template>
      <template v-else-if="!loading && reportTransactionSent">
        <span v-sanitized-html="reportSentLabel"></span>
      </template>
      <template v-else-if="!loading && reportTransactionSending">
        {{ $t('uem.reportTransactionSending') }}
      </template>
      <template v-else-if="!loading && reportTransactionError">
        <div class="mb-4">{{ $t('uem.reportTransactionError') }}</div>
        <div v-sanitized-html="reportErrorMessage" class="error--text mb-4"></div>
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
      <template v-else-if="!loading && sendingWalletReward">
        {{ $t('uem.sendingWalletRewards') }}
      </template>
      <template v-else-if="!loading && rewardNotSentYet">
        {{ $t('uem.rewardNotSentYet') }}
      </template>
      <template v-else-if="!loading && reportTransactionNotSent">
        <div class="mb-4">{{ $t('uem.reportTransactionNotSent') }}</div>
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
    womServerUrl() {
      return this.hub?.womServerUrl;
    },
    meedsServerUrl() {
      return !this.womServerUrl || this.womServerUrl?.includes?.('wom.meeds.io') ? 'https://www.meeds.io/hubs' : this.womServerUrl.replace('/api', '');
    },
    fullReportUrl() {
      if (!this.meedsServerUrl || !this.reportId) {
        return null;
      }
      return `${this.meedsServerUrl}?report=${this.reportId}`;
    },
    reportSentLabel() {
      return this.reportSentThisWeek && this.$t('uem.reportTransactionSentThisWeek', {
        0: `<a href="${this.fullReportUrl}" target="_blank">`,
        2: '</a>',
      }) ||  this.$t('uem.reportTransactionSent', {
        0: `<a href="${this.fullReportUrl}" target="_blank">`,
        2: '</a>',
      });
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
    periodEndDatePlusWeek() {
      return this.periodEndDate + 604800000;
    },
    periodNonEligible() {
      return this.periodEndDatePlusWeek < this.hubJoinDate;
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
    rewardNotSentYet() {
      return !this.periodWithoutRewards
        && !this.sendingWalletReward
        && !this.rewardReport?.successTransactionCount
        && this.rewardReport?.tokensToSend;
    },
    reportErrorObj() {
      return this.report?.error?.length
        && (this.report.error.indexOf('{') === 0)
        && JSON.parse(this.report.error);
    },
    shouldRetry() {
      return this.reportErrorObj?.shouldRetry;
    },
    reportErrorMessageKey() {
      return this.reportErrorObj?.messageKey;
    },
    reportErrorMessage() {
      return this.reportErrorMessageKey
        && this.$te(this.reportErrorMessageKey)
        && this.$t(this.reportErrorMessageKey, {
          0: '<strong>',
          1: '</strong>',
        })
        || this.$t('uem.unknownErrorSendingReport');
    },
    reportId() {
      return this.report?.reportId;
    },
    reportTransactionSent() {
      return this.reportId;
    },
    reportTransactionError() {
      return this.report?.error;
    },
    reportTransactionSending() {
      return this.report?.status === 'SENDING';
    },
    reportTransactionNotSent() {
      return !this.periodWithoutRewards
        && !this.rewardNotSentYet
        && !this.reportTransactionSent
        && !this.reportTransactionSending;
    },
    reportSentDate() {
      return this.report?.sentDate && new Date(this.report?.sentDate).getTime() || null;
    },
    uemCurrentWeekStartPeriod() {
      const date = new Date();
      const day = date.getDay();
      const diffDays = date.getDate() - day + (day && 1 || -6);
      date.setDate(diffDays);
      date.setUTCMilliseconds(0);
      date.setUTCSeconds(0);
      date.setUTCMinutes(0);
      date.setUTCHours(0);
      return date.getTime();
    },
    reportSentThisWeek() {
      return this.reportSentDate && this.reportSentDate > this.uemCurrentWeekStartPeriod;
    },
    alertType() {
      if (this.reportTransactionSent) {
        return 'success';
      } else if (this.reportTransactionError) {
        return 'warning';
      } else {
        return 'info';
      }
    },
  },
  watch: {
    connected() {
      if (this.periodId && this.connected) {
        this.getReport();
      }
    },
    rewardReport(newVal, oldVal) {
      if (this.periodId && this.connected) {
        this.getReport();
      } else if (newVal && oldVal) {
        this.report = null;
      }
    },
  },
  created() {
    this.getHub();
    document.addEventListener('deed.tenant.report.sent', this.refreshReportFromTriggeredEvent);
    document.addEventListener('deed.tenant.report.sending', this.refreshReportFromTriggeredEvent);
    document.addEventListener('deed.tenant.report.error', this.refreshReportFromTriggeredEvent);
  },
  beforeDestroy() {
    document.removeEventListener('deed.tenant.report.sent', this.refreshReportFromTriggeredEvent);
    document.removeEventListener('deed.tenant.report.sending', this.refreshReportFromTriggeredEvent);
    document.removeEventListener('deed.tenant.report.error', this.refreshReportFromTriggeredEvent);
  },
  methods: {
    resendReport() {
      this.sending = true;
      return this.$hubReportService.sendReport(this.periodId)
        .then(() => {
          // refresh report async
          this.getReport();
        })
        .finally(() => this.sending = false);
    },
    refreshReportFromTriggeredEvent(event) {
      if (event?.detail?.long === this.periodId) {
        this.report = null;
        this.getReport();
      }
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
        .then(hub => this.hub = hub)
        .finally(() => this.loading = false);
    },
  },
};
</script>
