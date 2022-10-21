<!--

 This file is part of the Meeds project (https://meeds.io/).
 
 Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 
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
  <v-app>
    <div class="d-flex flex-row full-height">
      <v-card
        tile
        flat
        min-width="33%"
        height="100%"
        class="primary fill-height width-min-content flex-shrink-1 d-none d-sm-flex">
        <nav class="fill-height flex-grow-1">
          <portal-deed-login-introduction>
            <template #title>
              {{ $t('portal.deedTenant.login.workMetaverse') }}
            </template>
            <template #subtitle>
              {{ $t('portal.deedTenant.login.workMetaverseSubtitle') }}
            </template>
          </portal-deed-login-introduction>>
        </nav>
      </v-card>
      <v-main
        id="mainAppArea"
        class="border-box-sizing overflow-y-auto fill-height flex-grow-1 flex-shrink-1 pa-0 mb-16 mb-sm-0">
        <portal-deed-login-introduction
          height="150px"
          class="d-sm-none d-block">
          <template #title>
            {{ $t('portal.deedTenant.login.web3Workspace') }}
          </template>
        </portal-deed-login-introduction>
        <portal-deed-login-main
          :params="params"
          class="mx-auto my-4 my-sm-auto px-4 px-sm-0" />
        <portal-deed-login-branding-image :params="params" />
      </v-main>
    </div>
  </v-app>
</template>
<script>
export default {
  props: {
    params: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    cities: ['Tanit', 'Reshef', 'Ashtarte', 'Melqart', 'Eshmun', 'Kushor', 'Hammon'],
    cardTypes: ['Common', 'Uncommon', 'Rare', 'Legendary'],
  }),
  computed: {
    nftId() {
      return this.params?.nftId;
    },
    cityIndex() {
      return this.params?.cityIndex;
    },
    cardTypeIndex() {
      return this.params?.cardTypeIndex;
    },
    cityName() {
      return this.cities[this.cityIndex];
    },
    cardTypeName() {
      return this.cardTypes[this.cardTypeIndex];
    },
    summary() {
      return `${this.cardTypeName} #${this.nftId}`;
    },
    brandingLogo() {
      return this.params && this.params.brandingLogo;
    },
    mobile() {
      return this.$vuetify.breakpoint.xs;
    },
  },
  created() {
    document.title = this.$t('UILoginForm.label.login');
  },
  mounted() {
    this.$root.$applicationLoaded();
  },
};
</script>