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
  <v-hover>
    <v-img
      slot-scope="{ hover }"
      :src="logoPreviewSrc" 
      role="presentation"
      transition="none"
      class="mx-auto"
      width="400"
      min-height="150"
      max-height="150"
      contain
      eager>
      <v-expand-transition>
        <div
          v-show="hover"
          class="justify-center light-grey-background absolute-full-size">

          <v-btn
            v-if="hover && !isDefaultSrc"
            color="error"
            class="absolute-all-center white error"
            icon
            border
            outlined
            @click="resetLogo">
            <v-icon>fas fa-trash</v-icon>
          </v-btn>
          <v-file-input
            v-if="!resetInput"
            v-show="isDefaultSrc"
            id="logoFileInput"
            ref="logoFileInput"
            :loading="sendingImage"
            prepend-icon="fas fa-camera rounded-circle primary-border-color primary--text white py-1 absolute-all-center"
            accept="image/*"
            class="branding-logo-selector"
            rounded
            clearable
            @change="uploadAvatar" />
        </div>
      </v-expand-transition>
    </v-img>
  </v-hover>
</template>

<script>
export default {
  props: {
    value: {
      type: String,
      default: () => null,
    },
    branding: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    sendingImage: false,
    logoSrc: null,
    uploadInProgress: false,
    uploadProgress: 0,
    maxFileSize: 2097152,
    resetInput: false,
  }),
  computed: {
    isDefaultSrc() {
      return !this.logoSrc;
    },
    defaultLogoSrc() {
      if (!this.branding?.logo?.data) {
        return `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/platform/branding/logo`;
      }

      if (Array.isArray(this.branding.logo.data)) {
        return this.convertImageDataAsSrc(this.branding.logo.data);
      } else {
        return this.branding.logo.data;
      }
    },
    logoPreviewSrc() {
      if (!this.logoSrc) {
        return this.defaultLogoSrc;
      }

      if (Array.isArray(this.logoSrc)) {
        return this.convertImageDataAsSrc(this.logoSrc);
      } else {
        return this.logoSrc;
      }
    },
  },
  watch: {
    logoPreviewSrc() {
      this.$emit('logo-src', this.logoPreviewSrc);
    },
    sendingImage() {
      if (this.sendingImage) {
        document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
      } else {
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
      }
    },
  },
  methods: {
    resetLogo() {
      this.logoSrc = null;
      this.$emit('input', null);
      this.sendingImage = false;
      if (this.$refs.logoFileInput) {
        this.resetInput = true;
        this.$nextTick().then(() => this.resetInput = false);
      }
    },
    uploadAvatar(file) {
      if (file && file.size) {
        if (file.type && file.type.indexOf('image/') !== 0) {
          this.$emit('error', this.$t('mustpng.label'));
          return;
        }
        if (file.size > this.maxFileSize) {
          this.$emit('error', this.$t('toobigfile.label'));
          return;
        }
        this.sendingImage = true;
        const self = this;
        return this.$uploadService.upload(file)
          .then(uploadId => {
            if (uploadId) {
              const reader = new FileReader();
              reader.onload = (e) => {
                self.logoSrc = e.target.result;
                self.$forceUpdate();
              };
              reader.readAsDataURL(file);
              this.$emit('input', uploadId);
            } else {
              this.$emit('error', this.$t('deed.setup.uploadingError'));
            }
          })
          .catch(error => this.$emit('error', error))
          .finally(() => this.sendingImage = false);
      }
    },
    convertImageDataAsSrc(imageData) {
      let binary = '';
      const bytes = new Uint8Array(imageData);
      bytes.forEach(byte => binary += String.fromCharCode(byte));
      return `data:image/png;base64,${btoa(binary)}`;
    },
  },
};
</script>