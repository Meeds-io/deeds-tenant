<template>
  <div class="d-flex flex-column align-center my-5">
    <p class="text-subtitle-1 font-weight-bold">
      {{ $t('wom.connectionInProgress') }}
    </p>
    <p class="text-subtitle-2 mb-4">
      {{ $t('wom.connectionInProgressDescription') }}
    </p>
    <img
      :alt="$t('wom.connectionInProgressAlt')"
      src="/deeds-tenant/images/connection_process.gif"
      height="45px"
      width="160px"
      class="mt-2">
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