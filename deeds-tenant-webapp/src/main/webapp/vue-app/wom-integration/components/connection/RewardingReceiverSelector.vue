<template>
  <div>
    <div v-if="specificAddress" class="d-flex text-start">
      <div v-if="isSpecificEarnerWallet" class="text-subtitle-1 pt-2px">
        {{ $t('wom.receiverWallet') }}
      </div>
      <div v-else>
        <div class="text-subtitle-1 pt-2px">{{ $t('wom.hubWallet') }}</div>
        <div class="caption text-light-color">{{ $t('wom.selectEarningsReceiverAddress') }}</div>
      </div>
      <wom-integration-address
        :address="specificAddress"
        clearable
        class="flex-shrink-0 ms-auto"
        @clear="reset" />
    </div>
    <div v-else>
      <div class="d-flex">
        <v-text-field
          v-model="manualAddress"
          :rules="rules.address"
          class="pt-0 mt-0 width-auto full-height flex-grow-1 flex-shrink-1"
          outlined
          dense
          autofocus
          mandatory
          @keypress="invalidAddress = false">
          <template #append>
            <v-btn
              :title="$t('wom.validateAddress')"
              class="mt-0"
              icon
              small
              @click="setManualAddress">
              <v-icon color="success">fa-check</v-icon>
            </v-btn>
          </template>
        </v-text-field>
        <v-btn
          :title="$t('wom.chooseAddressWithMetamask')"
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
    manualAddress: null,
    specificAddress: null,
    adminAddress: null,
    invalidAddress: false,
  }),
  computed: {
    rules() {
      return {
        address: [
          () => !!this.manualAddress || !this.invalidAddress || this.$t('wom.rewardingEarnerAddressMandatory'),
          () => !this.manualAddress || !this.invalidAddress || this.$t('wom.rewardingEarnerAddressInvalid')
        ],
      };
    },
    isSpecificEarnerWallet() {
      return this.specificAddress && this.specificAddress?.toLowerCase() !== this.adminAddress?.toLowerCase();
    },
    isMobile() {
      return this.$vuetify.breakpoint.mobile;
    },
  },
  watch: {
    specificAddress() {
      this.invalidAddress = false;
      this.$emit('update:address', this.specificAddress);
    },
  },
  created() {
    this.adminAddress = this.address;
    this.specificAddress = this.address;
  },
  methods: {
    reset() {
      this.manualAddress = this.specificAddress;
      this.specificAddress = null;
      this.invalidAddress = null;
    },
    selectAddress() {
      return this.$metamaskUtils.chooseAccount(this.isMobile)
        .then(address => this.specificAddress = address)
        .catch(console.debug);// eslint-disable-line no-console
    },
    setManualAddress() {
      if (!this.manualAddress || !this.$metamaskUtils.isAddress(this.manualAddress?.toLowerCase())) {
        this.invalidAddress = true;
      } else {
        this.invalidAddress = false;
        this.specificAddress = this.manualAddress;
      }
      const manualAddress = this.manualAddress;
      this.manualAddress = null;
      this.$nextTick(() => this.manualAddress = manualAddress);
    },
  },
};
</script>