<template>
  <v-list-item
    :key="deed.nftId"
    class="px-0"
    three-line
    @click="$emit('select')">
    <v-list-item-action v-if="selectable" class="me-4">
      <v-radio
        :key="deed.nftId"
        :value="deed.nftId"
        on-icon="fa-lg far fa-dot-circle"
        off-icon="fa-lg far fa-circle" />
    </v-list-item-action>
    <v-list-item-content>
      <v-list-item-title v-if="deed.remainingMonths === 1" class="subtitle-2 text-color">
        {{ $t('wom.chooseDeed.leaseEndsInAMonth') }}
      </v-list-item-title>
      <v-list-item-title v-else-if="deed.remainingMonths > 0" class="subtitle-2 text-color">
        {{ $t('wom.chooseDeed.leaseEndsInMonths', {0: deed.remainingMonths}) }}
      </v-list-item-title>
      <v-list-item-title v-else-if="deed.remainingDays === 1" class="subtitle-2 text-color">
        {{ $t('wom.chooseDeed.leaseEndsInADay') }}
      </v-list-item-title>
      <v-list-item-title v-else-if="deed.remainingDays > 0" class="subtitle-2 text-color">
        {{ $t('wom.chooseDeed.leaseEndsInDays', {0: deed.remainingDays}) }}
      </v-list-item-title>
      <v-list-item-title v-else class="subtitle-2 text-color">
        {{ $t('wom.chooseDeed.nolimitation') }}
      </v-list-item-title>
      <v-list-item-subtitle>
        {{ $t('wom.chooseDeed.maxUsers', {0: $tenantUtils.formatNumber(deed.maxUsers)}) }}
      </v-list-item-subtitle>
      <v-list-item-subtitle>
        {{ $t('wom.chooseDeed.mintingPower', {0: $tenantUtils.formatNumber(deed.mintingPower, 0, 1)}) }}
      </v-list-item-subtitle>
    </v-list-item-content>
    <v-list-item-action class="mx-0">
      <wom-setup-deed-chip :deed="deed" />
    </v-list-item-action>
    <v-list-item-action v-if="clearable">
      <v-btn
        icon
        outlined
        small
        class="me-n2"
        @click="$emit('clear')">
        <v-icon size="24" class="error--text">fa-times</v-icon>
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
    selectable: {
      type: Boolean,
      default: false,
    },
    clearable: {
      type: Boolean,
      default: false,
    },
  },
};
</script>