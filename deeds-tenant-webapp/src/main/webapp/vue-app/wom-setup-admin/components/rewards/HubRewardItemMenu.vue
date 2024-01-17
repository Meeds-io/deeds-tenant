<template>
  <div>
    <v-menu
      v-model="menu"
      :left="!$vuetify.rtl"
      :right="$vuetify.rtl"
      bottom
      offset-y
      attach>
      <template #activator="{ on, attrs }">
        <v-btn
          :loading="loading"
          icon
          small
          class="ms-2"
          v-bind="attrs"
          v-on="on">
          <v-icon size="16" class="icon-default-color">fas fa-ellipsis-v</v-icon>
        </v-btn>
      </template>
      <v-list dense class="pa-0">
        <v-list-item
          v-if="canSend"
          dense
          @click="$emit('send')">
          <v-icon size="13" class="dark-grey-color">fa-cloud-download-alt</v-icon>
          <v-list-item-title class="pl-3">{{ sendButtonTitle }}</v-list-item-title>
        </v-list-item>
        <v-list-item
          v-if="canRefresh"
          dense
          @click="$emit('refresh')">
          <v-icon size="13" class="dark-grey-color">fa-sync</v-icon>
          <v-list-item-title class="pl-3">{{ $t('wom.refresh') }}</v-list-item-title>
        </v-list-item>
        <v-list-item
          v-if="expanded"
          dense
          @click="$emit('collapse')">
          <v-icon size="13" class="dark-grey-color">fa-compress-arrows-alt</v-icon>
          <v-list-item-title class="pl-3">{{ $t('wom.close') }}</v-list-item-title>
        </v-list-item>
        <v-list-item
          v-else
          dense
          @click="$emit('expand')">
          <v-icon size="13" class="dark-grey-color">fa-expand</v-icon>
          <v-list-item-title class="pl-3">{{ $t('wom.details') }}</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>
    <v-btn
      v-if="expanded"
      :title="$t('wom.close')"
      :loading="loading"
      icon
      small
      class="ms-2"
      @click="$emit('collapse')">
      <v-icon size="16" class="icon-default-color">fa-compress-arrows-alt</v-icon>
    </v-btn>
  </div>
</template>
<script>
export default {
  props: {
    report: {
      type: Object,
      default: null,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    expanded: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    menu: false,
  }),
  computed: {
    hub() {
      return this.$root.hub;
    },
    connectedToWoM() {
      return !!this.hub;
    },
    status() {
      return this.report?.status;
    },
    isErrorSending() {
      return this.status === 'ERROR_SENDING';
    },
    sendButtonTitle() {
      return this.isErrorSending && this.$t('wom.resend') || this.$t('wom.send');
    },
    canSend() {
      return this.report?.canSend;
    },
    canRefresh() {
      return this.report?.canRefresh;
    },
  },
};
</script>