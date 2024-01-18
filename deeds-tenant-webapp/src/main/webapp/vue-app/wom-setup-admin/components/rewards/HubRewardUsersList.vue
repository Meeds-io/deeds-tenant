<template>
  <component
    :is="$vuetify.breakpoint.mobile && 'v-data-iterator' || 'v-data-table'"
    :loading="loading"
    :headers="headers"
    :items="rewards"
    :items-per-page="10"
    item-key="identityId"
    sort-by="name">
    <template v-if="$vuetify.breakpoint.mobile" #default="props">
      <v-row class="ma-0 pa-0 border-box-sizing" dense>
        <v-col
          v-for="item in props.items"
          :key="item.name"
          cols="12"
          sm="6"
          md="4"
          lg="3">
          <v-card class="border-color" flat>
            <v-card-title class="subheading font-weight-bold text-truncate">
              {{ item.name }}
            </v-card-title>
            <v-divider />
            <v-list dense>
              <v-list-item>
                <v-list-item-content>{{ headers[1].text }}:</v-list-item-content>
                <v-list-item-content class="align-end">
                  <wom-setup-address
                    v-if="item.address"
                    :network-id="networkId"
                    :address="item.address" />
                </v-list-item-content>
              </v-list-item>
              <v-list-item>
                <v-list-item-content>{{ headers[3].text }}:</v-list-item-content>
                <v-list-item-content class="align-end">
                  {{ item.tokensSent }} Ɱ
                </v-list-item-content>
              </v-list-item>
              <v-list-item>
                <v-list-item-content>{{ headers[4].text }}:</v-list-item-content>
                <v-list-item-content class="align-end">
                  {{ item.points }}
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-card>
        </v-col>
      </v-row>
    </template>
    <template v-if="!$vuetify.breakpoint.mobile" #[`item.tokensSent`]="{item}">
      {{ item.tokensSent }} Ɱ
    </template>
    <template v-if="!$vuetify.breakpoint.mobile" #[`item.address`]="{item}">
      <wom-setup-address
        v-if="item.address"
        :network-id="networkId"
        :address="item.address" />
    </template>
    <template v-if="!$vuetify.breakpoint.mobile" #[`item.transactionHash`]="{item}">
      <wom-setup-address
        v-if="item.transactionHash"
        :network-id="networkId"
        :address="item.transactionHash"
        transaction />
    </template>
  </component>
</template>
<script>
export default {
  props: {
    fromDate: {
      type: Object,
      default: null,
    },
    toDate: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    rewards: [],
    loading: false,
  }),
  computed: {
    periodMedian() {
      return new Date((new Date(this.toDate).getTime() + new Date(this.fromDate).getTime()) / 2).toISOString().substring(0, 10);
    },
    networkId() {
      const reward = this.rewards.find(t => t?.transaction?.networkId);
      return reward?.transaction?.networkId;
    },
    headers() {
      return [
        {
          text: this.$t('wom.userName'),
          align: 'start',
          sortable: true,
          value: 'name',
        },
        {
          text: this.$t('wom.userAddress'),
          align: 'center',
          sortable: false,
          value: 'address',
        },
        {
          text: this.$t('wom.transaction'),
          align: 'center',
          sortable: false,
          value: 'transactionHash',
        },
        {
          text: this.$t('wom.amountEarned'),
          align: 'right',
          sortable: true,
          value: 'tokensSent',
        },
        {
          text: this.$t('wom.achivementPoints'),
          align: 'right',
          sortable: true,
          value: 'points',
        },
      ];
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.loading = true;
      this.$womReportService.getLocalRewardDetails(this.periodMedian)
        .then(data => {
          const rewards = (data?.rewards || []);
          this.rewards = rewards.filter(r => r.tokensSent
              || r.rewards?.find(plugin => plugin.points))
            .map(r => ({
              name: r?.wallet?.name,
              points: this.formatNumber(r?.rewards?.map(x => x?.points || 0).reduce((x, y) => x + y, 0), 0),
              tokensSent: this.formatNumber(r?.tokensSent, 2),
              address: r.wallet?.address,
              transactionHash: r?.transaction?.hash,
              transaction: r?.transaction,
              wallet: r?.wallet,
            }));
        })
        .finally(() => this.loading = false);
    },
    formatNumber(number, fractions) {
      return number && new Intl.NumberFormat(eXo.env.portal.language, {
        style: 'decimal',
        minimumFractionDigits: fractions || 0,
        maximumFractionDigits: fractions || 0,
      }).format(number) || 0;
    },
  },
};
</script>