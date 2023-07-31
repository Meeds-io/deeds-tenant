<template>
  <div>
    <div v-if="specificAddress" class="d-flex align-center">
      <div>
        {{ walletTitle }}
      </div>
      <wom-integration-address
        :address="specificAddress"
        clearable
        class="ms-auto"
        @clear="reset" />
    </div>
    <div v-else>
      <div class="mb-4">
        {{ $t('wom.selectEarningsReceiverAddress') }}
      </div>
      <div class="d-flex">
        <v-text-field
          v-model="specificAddress"
          class="pt-0 mt-0 width-auto full-height flex-grow-1 flex-shrink-1"
          outlined
          dense
          autofocus
          mandatory />
        <v-btn
          class="mx-auto white-background primary-border-color ms-2 d-block rounded-lg"
          height="40"
          outlined
          @click="selectAddress()">
          <v-img
            src="/deeds-tenant/images/metamask.svg"
            max-height="25px"
            max-width="25px" />
        </v-btn>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    address: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    specificAddress: null,
    adminAddress: null,
  }),
  computed: {
    walletTitle() {
      return (!this.specificAddress || this.specificAddress === this.adminAddress)
        && this.$t('wom.hubWallet')
        || this.$t('wom.receiverWallet');
    },
    isMobile() {
      return this.$vuetify.breakpoint.mobile;
    },
  },
  watch: {
    specificAddress() {
      this.$emit('update:address', this.specificAddress);
    },
  },
  created() {
    this.adminAddress = this.address;
    this.specificAddress = this.address;
  },
  methods: {
    reset() {
      this.specificAddress = null;
    },
    selectAddress() {
      return this.$metamaskUtils.chooseAccount(this.isMobile)
        .then(address => this.specificAddress = address)
        .catch(console.debug);// eslint-disable-line no-console
    },
  },
};
</script>