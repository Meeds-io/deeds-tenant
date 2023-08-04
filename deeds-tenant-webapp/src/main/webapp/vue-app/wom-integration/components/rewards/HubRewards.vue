<template>
  <v-card :loading="loading" flat>
    <v-list-item two-line>
      <v-list-item-content>
        <v-list-item-title class="text-subtitle-1 font-weight-bold">{{ $t('wom.UEMRewards') }}</v-list-item-title>
        <v-list-item-subtitle>{{ $t('wom.UEMRewardsSummary') }}</v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-data-iterator
      :items="reports"
      :loading="loading"
      item-key="id"
      hide-default-footer>
      <template #no-data>
        <div class="align-center justify-center text-subtitle dark-grey-color">
          <v-icon
            size="36"
            color="secondary"
            class="me-4">
            fa-hand-holding-usd
          </v-icon>
          {{ $t('womn.noUEMRewards') }}
        </div>
      </template>
      <template #default="props">
        <v-row class="mx-0 border-box-sizing">
          <v-col
            v-for="(item, index) in props.items"
            :key="item.id"
            cols="12"
            sm="12"
            md="6"
            lg="4">
            <wom-integration-hub-reward-item
              :key="`report-${item.id}`"
              :report="item"
              :expand="expandedIndex === index"
              @refresh="refreshReport"
              @expand="expandedIndex = index"
              @collapse="expandedIndex = null" />
          </v-col>
        </v-row>
      </template>
    </v-data-iterator>
    <v-btn
      v-if="hasMore"
      class="btn"
      block
      @click="loadMore">
      {{ $t('wom.loadMore') }}
    </v-btn>
  </v-card>
</template>
<script>
export default {
  data: () => ({
    reports: [],
    loading: false,
    pageSize: 10,
    page: 0,
    hasMore: false,
    expandedIndex: null,
  }),
  computed: {
    offset() {
      return (this.page - 1) * this.pageSize;
    },
    limit() {
      return this.pageSize;
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.loadMore();
    },
    loadMore() {
      this.page++;
      this.loading = true;
      return this.$hubReportService.getReports(this.offset, this.limit + 1)
        .then(reports => {
          if (reports?.length) {
            this.reports.push(...reports.slice(0, this.limit));
            this.hasMore = reports && reports.length > this.limit;
          } else {
            this.hasMore = false;
          }
        })
        .finally(() => this.loading = false);
    },
    refreshReport(report) {
      if (!report) {
        return;
      }
      const index = this.reports.findIndex(r => report.id === r.id);
      this.reports.splice(index, 1, report);
      this.reports = this.reports.slice();
    },
  },
};
</script>