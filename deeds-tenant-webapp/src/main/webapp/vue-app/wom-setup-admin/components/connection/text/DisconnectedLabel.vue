<template>
  <div>
    <p class="text-center text-color font-weight-bold mb-4">
      {{ $t('wom.disconnected.information') }}
    </p>
    <p
      v-sanitized-html="$t('wom.disconnected.description.part1', {0: '<strong>', 1: '</strong>'})"
      class="text-color my-0">
    </p>
    <p
      v-sanitized-html="$t('wom.disconnected.description.part2', {
        0: `<a href='http://www.meeds.io/whitepaper' target='_blank' rel='nofollow noreferrer noopener'>`,
        1: '</a>'
      })"
      class="text-color my-0">
    </p>
    <p class="text-color my-0">
      {{ $t('wom.disconnected.description.part3') }}
    </p>
  </div>
</template>