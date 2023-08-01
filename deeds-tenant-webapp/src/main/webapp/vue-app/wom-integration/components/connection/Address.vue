<template>
  <v-chip outlined>
    <wom-integration-address-icon
      :address="address"
      class="me-2 ms-n2" />
    <span :title="address" class="me-2 dark-grey-color">
      {{ addressPreview }}
    </span>
    <v-btn
      v-if="clearable"
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
    clearable: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    addressPreview() {
      return this.address && `${this.address.substring(0,5)}...${this.address.substring(this.address.length-4 ,this.address.length)}`;
    },
  },
};
</script>