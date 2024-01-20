<template>
  <div>
    <v-progress-linear v-if="loading" indeterminate />
    <div v-else-if="leases.length || ownedDeeds.length">
      <p>
        {{ $t('wom.chooseDeed.part1') }}
      </p>
      <p>
        {{ $t('wom.chooseDeed.part2') }}
      </p>
      <v-radio-group
        v-model="deedId"
        mandatory>
        <v-list-item
          v-for="deed in ownedDeeds"
          :key="deed.nftId"
          class="px-0"
          three-line
          @click="deedId = deed.nftId">
          <v-list-item-action class="me-4">
            <v-radio
              :key="deed.nftId"
              :value="deed.nftId"
              on-icon="fa-lg far fa-dot-circle"
              off-icon="fa-lg far fa-circle" />
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title class="subtitle-2 text-color">
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
        </v-list-item>
        <v-list-item
          v-for="lease in leases"
          :key="lease.nftId"
          class="px-0"
          three-line
          @click="deedId = lease.nftId">
          <v-list-item-action class="me-4">
            <v-radio
              :key="lease.nftId"
              :value="lease.nftId"
              on-icon="fa-lg far fa-dot-circle"
              off-icon="fa-lg far fa-circle" />
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title v-if="lease.remainingMonths === 1" class="subtitle-2 text-color">
              {{ $t('wom.chooseDeed.leaseEndsInAMonth') }}
            </v-list-item-title>
            <v-list-item-title v-else-if="lease.remainingMonths > 0" class="subtitle-2 text-color">
              {{ $t('wom.chooseDeed.leaseEndsInMonths', {0: lease.remainingMonths}) }}
            </v-list-item-title>
            <v-list-item-title v-else-if="lease.remainingDays === 1" class="subtitle-2 text-color">
              {{ $t('wom.chooseDeed.leaseEndsInADay') }}
            </v-list-item-title>
            <v-list-item-title v-else-if="lease.remainingDays > 0" class="subtitle-2 text-color">
              {{ $t('wom.chooseDeed.leaseEndsInDays', {0: lease.remainingDays}) }}
            </v-list-item-title>
            <v-list-item-title v-else class="subtitle-2 text-color">
              {{ $t('wom.chooseDeed.nolimitation') }}
            </v-list-item-title>
            <v-list-item-subtitle>
              {{ $t('wom.chooseDeed.maxUsers', {0: $tenantUtils.formatNumber(lease.maxUsers)}) }}
            </v-list-item-subtitle>
            <v-list-item-subtitle>
              {{ $t('wom.chooseDeed.mintingPower', {0: $tenantUtils.formatNumber(lease.mintingPower, 0, 1)}) }}
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action class="mx-0">
            <wom-setup-deed-chip :deed="lease" />
          </v-list-item-action>
        </v-list-item>
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
    address: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    leases: [],
    ownedDeeds: [],
    deedId: null,
    loading: true,
    MONTH_IN_SECONDS: 2629800,
    DAY_IN_SECONDS: 86400,
  }),
  computed: {
    changed() {
      return this.deedId && this.lastCheckedDeedId !== this.deedId;
    },
    deed() {
      if (this.deedId == null) {
        return null;
      }
      return this.leases.find(l => l.nftId === this.deedId)
        || this.ownedDeeds.find(d => d.nftId === this.deedId);
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
    deedId() {
      this.$emit('input', this.deedId);
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
      this.loadLeases()
        .then(data => {
          this.leases = data?._embedded?.leases?.filter?.(l => l.provisioningStatus === 'STOP_CONFIRMED') || [];
          this.leases.forEach(lease => {
            if (lease.endDate) {
              lease.remainingMonths = parseInt((new Date(lease.endDate).getTime() - Date.now()) / 1000 / this.MONTH_IN_SECONDS);
              lease.remainingDays = parseInt((new Date(lease.endDate).getTime() - Date.now()) / 1000 / this.DAY_IN_SECONDS);
            }
            lease.maxUsers = this.getMaxUsers(lease.cardType);
          });
        })
        .then(this.loadOwnedDeeds)
        .then(ownedDeeds => {
          ownedDeeds = ownedDeeds?.filter?.(d => d.managerAddress?.toLowerCase?.() === this.address.toLowerCase() && d.provisioningStatus === 'STOP_CONFIRMED') || [];
          return Promise.all(ownedDeeds.map(deed =>
            this.getDeed(deed.nftId)
              .then(deedMetadata => {
                deed.name = deedMetadata.name;
                deed.city = deedMetadata.name.split('-')[0].trim().toUpperCase();
                deed.cardType = deedMetadata.name.split('-')[1].trim().toUpperCase();
                deed.mintingPower = deedMetadata.attributes.find(a => a.trait_type === 'Minting Power').value;
                deed.maxUsers = this.getMaxUsers(deed.cardType);
                return deed;
              })
          )).then(ownedDeeds => this.ownedDeeds = ownedDeeds);
        })
        .finally(() => this.loading = false);
    },
    loadLeases() {
      return this.$womService.getLeases(this.$root.configuration.womServerUrl, this.address);
    },
    loadOwnedDeeds() {
      return this.$womService.getOwnedDeeds(this.$root.configuration.womServerUrl, this.address);
    },
    getDeed(deedId) {
      return this.$womService.getDeed(this.$root.configuration.womServerUrl, deedId);
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
      this.deed = null;
      this.deedId = null;
      this.isManager = false;
    },
  },
};
</script>