<template>
  <v-card flat>
    <v-list-item two-line>
      <v-list-item-content>
        <v-list-item-title class="text-subtitle-1 font-weight-bold">{{ $t('wom.UEMRewards') }}</v-list-item-title>
        <v-list-item-subtitle>{{ $t('wom.UEMRewardsSummary') }}</v-list-item-subtitle>
      </v-list-item-content>
    </v-list-item>
    <v-data-iterator
      :items="rewards"
      hide-default-footer>
      <template #no-data>
        <div class="align-center justify-center text-subtitle dark-grey-color">
          <v-icon
            size="36"
            color="secondary"
            class="me-4">
            fa-hand-holding-usd
          </v-icon>
          {{ $t('womn.noUEMRewards') }}
        </div>
      </template>
      <template #default="props">
        <v-row>
          <v-col
            v-for="item in props.items"
            :key="item.id"
            cols="12"
            sm="12"
            md="6"
            lg="4">
            <wom-integration-uem-reward-item :reward="item" />
          </v-col>
        </v-row>
      </template>
    </v-data-iterator>
  </v-card>
</template>
<script>
export default {
  data: () => ({
    rewards: [],
  }),
};
</script>