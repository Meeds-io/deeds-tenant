<template>
  <div>
    <div v-if="address" class="d-flex align-center">
      <div class="flex-grow-1 text-start">
        {{ $t('wom.welcome') }}
      </div>
      <wom-setup-address
        :address="address"
        :clearable="!disabled"
        @clear="reset" />
    </div>
    <div v-else>
      <div v-sanitized-html="signMessageTitle" class="mb-4"></div>
      <wom-setup-metamask-button
        :message="rawMessage"
        :disabled="!rawMessage"
        :address.sync="address"
        :allowed-address="edit && hub.hubOwnerAddress"
        :signature.sync="signature"
        label="wom.start"
        primary />
    </div>
  </div>
</template>
<script>
export default {
  props: {
    rawMessage: {
      type: String,
      default: null,
    },
    hub: {
      type: Object,
      default: null,
    },
    edit: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    address: null,
    signature: null,
  }),
  computed: {
    signMessageTitle() {
      return this.$t('wom.signMessageTitle', {
        0: '<a href="https://www.meeds.io/marketplace" target="_blank">',
        1: '</a>',
      });
    },
  },
  watch: {
    address() {
      this.$emit('update:address', this.address);
    },
    signature() {
      this.$emit('update:signature', this.signature);
    },
  },
  methods: {
    reset() {
      this.address = null;
      this.signature = null;
    },
  },
};
</script>