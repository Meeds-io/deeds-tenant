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
          <deed-login-introduction>
            <template #title>
              {{ $t('portal.deedTenant.login.workMetaverse') }}
            </template>
            <template #subtitle>
              {{ $t('portal.deedTenant.login.workMetaverseSubtitle') }}
            </template>
          </deed-login-introduction>>
        </nav>
      </v-card>
      <v-main
        id="mainAppArea"
        class="border-box-sizing fill-height flex-grow-1 flex-shrink-1 pa-0 mb-16 mb-sm-0">
        <v-row class="overflow-x-hidden" no-gutters>
          <v-col v-if="center" class="d-none d-sm-flex" />
          <v-col :class="center && 'align-self-top align-self-sm-center' || 'd-flex flex-column'">
            <deed-login-introduction
              height="150px"
              class="d-sm-none d-block">
              <template v-if="$slots.title" #title>
                <slot name="title"></slot>
              </template>
              <template v-else #title>
                {{ companyName }}
              </template>
            </deed-login-introduction>
            <div :class="center && 'd-block' || 'd-sm-flex flex-column flex-grow-1 px-sm-8'" class="px-4 px-sm-0">
              <v-card-title v-if="$slots.title" class="display-1 primary--text px-0 d-none d-sm-block">
                <slot name="title"></slot>
              </v-card-title>
              <div :class="center && 'd-block' || 'd-sm-flex flex mx-0'">
                <slot></slot>
              </div>
            </div>
            <deed-login-branding-image v-if="brandingImage" :params="params" />
          </v-col>
          <v-col v-if="center" class="d-none d-sm-flex" />
        </v-row>
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
    center: {
      type: Boolean,
      default: false,
    },
    brandingImage: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    companyName() {
      return this.params?.companyName;
    },
  },
  mounted() {
    this.$root.$applicationLoaded();
  },
};
</script>