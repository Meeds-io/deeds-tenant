<template>
  <v-list-item class="border-color rounded-lg" two-line>
    <v-list-item-avatar
      class="deed-avatar"
      height="70"
      min-width="70"
      width="70">
      <v-img :src="cardImage" />
    </v-list-item-avatar>
    <v-list-item-content>
      <v-list-item-title class="text-capitalize">{{ cardType }} #{{ deedId }}</v-list-item-title>
      <v-list-item-subtitle class="text-capitalize">{{ city }}</v-list-item-subtitle>
    </v-list-item-content>
    <v-list-item-action>
      <v-btn
        icon
        @click="$emit('clear')">
        <v-icon size="18" class="error--text">fa-times</v-icon>
      </v-btn>
    </v-list-item-action>
  </v-list-item>
</template>
<script>
export default {
  props: {
    deed: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    cities: ['TANIT', 'RESHEF', 'ASHTARTE', 'MELQART', 'ESHMUN', 'KUSHOR', 'HAMMON'],
    cardTypes: ['COMMON', 'UNCOMMON', 'RARE', 'LEGENDARY'],
  }),
  computed: {
    deedId() {
      return this.deed?.nftId;
    },
    cityIndex() {
      return this.deed?.city;
    },
    cardTypeIndex() {
      return this.deed?.type;
    },
    city() {
      return this.cities[this.cityIndex];
    },
    cardType() {
      return this.cardTypes[this.cardTypeIndex];
    },
    cardImage() {
      return this.city && this.cardType && `https://wom.meeds.io/static/images/nft/${this.city.toLowerCase()}-${this.cardType.toLowerCase()}.png`;
    },
  },
};
</script>