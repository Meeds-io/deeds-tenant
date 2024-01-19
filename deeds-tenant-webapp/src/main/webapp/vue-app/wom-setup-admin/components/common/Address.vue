<template>
  <v-chip
    :small="small"
    :href="url"
    :loading="loading"
    target="_blank"
    rel="nofollow noreferrer noopener"
    outlined>
    <wom-setup-address-icon
      v-if="!transaction"
      :address="address"
      class="me-2 ms-n2" />
    <span
      :title="tooltip"
      :class="url && 'primary--text' || 'dark-grey-color'"
      class="me-2">
      {{ addressPreview }}
    </span>
    <v-btn
      v-if="clearable"
      :loading="loading"
      icon
      outlined
      small
      class="me-n2"
      @click="$emit('clear')">
      <v-icon size="18" class="error--text">fa-times</v-icon>
    </v-btn>
  </v-chip>
</template>
<script>
export default {
  props: {
    address: {
      type: String,
      default: null,
    },
    title: {
      type: String,
      default: null,
    },
    networkId: {
      type: String,
      default: null,
    },
    token: {
      type: Boolean,
      default: false,
    },
    transaction: {
      type: Boolean,
      default: false,
    },
    small: {
      type: Boolean,
      default: false,
    },
    clearable: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    ens: null,
    loading: false,
  }),
  computed: {
    addressPreview() {
      if (this.ens) {
        return this.ens;
      } else if (this.address) {
        return `${this.address.substring(0,5)}...${this.address.substring(this.address.length-4 ,this.address.length)}`;
      } else {
        return null;
      }
    },
    blockchain() {
      return this.networkId && (this.$root.blockchains[this.networkId] || this.$root.blockchains[0]) || null;
    },
    blockexplorer() {
      return this.blockchain?.blockexplorer;
    },
    url() {
      return this.blockexplorer && `${this.blockexplorer}/${this.token && 'token' || (this.transaction && 'tx') || 'address'}/${this.address}`;
    },
    tooltip() {
      return this.title || this.address;
    },
  },
  watch: {
    address() {
      if (this.address) {
        this.lookupAddress();
      }
    },
  },
  created() {
    if (this.address) {
      this.lookupAddress();
    }
  },
  methods: {
    lookupAddress() {
      const provider = new window.ethers.providers.Web3Provider(window.ethereum);
      this.loading = true;
      provider.lookupAddress(this.address)
        .then(name => this.ens = name)
        .finally(() => this.loading = false);
    },
  },
};
</script>