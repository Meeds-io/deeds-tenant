<template>
  <component
    :is="expand && 'v-dialog' || 'v-card'"
    v-on="componentEvents"
    v-bind="componentProps">
    <v-scale-transition>
      <v-card
        :loading="loading"
        class="overflow-y-auto fill-height"
        flat>
        <v-card-title class="headine d-flex flex-sm-nowrap">
          <v-icon color="secondary" class="d-none d-sm-block">fa-calendar</v-icon>
          <div
            :class="expand && 'justify-start ms-0 ms-sm-2' || 'justify-center'"
            class="d-flex flex-column flex-sm-row flex-grow-1 flex-shrink-1 align-center">
            <date-format :value="fromDate" class="text-break" />
            <span class="mx-2 text-no-wrap">{{ $t('wom.toDate') }}</span>
            <date-format :value="toDate" class="text-break" />
          </div>
          <wom-setup-hub-reward-item-menu
            :report="report"
            :loading="loading"
            :expanded="expand"
            @refresh="refresh"
            @send="send"
            @expand="$emit('expand')"
            @collapse="$emit('collapse')" />
        </v-card-title>
        <v-card-text
          :class="expand && 'mt-4 pb-6' || 'mt-n2 pb-2'"
          class="d-flex flex-wrap">
          <wom-setup-hub-reward-status
            :report="report"
            class="me-2 mt-2" />
          <wom-setup-hub-blockchain-chip
            :network-id="blockchainNetworkId"
            class="mt-2 me-2" />
          <wom-setup-address
            :title="$t('wom.tokenAddress')"
            :address="tokenAddress"
            :network-id="blockchainNetworkId"
            class="mt-2 me-2"
            small
            token />
        </v-card-text>
        <v-list :class="expand && 'py-0 px-2' || 'pa-0'">
          <v-row
            class="mx-0 border-box-sizing"
            no-gutters
            dense>
            <v-col
              cols="12"
              sm="6">
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title :title="$t('wom.participantsCount')">{{ $t('wom.participantsCount') }}</v-list-item-title>
                  <v-list-item-subtitle>{{ participantsCount }} {{ $t('wom.users') }}</v-list-item-subtitle>
                </v-list-item-content>
              </v-list-item>
            </v-col>
            <v-col
              cols="12"
              sm="6">
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title :title="$t('wom.recipientsCount')">{{ $t('wom.recipientsCount') }}</v-list-item-title>
                  <v-list-item-subtitle>{{ recipientsCount }} {{ $t('wom.users') }}</v-list-item-subtitle>
                </v-list-item-content>
              </v-list-item>
            </v-col>
            <v-col
              cols="12"
              sm="6">
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title :title="$t('wom.achievementsCount')">{{ $t('wom.achievementsCount') }}</v-list-item-title>
                  <v-list-item-subtitle>{{ achievementsCount }} {{ $t('wom.achievements') }}</v-list-item-subtitle>
                </v-list-item-content>
              </v-list-item>
            </v-col>
            <v-col
              cols="12"
              sm="6">
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title :title="$t('wom.usersRewardAmount')">{{ $t('wom.usersRewardAmount') }}</v-list-item-title>
                  <v-list-item-subtitle>{{ hubRewardAmount }} Ɱ</v-list-item-subtitle>
                </v-list-item-content>
              </v-list-item>
            </v-col>
          </v-row>
        </v-list>
        <wom-setup-hub-reward-users-list
          v-if="expand"
          :from-date="fromDate"
          :to-date="toDate"
          class="px-6 mt-8" />
      </v-card>
    </v-scale-transition>
  </component>
</template>
<script>
export default {
  props: {
    report: {
      type: Object,
      default: null,
    },
    expand: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    loading: false,
  }),
  computed: {
    componentProps() {
      return this.expand && {
        value: true,
        eager: true,
        fullscreen: true,
      } || {
        elevation: 0,
        hover: true,
        class: 'border-color',
      };
    },
    componentEvents() {
      return this.expand && {
        input: opened => {
          if (!opened) {
            this.$emit('collapse');
          }
        }
      } || {};
    },
    fromDate() {
      return this.report?.fromDate;
    },
    toDate() {
      return this.report?.toDate;
    },
    participantsCount() {
      return new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(this.report?.participantsCount || 0);
    },
    recipientsCount() {
      return new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(this.report?.recipientsCount || 0);
    },
    achievementsCount() {
      return new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(this.report?.achievementsCount || 0);
    },
    hubRewardAmount() {
      return new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(this.report?.hubRewardAmount || 0);
    },
    blockchainNetworkId() {
      return this.report?.rewardTokenNetworkId || 0;
    },
    tokenAddress() {
      return this.report?.rewardTokenAddress || 0;
    },
  },
  methods: {
    send() {
      this.loading = true;
      return this.$womReportService.sendReport(this.report?.id)
        .then(report => this.$emit('refresh', report))
        .then(() => this.$root.$emit('alert-message', this.$t('wom.reportSentSuccessfully'), 'success'))
        .catch(e => {
          const error = (e?.cause || String(e));
          const errorMessageKey = error.includes('wom.') && `wom.${error.split('wom.')[1]}` || error;
          this.$root.$emit('alert-message', this.$t(errorMessageKey), 'error');
        })
        .finally(() => this.loading = false);
    },
    refresh() {
      this.loading = true;
      return this.$womReportService.getReport(this.report?.id, true)
        .then(report => this.$emit('refresh', report))
        .then(() => this.$root.$emit('alert-message', this.$t('wom.reportRefreshedSuccessfully'), 'success'))
        .catch(e => {
          const error = (e?.cause || String(e));
          const errorMessageKey = error.includes('wom.') && `wom.${error.split('wom.')[1]}` || error;
          this.$root.$emit('alert-message', this.$t(errorMessageKey), 'error');

          return this.$womReportService.getReport(this.report?.id)
            .then(report => this.$emit('refresh', report))
            .catch(() => this.$emit('report-not-found', this.report?.id));
        })
        .finally(() => this.loading = false);
    },
  },
};
</script>