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
  <v-flex
    ref="form"
    name="loginForm"
    action="/portal/login"
    method="post"
    autocomplete="off"
    class="d-flex flex-column flex ma-0">
    <v-row class="flex mx-0">
      <v-col class="px-0">
        <v-card
          max-width="100%"
          flat>
          <h4>
            {{ $t('deed.register.tenantSetupIntroduction') }}
          </h4>
          <deed-tenant-setup-branding
            ref="brandingForm"
            @validity-check="validForm = $event" />
        </v-card>
      </v-col>
    </v-row>
    <v-row class="flex mx-0 flex-column-reverse flex-sm-row align-center align-sm-left align-content-end">
      <v-col class="d-flex justify-center justify-sm-start align-center mx-0 px-0 col-auto">
        <v-btn
          :aria-label="$t('portal.skip')"
          :disabled="sending || resetting"
          class="pa-0"
          href="/portal/"
          text
          link
          elevation="0"
          small>
          <span class="text-capitalize text-decoration-underline">
            {{ $t('portal.skip') }}
          </span>
        </v-btn>
        <div class="ma-2"></div>
      </v-col>
      <v-col class="d-flex justify-center justify-sm-end flex-column-reverse flex-sm-row align-center mx-0 px-0">
        <v-btn
          :aria-label="$t('portal.reset')"
          :loading="resetting"
          :disabled="sending"
          class="btn register-button"
          outlined
          elevation="0"
          @click="resetSettings">
          <span class="text-capitalize">
            {{ $t('portal.reset') }}
          </span>
        </v-btn>
        <div class="ma-2"></div>
        <v-btn
          :aria-label="$t('deed.setup')"
          :disabled="!validForm"
          :loading="sending"
          color="primary"
          class="btn btn-primary register-button"
          elevation="0"
          @click="saveSettings">
          <span class="text-capitalize">
            {{ $t('deed.setup') }}
          </span>
        </v-btn>
      </v-col>
    </v-row>
  </v-flex>
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
    validForm: false,
    resetting: false,
    sending: false,
  }),
  methods: {
    saveSettings() {
      this.sending = true;
      this.$refs.brandingForm.save()
        .then(() => window.location.href = eXo.env.portal.context)
        .catch(() => this.sending = false);
    },
    resetSettings() {
      this.resetting = true;
      this.$refs.brandingForm.reset()
        .finally(() => this.resetting = false);
    },
  },
};
</script>