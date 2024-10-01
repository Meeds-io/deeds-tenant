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
  <v-app flat>
    <div class="px-6">
      <v-list-item
        class="px-0"
        two-line
        dense>
        <v-list-item-content>
          <v-list-item-title class="text-title">
            <help-label
              label="wom.setup.title"
              label-class="text-title"
              tooltip="wom.whatIsWoM">
              <template slot="helpContent">
                <v-card-text class="text-color pa-0 mb-4">
                  {{ $t('wom.whatIsWoM.paragraph1') }}
                </v-card-text>
                <v-card-text class="text-color pa-0 mb-4">
                  {{ $t('wom.whatIsWoM.paragraph2') }}
                </v-card-text>
                <v-card-text
                  v-sanitized-html="$t('wom.whatIsWoM.paragraph3', {
                    0: `<a href='https://www.meeds.io/whitepaper' target='_blank'>`,
                    1: '</a>'
                  })"
                  class="text-color pa-0 mb-4" />
              </template>
            </help-label>
          </v-list-item-title>
          <v-list-item-subtitle>
            <help-label
              label="wom.setup.subtitle1"
              label-class="text-sub-title"
              tooltip="wom.setup.subtitle1.tooltip">
              <template slot="helpContent">
                <v-card-text class="text-color pa-0 mb-4">
                  {{ $t('wom.setup.subtitle1.paragraph1') }}
                </v-card-text>
                <v-card-text
                  v-sanitized-html="$t('wom.setup.subtitle1.paragraph2', {
                    0: `<a href='https://www.meeds.io/hubs' target='_blank'>`,
                    1: '</a>'
                  })"
                  class="text-color pa-0 mb-4" />
                <v-card-text class="text-color pa-0 mb-4">
                  {{ $t('wom.setup.subtitle1.paragraph3') }}
                </v-card-text>
              </template>
            </help-label>
            <help-label
              label="wom.setup.subtitle2"
              label-class="text-sub-title"
              tooltip="wom.setup.subtitle2.tooltip">
              <template slot="helpContent">
                <v-card-text class="text-color pa-0 mb-4">
                  {{ $t('wom.setup.subtitle2.paragraph1') }}
                </v-card-text>
                <v-card-text
                  v-sanitized-html="$t('wom.setup.subtitle2.paragraph2', {
                    0: `<a href='https://www.meeds.io/whitepaper' target='_blank'>`,
                    1: '</a>'
                  })"
                  class="text-color pa-0 mb-4" />
              </template>
            </help-label>
          </v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-btn
            icon
            @click="$refs.drawer.open()">
            <v-icon size="18" class="icon-default-color">fa-edit</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
    </div>
    <wom-setup-drawer ref="drawer" />
  </v-app>
</template>
<script>
export default {
  mounted() {
    this.$root.$applicationLoaded();
    if (window.location.hash === '#wom') {
      this.$refs.drawer.open();
    }
  },
};
</script>
