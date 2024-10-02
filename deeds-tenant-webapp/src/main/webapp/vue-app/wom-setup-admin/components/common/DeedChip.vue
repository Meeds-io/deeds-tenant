<template>
  <v-chip
    :disabled="disabled"
    target="_blank"
    class="overflow-hidden d-block"
    outlined>
    <v-img
      :src="cardImage"
      max-height="36"
      max-width="36"
      class="rounded-circle ms-n3 me-2" />
    <div class="text-light-color font-weight-normal body-1">
      #{{ deedId }}
    </div>
  </v-chip>
</template>
<script>
export default {
  props: {
    deed: {
      type: Object,
      default: null,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    deedId() {
      return this.deed?.nftId;
    },
    city() {
      return this.deed?.city;
    },
    cardType() {
      return this.deed?.cardType;
    },
    cardImage() {
      return this.city && this.cardType && `https://wom.meeds.io/static/images/nft/${this.city?.toLowerCase()}-${this.cardType?.toLowerCase()}.png`;
    },
  },
};
</script>