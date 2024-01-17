<template>
  <div>
    <wom-setup-deed-chip
      v-if="deed"
      :deed="deed"
      @clear="reset" />
    <template v-else>
      <div class="mb-4">
        {{ $t('wom.selectDeedTitle') }}
      </div>
      <div class="d-flex mb-4">
        <v-chip class="mt-1 me-2 flex-grow-0 flex-shrink-0">
          Deed #
        </v-chip>
        <v-text-field
          v-model="deedId"
          :disabled="loading"
          :loading="loading"
          :rules="rules.deedId"
          class="pt-0 mt-0 width-auto full-height flex-grow-1 flex-shrink-1"
          outlined
          dense
          autofocus>
          <template #append>
            <v-btn
              v-show="changed"
              :loading="loading"
              class="mt-n1"
              outlined
              small
              @click="checkDeedId">
              <div class="primary--text">{{ $t('wom.verify') }}</div>
            </v-btn>
          </template>
        </v-text-field>
      </div>
    </template>
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
    deed: null,
    deedId: null,
    lastCheckedDeedId: null,
    selectedDeedId: null,
    isManager: false,
    loading: false,
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
    this.deedId = this.value;
  },
  methods: {
    checkDeedId() {
      this.loading = true;
      this.lastCheckedDeedId = this.deedId;
      this.$hubService.isTenantManager(this.address, this.deedId)
        .then(isManager => this.isManager = isManager)
        .then(() => this.isManager && this.$hubService.getHub(this.deedId))
        .then(deed => this.deed = deed || null)
        .catch(() => this.isManager = false)
        .finally(() => this.loading = false);
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