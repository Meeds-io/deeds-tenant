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
  <v-dialog
    ref="dialog"
    v-model="modal"
    color="white"
    width="290px">
    <template #activator="{ on }">
      <v-list-item two-line class="px-0" dense>
        <v-list-item-action class="me-2">
          <v-card
            :color="value"
            height="50px"
            width="50px"
            v-on="on" />
        </v-list-item-action>
        <v-list-item-content>
          <v-list-item-title>
            {{ label }} {{ $t('branding.color.label') }}
          </v-list-item-title>
          <v-spacer class="my-1" />
          <v-list-item-subtitle class="grey--text">
            {{ value }}
          </v-list-item-subtitle>
        </v-list-item-content>
      </v-list-item>
    </template>
    <v-color-picker
      v-model="value"
      :swatches="swatches"
      mode="hexa"
      show-swatches />
    <v-row class="mx-0 white">
      <v-col class="center">
        <v-btn
          text
          color="primary"
          @click="cancel">
          {{ $t('portal.cancel') }}
        </v-btn>
      </v-col>
      <v-col class="center">
        <v-btn
          text
          color="primary"
          @click="save">
          {{ $t('portal.ok') }}
        </v-btn>
      </v-col>
    </v-row>
  </v-dialog>
</template>
<script>
export default {
  props: {
    label: {
      type: String,
      default: null,
    },
    value: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    modal: false,
    originalValue: null,
    swatches: [
      ['#FF0000', '#AA0000', '#550000'],
      ['#FFFF00', '#AAAA00', '#555500'],
      ['#00FF00', '#00AA00', '#005500'],
      ['#00FFFF', '#00AAAA', '#005555'],
      ['#0000FF', '#0000AA', '#000055'],
    ],
  }),
  watch: {
    modal() {
      if (this.modal) {
        this.originalValue = this.value;
      }
    }
  },
  methods: {
    cancel() {
      this.value = this.originalValue;
      this.modal = false;
    },
    save() {
      this.$emit('input', this.value);
      this.modal = false;
    }
  }
};
</script>
