<template>
  <div v-if="statusTitle">
    <v-menu
      v-model="menu"
      :close-on-content-click="false"
      max-width="300"
      open-on-hover
      attach
      offset-y>
      <template #activator="{ on, attrs }">
        <v-btn
          :href="fullReportUrl"
          :color="color"
          target="_blank"
          rel="nofollow noreferrer noopener"
          outlined
          icon
          small
          class="border-color my-n1"
          v-bind="attrs"
          v-on="on">
          <v-icon
            :color="color"
            :size="iconSize">
            {{ icon }}
          </v-icon>
        </v-btn>
      </template>
      <v-card class="d-flex flex-column pa-4">
        <div v-if="errorMessage" v-sanitized-html="errorMessage"></div>
        <div v-else v-sanitized-html="statusTitle"></div>
      </v-card>
    </v-menu>
  </div>
</template>
<script>
export default {
  props: {
    report: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    menu: false,
  }),
  computed: {
    womServerUrl() {
      return this.hub?.womServerUrl;
    },
    meedsServerUrl() {
      return !this.womServerUrl || this.womServerUrl?.includes?.('wom.meeds.io') ? 'https://www.meeds.io' : this.womServerUrl.replace('/api', '');
    },
    fullReportUrl() {
      if (!this.meedsServerUrl || !this.reportId) {
        return null;
      }
      return `${this.meedsServerUrl}?report=${this.reportId}`;
    },
    hub() {
      return this.$root.hub;
    },
    connectedToWoM() {
      return !!this.hub?.connected;
    },
    status() {
      return this.report?.status;
    },
    isErrorSending() {
      return this.status === 'ERROR_SENDING';
    },
    errorMessage() {
      const messageKey = this.isErrorSending && this.$hubReportService.getErrorKey(this.report?.error);
      return this.isErrorSending && (this.$te(messageKey) ? this.$t(messageKey) : this.statusTitle) || null;
    },
    hubJoinDate() {
      return this.hub?.createdDate && new Date(this.hub?.createdDate);
    },
    reportEndDate() {
      return this.report?.toDate && new Date(this.report?.toDate);
    },
    outdatedReport() {
      return this.hubJoinDate && this.reportEndDate && this.hubJoinDate.getTime() > this.reportEndDate.getTime();
    },
    enableMoreActions() {
      return this.isErrorSending;
    },
    icon() {
      switch (this.status) {
      case 'NONE': return 'fa-info-circle';
      case 'INVALID': return 'fa-info-circle';
      case 'SENT': return 'fa-hourglass-start';
      case 'ERROR_SENDING': return 'fa-exclamation-triangle';
      case 'PENDING_REWARD': return 'fa-hourglass-half';
      case 'REWARDED': return 'fa-check-circle';
      case 'REJECTED': return 'fa-exclamation-circle';
      default: return '';
      }
    },
    iconSize() {
      switch (this.status) {
      case 'ERROR_SENDING': return 14;
      default: return 16;
      }
    },
    color() {
      switch (this.status) {
      case 'NONE': return 'grey';
      case 'INVALID': return 'grey darken-2';
      case 'SENT': return 'blue';
      case 'ERROR_SENDING': return 'orange';
      case 'PENDING_REWARD': return 'green darken-2';
      case 'REWARDED': return 'green';
      case 'REJECTED': return 'red';
      default: return '';
      }
    },
    statusTitle() {
      switch (this.status) {
      case 'NONE': {
        if (!this.connectedToWoM) {
          return null;
        } else if (this.outdatedReport) {
          return this.$t('wom.outdatedReport');
        } else {
          return this.$t('wom.notSentReportYet');
        }
      }
      case 'INVALID': return null;
      case 'SENT': return this.$t('wom.sentReport');
      case 'ERROR_SENDING': return this.$t('wom.errorSendingReport');
      case 'PENDING_REWARD': return this.$t('wom.pendingWoMRewardTransaction');
      case 'REWARDED': return this.$t('wom.hubRewardSent');
      case 'REJECTED': return this.$t('wom.hubRewardRejected');
      default: return null;
      }
    },
  },
};
</script>