<template>
  <div>
    <p>
      {{ $t('wom.chooseDeed.part1') }}
    </p>
    <p>
      {{ $t('wom.chooseDeed.part2') }}
    </p>
    <v-progress-liner v-if="loading" indeterminate />
    <v-radio-group
      v-else-if="leases.length || ownedDeeds.length"
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
            {{ $t('wom.chooseDeed.maxUsers', {0: deed.maxUsers}) }}
          </v-list-item-subtitle>
          <v-list-item-subtitle>
            {{ $t('wom.chooseDeed.mintingPower', {0: deed.mintingPower}) }}
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
            {{ $t('wom.chooseDeed.maxUsers', {0: lease.maxUsers}) }}
          </v-list-item-subtitle>
          <v-list-item-subtitle>
            {{ $t('wom.chooseDeed.mintingPower', {0: lease.mintingPower}) }}
          </v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action class="mx-0">
          <wom-setup-deed-chip :deed="lease" />
        </v-list-item-action>
      </v-list-item>
    </v-radio-group>
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
    loading: false,
    MONTH_IN_SECONDS: 2629800,
    DAY_IN_SECONDS: 86400,
  }),
  computed: {
    changed() {
      return this.deedId && this.lastCheckedDeedId !== this.deedId;
    },
    rules() {
      return {
        deedId: [
          () => !this.deedId?.length || this.loading || this.changed || this.isManager || this.$t('wom.notDeedManagerError')
        ],
      };
    },
  },
  watch: {
    isManager() {
      this.selectedDeedId = this.isManager && this.deedId || null;
    },
    selectedDeedId() {
      this.$emit('update:deedId', this.selectedDeedId);
    },
    loading() {
      if (!this.loading) {
        const deedId = this.deedId;
        this.deedId = null;
        this.$nextTick().then(() => this.deedId = deedId);
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
          this.leases = data?._embedded?.leases?.filter?.(l => l.provisioningStatus === 'START_CONFIRMED') || [];
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
          ownedDeeds = ownedDeeds?.filter?.(d => d.managerAddress?.toLowerCase?.() === this.address.toLowerCase() && d.provisioningStatus === 'START_CONFIRMED') || [];
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
      this.selectedDeedId = null;
      this.isManager = false;
    },
  },
};
</script>