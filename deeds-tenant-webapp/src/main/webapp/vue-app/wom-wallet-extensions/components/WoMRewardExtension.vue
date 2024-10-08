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
  <div v-if="!loading">
    <div v-if="reportId">
      {{ $t('wom.reportSent') }} <a
        :href="fullReportUrl"
        target="_blank"
        class="text-decoration-underline">{{ reportSentDate }}</a>
    </div>
    <div v-else-if="!connected">
      <div>{{ $t('wom.rewardSent') }} {{ rewardSentDate }}</div>
    </div>
    <div v-else-if="connected">
      <div class="text-subtitle text-color d-flex justify-end">{{ $t('wom.rewardSent') }} {{ rewardSentDate }}</div>
      <div v-if="!periodNonEligible" class="text-subtitle d-flex justify-end">
        {{ $t('uem.periodNonEligible') }}
      </div>
    </div>
    <div v-else-if="reportTransactionError || reportTransactionNotSent" class="d-flex">
      <div v-if="sending" class="text-subtitle pe-2 align-self-center">{{ $t('wom.SendingReport') }}... </div>
      <div v-else class="text-subtitle pe-2 align-self-center">
        <v-icon
          color="orange darken-2"
          class="pe-2"
          size="16">
          fas fa-exclamation-triangle
        </v-icon>
        <div v-sanitized-html="reportErrorMessage" class="error--text mb-4"></div>
      </div>
      <v-btn
        :disabled="sending"
        class="btn btn-primary"
        @click="resendReport">
        {{ $t('wom.send') }}
      </v-btn>
    </div>
  </div>
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
    hub: null,
    report: null,
    loading: true,
    sending: false,
    lang: eXo.env.portal.language,
    dateFormat: {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    },
  }),
  computed: {
    hubDeedId() {
      return this.hub?.deedId;
    },
    connected() {
      return this.hub?.connected && this.hub?.address && this.hubDeedId > 0;
    },
    rewardSentDate() {
      const reward = this.rewardReport?.rewards?.find(reward => reward?.transaction?.succeeded);
      const sentDate = new Date(reward?.transaction?.timestamp);
      return sentDate?.toLocaleString(this.lang, this.dateFormat);
    },
    periodId() {
      return this.rewardReport?.period?.id;
    },
    womServerUrl() {
      return this.hub?.womServerUrl;
    },
    meedsServerUrl() {
      return !this.womServerUrl || this.womServerUrl?.includes?.('wom.meeds.io') ? 'https://www.meeds.io/hubs' : `${this.womServerUrl.replace('/api', '')}/hubs`;
    },
    fullReportUrl() {
      if (!this.meedsServerUrl || !this.reportId) {
        return null;
      }
      return `${this.meedsServerUrl}?report=${this.reportId}`;
    },
    reportId() {
      return this.report?.reportId;
    },
    reportSentDate() {
      const sentDate = this.report?.sentDate && new Date(this.report?.sentDate) || null;
      return sentDate?.toLocaleString(this.lang, this.dateFormat);
    },
    reportTransactionError() {
      return this.report?.error;
    },
    periodWithoutRewards() {
      return !this.rewardReport?.validRewardCount;
    },
    sendingWalletReward() {
      return this.rewardReport?.pendingTransactionCount;
    },
    reportTransactionSending() {
      return this.report?.status === 'SENDING';
    },
    rewardNotSentYet() {
      return !this.periodWithoutRewards
          && !this.sendingWalletReward
          && !this.rewardReport?.successTransactionCount
          && this.rewardReport?.tokensToSend;
    },
    reportTransactionNotSent() {
      return !this.periodWithoutRewards
          && !this.rewardNotSentYet
          && !this.reportId
          && !this.reportTransactionSending;
    },
    periodEndDate() {
      return this.rewardReport?.period?.endDateInSeconds && this.rewardReport?.period?.endDateInSeconds * 1000;
    },
    periodEndDatePlusWeek() {
      return this.periodEndDate + 604800000;
    },
    hubJoinDate() {
      return this.hub?.createdDate && new Date(this.hub?.createdDate);
    },
    periodNonEligible() {
      return this.periodEndDatePlusWeek < this.hubJoinDate;
    },
    reportErrorObj() {
      return this.report?.error?.length
          && (this.report.error.indexOf('{') === 0)
          && JSON.parse(this.report.error);
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
  },
  created() {
    this.getHub().then(() => {
      this.$nextTick().then(() => {
        this.getReport();
      });
    });
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
