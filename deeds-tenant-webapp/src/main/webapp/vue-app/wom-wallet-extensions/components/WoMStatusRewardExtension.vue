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
    id="WoMStatusReward"
    class="border-radius border-color ma-5 mt-2"
    flat>
    <div class="d-flex flex-column flex-grow-1">
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="text-header text-wrap">
            {{ $t('wom.womStatus') }}
          </v-list-item-title>
        </v-list-item-content>
        <v-list-item-action class="ma-auto">
          <v-btn
            small
            icon
            @click="$refs.drawer.open()">
            <v-icon size="18">fas fa-edit</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="text-wrap">
            <help-label
              label="wom.JoinUserEngagementMinting"
              tooltip="wom.setup.subtitle2.tooltip">
              <template #helpContent>
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
          </v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <wom-setup-drawer ref="drawer" />
    </div>
  </v-card>
</template>
<script>
export default {
  mounted() {
    this.$root.$applicationLoaded();
  },
};
</script>