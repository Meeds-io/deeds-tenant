<template>
  <div>
    <v-progress-linear v-if="loading" indeterminate />
    <div v-else-if="deeds.length">
      <p>
        {{ $t(`wom.chooseDeed.${edit && 'edit.' || ''}part1`) }}
      </p>
      <p>
        {{ $t(`wom.chooseDeed.${edit && 'edit.' || ''}part2`) }}
      </p>
      <p v-if="edit">
        {{ $t(`wom.chooseDeed.edit.part3`) }}
      </p>
      <v-radio-group
        v-model="deedId"
        mandatory>
        <wom-setup-deed-item
          v-for="item in deeds"
          :key="item.nftId"
          :deed="item"
          selectable
          class="px-0"
          @select="deedId = item.nftId" />
      </v-radio-group>
      <div class="mt-6 text-center">
        <span>{{ $t('wom.selectOfferText') }}</span>
        <v-btn
          href="https://www.meeds.io/marketplace"
          target="_blank"
          text>
          <span class="text-none primary--text">{{ $t('wom.selectOfferLink') }}</span>
        </v-btn>
      </div>
    </div>
    <div v-else>
      <p>
        {{ $t('wom.noDeed.part1') }}
      </p>
      <p>
        {{ $t('wom.noDeed.part2') }}
      </p>
      <div class="mt-6 text-center">
        <v-btn
          href="https://www.meeds.io/marketplace"
          target="_blank"
          class="primary"
          elevation="0">
          <span class="text-none">{{ $t('wom.selectOfferButton') }}</span>
        </v-btn>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    value: {
      type: String,
      default: null,
    },
    hub: {
      type: Object,
      default: null,
    },
    address: {
      type: String,
      default: null,
    },
    edit: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    deeds: [],
    deedId: null,
    loading: true,
    MONTH_IN_SECONDS: 2629800,
    DAY_IN_SECONDS: 86400,
  }),
  computed: {
    ownedDeeds() {
      return this.deeds.filter(d => !d.endDate);
    },
    leases() {
      return this.deeds.filter(d => d.endDate);
    },
    deed() {
      if (this.deedId === null || (this.hub?.enabled && this.deedId === this.hub?.deedId)) {
        return null;
      }
      return this.deeds.find(l => l.nftId === this.deedId);
    },
    deedManagerAddress() {
      return this.deed?.managerAddress;
    },
    deedOwnerAddress() {
      return this.deed?.ownerAddress || this.deedManagerAddress;
    },
    deedMaxUsers() {
      return this.deed?.maxUsers || 0;
    },
    hubUsersCount() {
      return this.$root.configuration.usersCount || 0;
    },
    maxUsersReached() {
      return this.deedMaxUsers && this.hubUsersCount > this.deedMaxUsers;
    },
  },
  watch: {
    deed() {
      this.$emit('input', this.deed);
    },
    deedManagerAddress() {
      this.$emit('update:manager', this.deedManagerAddress);
    },
    deedOwnerAddress() {
      this.$emit('update:owner', this.deedOwnerAddress);
    },
    maxUsersReached() {
      if (this.maxUsersReached) {
        this.$root.$emit('alert-message-html', this.$t('wom.maxUsersReached', {
          0: `<span class="warning--text font-weight-bold">${this.$tenantUtils.formatNumber(this.deedMaxUsers)}</span>`,
          1: `<span class="error--text font-weight-bold">${this.$tenantUtils.formatNumber(this.hubUsersCount)}</span>`
        }), 'warning');
      } else {
        this.$root.$emit('close-alert-message');
      }
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.loading = true;
      this.$hubService.getManagedDeeds(this.$root.configuration.womServerUrl, this.address)
        .then(data => {
          if (data?.length) {
            data.forEach(deed => {
              if (deed.endDate) {
                deed.remainingMonths = parseInt((new Date(deed.endDate).getTime() - Date.now()) / 1000 / this.MONTH_IN_SECONDS);
                deed.remainingDays = parseInt((new Date(deed.endDate).getTime() - Date.now()) / 1000 / this.DAY_IN_SECONDS);
              }
              deed.maxUsers = this.getMaxUsers(deed.cardType);
            });
          }
          this.deeds = data || [];
          this.deedId = this.hub?.deedId;
        })
        .finally(() => this.loading = false);
    },
    getMaxUsers(cardType) {
      switch (cardType){
      case 'COMMON': return 100;
      case 'UNCOMMON': return 1000;
      case 'RARE': return 10000;
      case 'LEGENDARY': return this.$t('wom.unlimited');
      default: return '';
      }
    },
    reset() {
      this.deeds = [];
      this.deed = null;
      this.deedId = null;
      this.isManager = false;
    },
  },
};
</script>