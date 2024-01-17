<!--

 This file is part of the Meeds project (https://meeds.io/).
 
 Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 
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
    go-back-button
    @opened="init">
    <template #title>
      {{ $t('wom.colorDrawerTitle') }}
    </template>
    <template #content>
      <div class="d-flex position-relative" flat>
        <v-color-picker
          v-model="color"
          :swatches="swatches"
          dot-size="25"
          width="340"
          mode="hexa"
          show-swatches
          class="mx-auto mt-8" />
      </div>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-spacer />
        <v-btn
          class="btn mx-1"
          @click="close">
          {{ $t('wom.cancel') }}
        </v-btn>
        <v-btn
          class="btn btn-primary"
          @click="apply">
          {{ $t('wom.apply') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  props: {
    value: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    drawer: false,
    color: null,
    swatches: [
      ['#FF0000', '#319ab3', '#f97575'],
      ['#98cc81', '#4273c8', '#cea6ac'],
      ['#bc99e7', '#9ee4f5', '#774ea9'],
      ['#ffa500', '#bed67e', '#0E100F'],
      ['#ffaacc', '#0000AA', '#000055'],
    ],
  }),
  computed: {
    hexaColor() {
      return this.color?.hexa || this.color;
    },
  },
  methods: {
    init() {
      this.color = this.value;
      if (!this.value) {
        this.color = '#707070';
      }
    },
    open() {
      this.$refs.drawer.open();
    },
    close() {
      this.$refs.drawer.close();
    },
    apply() {
      this.$emit('input', this.hexaColor);
      this.close();
    }
  }
};
</script>
