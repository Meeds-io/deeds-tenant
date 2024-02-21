<template>
  <div class="d-flex flex-column align-center justify-center my-5">
    <p class="text-subtitle-1 font-weight-bold">
      {{ $t('wom.bridgingInProgress') }}
    </p>
    <p class="text-subtitle-2 ma-0">
      {{ $t('wom.bridgingInProgressDescription1') }}
    </p>
    <p class="text-subtitle-2">
      {{ $t('wom.bridgingInProgressDescription2') }}
    </p>
    <img
      :alt="$t('wom.bridgingInProgressAlt')"
      src="/deeds-tenant/images/bridging_process.gif"
      height="45px"
      width="160px"
      class="mb-4">
    <p class="text-subtitle-2">
      {{ $t('wom.bridgingInProgressImgTitle') }}
    </p>
  </div>
</template>