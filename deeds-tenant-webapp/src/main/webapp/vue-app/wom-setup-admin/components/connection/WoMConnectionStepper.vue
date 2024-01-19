<!--
  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <div class="flex-grow-1 flex-shrink-0">
    <wom-setup-deed-manager-selector
      v-show="stepper > 0 && stepper < 3"
      ref="managerSelector"
      :raw-message="rawMessage"
      :address.sync="deedManagerAddress"
      :signature.sync="signedMessage"
      class="mx-auto" />
    <wom-setup-deed-selector
      v-if="stepper === 2"
      :value="hubDeedId"
      :address="deedManagerAddress"
      :deed-id.sync="deedId"
      class="mt-5" />
  </div>
</template>
<script>
export default {
  props: {
    hub: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    drawer: false,
    loading: false,
    connecting: false,
    stepper: 1,
    deedManagerAddress: null,
    earnerAddress: null,
    hubUrl: null,
    token: null,
    signedMessage: null,
    deedId: null,
    validateEmpty: false,
    supportedLanguages: {
      'en': 'English',
      'fr': 'French / Français',
    },
  }),
  computed: {
    step1Valid() {
      return this.stepper !== 1 || this.token && this.deedManagerAddress && this.signedMessage;
    },
    step2Valid() {
      return this.stepper !== 2 || this.deedId;
    },
    step3Valid() {
      return this.stepper !== 3 || this.earnerAddress;
    },
    modified() {
      return !!this.deedManagerAddress;
    },
    rawMessage() {
      return this.token && this.$t('wom.signConnectMessage', {
        0: this.token,
      }).replace(/\\n/g, '\n') || null;
    },
    hubDeedId() {
      return this.hub?.deedId;
    },
    rules() {
      return {
        hubName: [
          () => !!this.hubName || !this.validateEmpty || this.$t('wom.emptyHubName'),
          () => !this.hubName || this.hubName?.length <= this.hubNameMaxLength || this.$t('wom.tooLongHubName')
        ],
        hubDescription: [
          () => !!this.hubDescription || !this.validateEmpty || this.$t('wom.emptyHubDesription'),
          () => !this.hubDescription || this.hubDescription?.length <= this.hubDescriptionMaxLength || this.$t('wom.tooLongHubDescription')
        ],
        hubUrl: [
          () => !!this.hubUrl || !this.validateEmpty || this.$t('wom.emptyHubUrl'),
          () => !this.hubUrl || this.hubUrl?.length <= 100 || this.$t('wom.tooLongHubUrl'),
          () => !this.hubUrl || !this.validateEmpty || this.isValidUrl(this.hubUrl) || this.$t('wom.invalidHubUrl')
        ],
      };
    },
  },
  watch: {
    loading() {
      this.$emit('loading', this.loading);
    },
    connecting() {
      this.$emit('connecting', this.connecting);
    },
    modified() {
      this.$emit('modified', this.modified);
    },
    deedManagerAddress(newVal, oldVal) {
      if (newVal && !oldVal) {
        this.stepper = 2;
      } else if (!newVal && oldVal) {
        this.stepper = 1;
      }
    },
    hubDescription() {
      if (this.$refs.hubDescriptionTranslation) {
        this.$refs.hubDescriptionTranslation.setValue(this.hubDescription);
      }
    },
  },
  created() {
    this.init();
  },
  methods: {
    open() {
      this.reset();
      this.$translationService.getTranslationConfiguration()
        .then(configuration => {
          if (configuration?.supportedLanguages) {
            const supportedLanguages = configuration?.supportedLanguages || this.supportedLanguages;
            Object.keys(this.supportedLanguages).forEach(lang => this.supportedLanguages[lang] = supportedLanguages[lang]);
          }
        });
      this.$refs.drawer.open();
    },
    init() {
      this.stepper = 1;

      this.loading = true;
      return this.$womService.generateToken()
        .then(token => this.token = token)
        .finally(() => this.loading = false);
    },
    reset() {
      this.deedId = null;
      this.earnerAddress = this.hub?.earnerAddress || this.$root?.configuration?.adminWallet || null;
      this.hubNameTranslations = this.hub?.name || null;
      this.hubName = this.hubNameTranslations?.en || null;
      this.hubDescriptionTranslations = this.hub?.description || null;
      this.hubDescription = this.hubDescriptionTranslations?.en || null;
      this.hubUrl = this.hub?.url || window.location.origin;
      this.hubColor = this.hub?.color || '#707070';
      this.hubAvatarUploadId = null;
      this.hubBannerUploadId = null;

      const womServerUrl = this.$root.configuration?.womServerUrl;
      const hubUpdateTime = this.hub?.updatedDate && new Date(this.hub?.updatedDate).getTime();
      const hubAddress = this.hub?.address;

      this.hubAvatarUrl = hubAddress && `${womServerUrl}/api/hubs/${hubAddress}/avatar?v=${hubUpdateTime || 0}` || null;
      this.hubBannerUrl = hubAddress && `${womServerUrl}/api/hubs/${hubAddress}/banner?v=${hubUpdateTime || 0}` || null;

      this.validateEmpty = false;
      this.deedManagerAddress = null;
      this.signedMessage = null;
      this.token = null;

      this.$refs?.managerSelector?.reset();
    },
    close() {
      this.reset();
      this.$nextTick(() => {
        window.setTimeout(() => this.$refs.drawer.close(), 50);
      });
    },
    next() {
      if (this.stepper === 1 && this.step1Valid) {
        this.stepper = 2;
      } else if (this.stepper === 2 && this.step2Valid) {
        this.stepper = 3;
      }
    },
    previous() {
      if (this.stepper > 1) {
        this.stepper--;
      }
    },
    connect() {
      this.validateEmpty = true;
      if (!this.$refs.hubForm.validate()) {
        return;
      }

      this.connecting = true;
      this.$womService.connectToWoM({
        deedId: this.deedId,
        deedManagerAddress: this.deedManagerAddress,
        name: this.hubNameTranslations,
        description: this.hubDescriptionTranslations,
        url: this.hubUrl,
        color: this.hubColor,
        earnerAddress: this.earnerAddress,
        usersCount: this.$root?.configuration?.usersCount,
        rewardsPeriodType: this.$root?.configuration?.rewardsPeriodType,
        rewardsPerPeriod: this.$root?.configuration?.rewardsPerPeriod,
        signedMessage: this.signedMessage,
        rawMessage: this.rawMessage,
        token: this.token,
      })
        .then(() => {
          if (this.hubAvatarUploadId) {
            return this.$womService.saveHubAvatar({
              uploadId: this.hubAvatarUploadId,
              signedMessage: this.signedMessage,
              rawMessage: this.rawMessage,
              token: this.token,
            });
          }
        })
        .then(() => {
          if (this.hubBannerUploadId) {
            return this.$womService.saveHubBanner({
              uploadId: this.hubBannerUploadId,
              signedMessage: this.signedMessage,
              rawMessage: this.rawMessage,
              token: this.token,
            });
          }
        })
        .then(() => {
          this.connecting = false;
          this.close();
          if (this.hub) {
            this.$root.$emit('alert-message', this.$t('wom.hubDetailsModifiedSuccessfully'), 'success');
          } else {
            this.$root.$emit('alert-message-html-confeti', this.$t('wom.connectedToWoMSuccessfully'), 'success');
          }
          this.$root.$emit('wom-connection-success');
        })
        .catch(e => {
          this.connecting = false;
          const error = (e?.cause || String(e));
          const errorMessageKey = error.includes('wom.') && `wom.${error.split('wom.')[1]}` || error;
          this.$root.$emit('alert-message', this.$t(errorMessageKey), 'error');
        });
    },
    isValidUrl(url) {
      try {
        return !!new URL(url).origin.length;
      } catch (e) {
        return false;
      }
    },
    hubDescriptionCounterValue(value) {
      return `${value && value.length || 0} / ${this.hubDescriptionMaxLength}`;
    },
  },
};
</script>