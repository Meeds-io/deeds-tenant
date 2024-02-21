<template>
  <v-list-item
    :key="deed.nftId"
    :disabled="deed.connected || selected"
    :three-line="!usedByOtherHub"
    :class="{
      'disabled-background': deed.connected,
    }"
    class="px-0"
    v-on="selectable && !deed.connected && {
      click: () => $emit('select')
    }">
    <v-list-item-action
      v-if="selectable"
      class="me-4">
      <div>
        <v-radio
          v-show="!selected"
          :key="deed.nftId"
          :value="deed.nftId"
          :disabled="usedByOtherHub"
          on-icon="fa-lg far fa-dot-circle"
          off-icon="fa-lg far fa-circle" />
      </div>
      <v-icon
        v-if="selected"
        size="18"
        class="mx-1"
        color="success">
        fa-check
      </v-icon>
    </v-list-item-action>
    <v-list-item-content>
      <v-list-item-title v-if="usedByOtherHub" class="subtitle-2">
        {{ $t('wom.alreadyConnected') }}
      </v-list-item-title>
      <v-list-item-title v-else-if="deed.remainingMonths === 1" class="subtitle-2 text-color">
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
      <v-list-item-title v-else-if="deed.endDate" class="subtitle-2 text-color">
        {{ $t('wom.chooseDeed.lessThanADay') }}
      </v-list-item-title>
      <v-list-item-title v-else class="subtitle-2 text-color">
        {{ $t('wom.chooseDeed.nolimitation') }}
      </v-list-item-title>
      <v-list-item-subtitle v-if="!usedByOtherHub">
        {{ $t('wom.chooseDeed.maxUsers', {0: $tenantUtils.formatNumber(deed.maxUsers)}) }}
      </v-list-item-subtitle>
      <v-list-item-subtitle v-if="!usedByOtherHub">
        {{ $t('wom.chooseDeed.mintingPower', {0: $tenantUtils.formatNumber(deed.mintingPower, 0, 1)}) }}
      </v-list-item-subtitle>
    </v-list-item-content>
    <v-list-item-action class="mx-0">
      <wom-setup-deed-chip
        :deed="deed"
        :disabled="usedByOtherHub" />
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
    selected: {
      type: Boolean,
      default: false,
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
  computed: {
    usedByOtherHub() {
      return this.deed.connected && !this.selected;
    },
  },
};
</script>