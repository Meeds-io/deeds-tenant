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
  <v-card
    :disabled="disabled"
    class="full-width"
    flat>
    <v-card
      class="rounded-lg"
      height="270px"
      max-height="270px"
      hover
      outlined>
      <v-card-actions class="position-absolute z-index-two r-0">
        <v-btn
          :title="$t('wom.editHubTooltip')"
          elevation="0"
          icon
          class="px-0 ms-0 me-1 elevation-1 transparent"
          @click.prevent.stop="$emit('edit')">
          <v-icon
            size="22"
            color="white"
            class="ml-1">
            fa-edit
          </v-icon>
        </v-btn>
        <v-chip
          class="overflow-hidden d-block me-2"
          color="white"
          outlined
          small>
          <v-img
            :src="cardImage"
            max-height="22"
            max-width="22"
            class="rounded-circle ms-n3" />
          <div class="white--text font-weight-normal body-1 ms-2">
            #{{ deedId }}
          </div>
        </v-chip>
      </v-card-actions>
      <v-card
        :color="hubBackgroundColor" 
        height="100px"
        width="100%"
        flat />
      <v-card
        height="75px"
        width="75px"
        class="ms-5 mt-n10 rounded-lg position-absolute"
        outlined>
        <v-img
          v-if="hubLogoUrl"
          :src="hubLogoUrl"
          class="no-border-radius mx-auto"
          height="100%"
          width="90%"
          contain />
      </v-card>
      <div class="d-flex flex-column pt-2 px-4 pb-4 overflow-hidden">
        <div :title="hubName" class="ms-10 ps-15 text-truncate">
          <span class="text-h6 font-weight-bold text-truncate">
            {{ hubName }}
          </span>
        </div>
        <v-card
          height="50px"
          class="text-light-color mt-3 pa-0"
          flat>
          <span class="text-truncate-2">
            {{ hubDescriptionText }}
          </span>
        </v-card>
        <v-spacer />
        <div class="d-flex mt-4">
          <div v-if="!hubUsers" class="d-flex align-center justify-center me-auto">
            <v-icon size="21" class="secondary--text me-3">fas fa-bolt</v-icon>
            <span class="text-light-color"> {{ $t('wom.gettingStrated') }} </span>
          </div>
          <template v-else>
            <div class="d-flex align-center justify-center me-auto">
              <v-img 
                src="/deeds-tenant/images/teamwork_icon_red.webp"
                class="me-2"
                width="25px"
                height="25px" />
              <div class="text-light-color font-weight-normal">
                {{ hubUsers }}
              </div>
            </div>
            <div v-if="!hubRewardsAmount || !hubRewardsPeriod" class="d-flex align-center justify-center ms-auto">
              <v-icon size="21" class="secondary--text me-3">fas fa-bolt</v-icon>
              <span class="text-light-color"> {{ $t('wom.gettingStrated') }} </span>
            </div>
            <div v-else class="d-flex align-center justify-center ms-2">
              <v-img 
                src="/deeds-tenant/images/meed_circle.webp"
                class="me-2"
                width="25px"
                height="25px" />
              <div class="text-light-color d-flex font-weight-normal">
                {{ hubRewardsAmount }}
                <span class="ms-2 text-no-wrap">Ɱ / {{ hubRewardsPeriod }}</span>
              </div>
            </div>
          </template>
        </div>
      </div>
    </v-card>
  </v-card>
</template>
<script>
export default {
  props: {
    hub: {
      type: Object,
      default: null,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    language() {
      return eXo.env.portal.language;
    },
    hubName() {
      return this.language === 'fr' && this.hub?.name?.fr || this.hub?.name?.en;
    },
    hubDescription() {
      return this.language === 'fr' && this.hub?.description?.fr || this.hub?.description?.en;
    },
    hubDescriptionText() {
      return this.hubDescription && this.$utils.htmlToText(this.hubDescription);
    },
    hubBackgroundColor() {
      return this.hub?.backgroundColor || this.hub?.color;
    },
    hubAddress() {
      return this.hub?.address;
    },
    hubUpdateTime() {
      return this.hub?.updatedDate && new Date(this.hub?.updatedDate).getTime();
    },
    womServerUrl() {
      return this.$root.configuration.womServerUrl;
    },
    hubLogoUrl() {
      return this.disabled
        && `/portal/rest/v1/platform/branding/logo?v=${Date.now()}`
        || `${this.womServerUrl}/api/hubs/${this.hubAddress}/avatar?v=${this.hubUpdateTime || 0}`;
    },
    hubUsersCount() {
      return this.hub?.usersCount || 0;
    },
    hubUsers() {
      return this.hubUsersCount > 999 ? `${parseInt(this.hubUsersCount / 1000)}K` : this.hubUsersCount;
    },
    hubUrl() {
      return this.hub?.hubUrl || this.hub?.url;
    },
    hubRewardsPeriodType() {
      return this.hub?.rewardsPeriodType?.toLowerCase?.();
    },
    hubRewardsPeriod() {
      return this.hubRewardsPeriodType && this.$t(`wom.${this.hubRewardsPeriodType}`);
    },
    hubRewardsAmount() {
      return new Intl.NumberFormat(this.language, {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(this.hub?.rewardsPerPeriod || 0);
    },
    deedId() {
      return this.hub?.deedId;
    },
    cityIndex() {
      return this.hub?.city;
    },
    cardTypeIndex() {
      return this.hub?.type;
    },
    city() {
      return this.$root.cities[this.cityIndex];
    },
    cardType() {
      return this.$root.cardTypes[this.cardTypeIndex];
    },
    cardImage() {
      return this.city && this.cardType && `https://wom.meeds.io/static/images/nft/${this.city.toLowerCase()}-${this.cardType.toLowerCase()}.png`;
    },
  },
};
</script>