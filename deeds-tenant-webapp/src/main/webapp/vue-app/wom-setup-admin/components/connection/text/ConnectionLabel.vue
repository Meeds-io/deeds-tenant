<template>
  <div class="d-flex flex-column my-5">
    <p class="text-subtitle-1 font-weight-bold mx-auto">
      {{ $t('wom.connectionReady') }}
    </p>
    <p class="text-subtitle-2 mb-4">
      {{ $t('wom.connectionReadyDescription') }}
    </p>
    <v-card
      class="px-4"
      elevation="2">
      <wom-setup-deed-item :deed="deed" />
    </v-card>
  </div>
</template>
<script>
export default {
  props: {
    deed: {
      type: Object,
      default: null,
    },
  },
};
</script>