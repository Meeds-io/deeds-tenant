<template>
  <div>
    <p
      class="text-subtitle-1 font-weight-bold mb-5 text-center"
      v-sanitized-html="$t('wom.connected')"></p>
    <p class="my-0">
      {{ $t('wom.connected.part1') }}
    </p>
    <p
      class="my-0"
      v-sanitized-html="$t('wom.connected.part2', {
        0: `<a href='http://www.meeds.io/hubs' target='_blank' rel='nofollow noreferrer noopener'>`,
        1: '</a>'
      })">
    </p>
    <p class="mb-5">
      {{ $t('wom.connected.part3') }}
    </p>
  </div>
</template>