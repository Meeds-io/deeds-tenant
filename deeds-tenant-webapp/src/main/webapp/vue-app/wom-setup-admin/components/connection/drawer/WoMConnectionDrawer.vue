<!--
  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2023 Meeds Association contact@meeds.io

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
  <exo-drawer
    ref="drawer"
    v-model="drawer"
    :right="!$vuetify.rtl"
    :confirm-close="confirmClose"
    :confirm-close-labels="confirmCloseLabels"
    class="WoMConnectionDrawer"
    @opened="init"
    @closed="reset">
    <template slot="title">
      <span class="pb-2"> {{ $t('wom.connectionDrawerTitle') }} </span>
    </template>
    <template v-if="!loading && drawer" #content>
      <v-stepper
        v-model="stepper"
        vertical
        class="ma-0 py-0"
        flat>
        <div class="flex-grow-1 flex-shrink-0">
          <v-stepper-step
            :complete="stepper > 1"
            step="1"
            class="ma-0">
            <span class="font-weight-bold dark-grey-color text-subtitle-1">
              {{ $t('wom.connectWallet') }}
            </span>
          </v-stepper-step>
          <v-slide-y-transition>
            <wom-setup-deed-manager-selector
              v-show="stepper === 1"
              ref="managerSelector"
              :raw-message="rawMessage"
              :address.sync="deedManagerAddress"
              :signature.sync="signedMessage"
              class="px-6" />
          </v-slide-y-transition>
        </div>
        <div class="flex-grow-1 flex-shrink-0">
          <v-stepper-step
            :complete="stepper > 2"
            step="2"
            class="ma-0">
            <span class="font-weight-bold dark-grey-color text-subtitle-1">
              {{ $t('wom.chooseDeed') }}
            </span>
          </v-stepper-step>
          <v-slide-y-transition>
            <wom-setup-deed-selector
              v-show="stepper === 2"
              :value="hubDeedId"
              :address="deedManagerAddress"
              :deed-id.sync="deedId"
              class="px-6" />
          </v-slide-y-transition>
        </div>
        <div class="flex-grow-1 flex-shrink-0">
          <v-stepper-step
            :complete="stepper > 3"
            step="3"
            class="ma-0">
            <span class="font-weight-bold dark-grey-color text-subtitle-1">
              {{ $t('wom.defineYourHub') }}
            </span>
          </v-stepper-step>
          <v-slide-y-transition>
            <v-form
              ref="hubForm"
              v-model="isValidForm"
              v-show="stepper === 3"
              class="form-horizontal pt-0 pb-4 px-6"
              flat
              @submit.prevent="connect">
              <wom-setup-rewarding-receiver
                :address.sync="earnerAddress" />

              <translation-text-field
                ref="hubNameTranslations"
                v-model="hubNameTranslations"
                :rules="rules.hubName"
                :field-value.sync="hubName"
                :placeholder="$t('wom.hubNamePlaceholder')"
                :maxlength="hubNameMaxLength"
                :supported-languages="supportedLanguages"
                drawer-title="wom.translateHubName"
                name="hubName"
                class="width-auto flex-grow-1 mt-4"
                back-icon
                no-expand-icon
                autofocus
                required>
                <template #title>
                  <div class="text-subtitle-1">
                    {{ $t('wom.hubName') }}
                  </div>
                </template>
              </translation-text-field>
              <translation-text-field
                ref="hubDescriptionTranslation"
                v-model="hubDescriptionTranslations"
                :field-value.sync="hubDescription"
                :maxlength="hubDescriptionMaxLength"
                :supported-languages="supportedLanguages"
                drawer-title="wom.translateHubDescription"
                class="width-auto flex-grow-1 mt-4"
                no-expand-icon
                back-icon
                rich-editor>
                <template #title>
                  <div class="text-subtitle-1">
                    {{ $t('wom.hubDescription') }}
                  </div>
                </template>
                <rich-editor
                  id="hubDescription"
                  ref="hubDescriptionEditor"
                  v-model="hubDescription"
                  :placeholder="$t('wom.hubDescriptionPlaceholder')"
                  :max-length="hubDescriptionMaxLength"
                  :tag-enabled="false"
                  ck-editor-type="hubDescription" />
              </translation-text-field>

              <div class="text-subtitle-1 mb-2 mt-4">
                {{ $t('wom.hubUrl') }}
              </div>
              <v-text-field
                ref="hubUrl"
                v-model="hubUrl"
                :placeholder="$t('wom.hubUrlPlaceholder')"
                :rules="rules.hubUrl"
                class="pt-0 mt-0 width-auto full-height flex-grow-1 flex-shrink-1"
                outlined
                dense
                mandatory />

              <div class="subtitle-1 mt-4">
                {{ $t('wom.hubAvatar') }}
              </div>
              <wom-setup-image-selector
                ref="hubAvatar"
                v-model="hubAvatarUrl"
                :max-upload-size="0.5"
                image-type="avatar"
                @updated="hubAvatarUploadId = $event" />

              <div class="subtitle-1 mt-4">
                {{ $t('wom.hubBanner') }}
              </div>
              <wom-setup-image-selector
                ref="hubBanner"
                v-model="hubBannerUrl"
                :max-upload-size="0.5"
                image-type="cover"
                @updated="hubBannerUploadId = $event" />

              <div class="subtitle-1 mt-4">
                {{ $t('wom.hubBrandingColor') }}
              </div>
              <wom-setup-color-picker
                v-model="hubColor" />
            </v-form>
          </v-slide-y-transition>
        </div>
      </v-stepper>
    </template>
    <template v-if="!loading && drawer" #footer>
      <div class="d-flex">
        <v-btn
          v-if="stepper > 1"
          class="btn me-2"
          @click="previous">
          <template>
            {{ $t('wom.previous') }}
          </template>
        </v-btn>
        <v-spacer />
        <v-btn
          class="btn me-2"
          @click="close">
          <template>
            {{ $t('wom.cancel') }}
          </template>
        </v-btn>
        <v-btn
          v-if="stepper < 3"
          :disabled="!step1Valid || !step2Valid"
          class="btn primary"
          @click="next">
          <template>
            {{ $t('wom.next') }}
          </template>
        </v-btn>
        <v-btn
          v-else
          :loading="connecting"
          :disabled="!step3Valid"
          class="btn primary"
          @click="connect">
          <template>
            {{ $t('wom.connect') }}
          </template>
        </v-btn>
      </div>
    </template>
  </exo-drawer>
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
    hubName: null,
    hubNameTranslations: null,
    hubDescription: null,
    hubDescriptionTranslations: null,
    hubNameMaxLength: 100,
    hubDescriptionMaxLength: 500,
    hubUrl: null,
    hubColor: null,
    hubAvatarUrl: null,
    hubBannerUrl: null,
    hubAvatarUploadId: null,
    hubBannerUploadId: null,
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
    confirmClose() {
      return !!this.deedManagerAddress;
    },
    confirmCloseLabels() {
      return {
        title: this.$t('wom.confirmCancelConnect'),
        message: this.$t('wom.confirmCancelConnectMessage'),
        ok: this.$t('wom.yes'),
        cancel: this.$t('wom.no'),
      };
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
      if (this.loading) {
        this.$refs.drawer.startLoading();
      } else {
        this.$refs.drawer.endLoading();
      }
    },
    connecting() {
      if (this.connecting) {
        this.$refs.drawer.startLoading();
      } else {
        this.$refs.drawer.endLoading();
      }
    },
    hubDescription() {
      if (this.$refs.hubDescriptionTranslation) {
        this.$refs.hubDescriptionTranslation.setValue(this.hubDescription);
      }
    },
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
      return this.$hubService.generateToken()
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
      this.$hubService.connectToWoM({
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
            return this.$hubService.saveHubAvatar({
              uploadId: this.hubAvatarUploadId,
              signedMessage: this.signedMessage,
              rawMessage: this.rawMessage,
              token: this.token,
            });
          }
        })
        .then(() => {
          if (this.hubBannerUploadId) {
            return this.$hubService.saveHubBanner({
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