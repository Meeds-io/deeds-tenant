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
  <v-row class="mx-auto mx-lg-8 mt-8">
    <v-col
      sm="12"
      md="7"
      lg="6"
      class="pa-0">
      <h4>
        {{ `2. ${$t('deed.register.tenantSetupStepDescription')}` }}
      </h4>
      <v-card flat>
        <v-alert
          v-if="errorMessage"
          type="error"
          class="position-static elevation-0"
          dismissible>
          {{ errorMessage }}
        </v-alert>
        <h4 class="font-weight-bold mb-0 mt-8">
          {{ $t('deed.setup.companyNameLabel') }}
        </h4>
        <v-card max-width="350px" flat>
          <v-text-field
            id="companyName"
            v-model="companyName"
            :placeholder="$t('deed.setup.companyNamePlaceholder')"
            class="setup-company-name border-box-sizing"
            name="companyName"
            type="text"
            autofocus="autofocus"
            aria-required="true"
            required="required"
            outlined
            dense />
        </v-card>
        <h4 class="font-weight-bold mb-0 mt-4">
          {{ $t('deed.setup.companyLogoLabel') }}
        </h4>
        <h6 class="text-subtitle grey--text">
          {{ $t('deed.setup.companyLogoSubtitle') }}
        </h6>
        <deed-tenant-setup-company-logo
          ref="companyLogo"
          v-model="logoUploadId"
          :branding="branding"
          @logo-src="logoSrc = $event"
          @error="errorMessage = $event" />
        <h4 class="font-weight-bold mb-0 mt-8">
          {{ $t('themeColors.label') }}
        </h4>
        <v-row class="colorsBlock">
          <v-col cols="6">
            <deed-tenant-setup-color-picker
              v-model="primaryColor"
              :label="$t('themeColors.primaryColor.label')" />
          </v-col>
          <v-col cols="6">
            <deed-tenant-setup-color-picker
              v-model="secondaryColor"
              :label="$t('themeColors.secondaryColor.label')" />
          </v-col>
        </v-row>
      </v-card>
    </v-col>
    <v-col
      md="5"
      lg="6"
      class="px-0 d-none d-sm-flex">
      <deed-tenant-setup-login-preview
        :params="params"
        :company-name="companyName"
        :company-logo="logoSrc"
        :primary-color="primaryColor"
        :secondary-color="secondaryColor" />
    </v-col>
  </v-row>
</template>
<script>
export default {
  props: {
    params: {
      type: Object,
      default: null,
    },
  },
  data(){
    return {
      branding: null,
      companyName: null,
      primaryColor: null,
      secondaryColor: null,
      errorMessage: null,
      logoSrc: null,
      logoUploadId: null,
    };
  },
  computed: {
    defaultCompanyName() {
      return this.branding?.companyName;
    },
    defaultPrimaryColor() {
      return this.branding?.themeColors?.primaryColor;
    },
    defaultSecondaryColor() {
      return this.branding?.themeColors?.secondaryColor;
    },
    isValidForm() {
      return this.companyName?.length && this.primaryColor?.length && this.secondaryColor?.length;
    },
  },
  watch: {
    logoSrc() {
      this.errorMessage = null;
    },
    isValidForm() {
      this.$emit('validity-check', this.isValidForm);
    },
  },
  created() {
    this.reset().finally(() => this.$root.$applicationLoaded());
  },
  methods: {
    save() {
      const branding = Object.assign({}, this.branding);
      Object.assign(branding, {
        companyName: this.companyName,
        themeColors: {
          primaryColor: this.primaryColor,
          secondaryColor: this.secondaryColor,
          tertiaryColor: this.secondaryColor,
        },
        logo: {
          uploadId: this.logoUploadId,
        },
      });
      this.errorMessage = null;
      return this.$brandingService.updateBrandingInformation(branding)
        .catch(e => {
          this.errorMessage = String(e);
          throw e;
        });
    },
    reset() {
      return this.$brandingService.getBrandingInformation()
        .then(data => {
          this.branding = data;
          return this.$nextTick();
        })
        .then(() => {
          this.$forceUpdate();
          this.resetDefaultValues();
        });
    },
    resetDefaultValues() {
      this.$refs.companyLogo.resetLogo();
      this.companyName = this.defaultCompanyName;
      this.primaryColor = this.defaultPrimaryColor;
      this.secondaryColor = this.defaultSecondaryColor;
    },
  }
};
</script>
