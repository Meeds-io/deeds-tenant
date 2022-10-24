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
  <form
    ref="form"
    name="loginForm"
    action="/portal/login"
    method="post"
    autocomplete="off"
    class="d-flex flex-column flex ma-0">
    <v-row class="flex mx-0">
      <v-col class="px-0">
        <h4>
          {{ isTenantManager && $t('deed.register.moveInIntroduction') || $t('deed.register.registerInIntroduction') }}
        </h4>
        <v-card
          width="350px"
          max-width="100%"
          class="mx-auto mx-sm-8 mt-8"
          flat>
          <v-alert
            v-if="errorCode"
            type="error"
            class="position-static elevation-0"
            dismissible>
            {{ errorMessage }}
          </v-alert>
          <input
            name="metamaskUserRegistration"
            type="hidden"
            value="true">
          <input
            :value="rememberme"
            name="rememberme"
            type="hidden">
          <v-row class="d-block ma-0 pa-0">
            <h4>
              {{ isTenantManager && '1. ' || '' }}
              {{ $t('deed.register.startWithYourProfile') }}
            </h4>
            <h4 class="font-weight-bold mb-0 mt-8">
              {{ $t('deed.register.yourETHAddress') }}
            </h4>
            <v-text-field
              id="username"
              v-model="username"
              :placeholder="$t('portal.register.username')"
              class="register-username border-box-sizing"
              name="username"
              type="text"
              aria-required="true"
              required="required"
              tabindex="1"
              disabled
              filled
              outlined
              dense />
          </v-row>
          <v-row class="d-block ma-0 pa-0">
            <h4 class="font-weight-bold mb-0 mt-4">
              {{ $t('deed.register.yourProfile') }}
            </h4>
            <v-text-field
              id="fullName"
              v-model="fullName"
              :loading="loading"
              :placeholder="$t('portal.register.displayName')"
              :autofocus="!fullName && 'autofocus' || ''"
              class="register-full-name border-box-sizing"
              name="fullName"
              tabindex="1"
              type="text"
              aria-required="true"
              required="required"
              outlined
              dense />
          </v-row>
          <v-row class="ma-0 pa-0">
            <v-text-field
              id="email"
              v-model="email"
              :loading="loading"
              :placeholder="$t('portal.register.email')"
              :autofocus="fullName && 'autofocus' || ''"
              class="register-full-name border-box-sizing"
              name="email"
              type="email"
              pattern="[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[a-z]{2,4}$"
              tabindex="2"
              aria-required="true"
              required="required"
              outlined
              dense />
          </v-row>
        </v-card>
      </v-col>
    </v-row>
    <v-row class="flex mx-0 align-center align-sm-left align-content-end">
      <v-col class="d-flex justify-center justify-sm-end flex-column-reverse flex-sm-row align-center mx-0 px-0">
        <v-btn
          :aria-label="$t('portal.cancel')"
          :disabled="sending"
          href="/portal/login"
          tabindex="4"
          class="btn register-button"
          outlined
          elevation="0">
          <span class="text-capitalize">
            {{ $t('portal.cancel') }}
          </span>
        </v-btn>
        <div class="ma-2"></div>
        <v-btn
          :aria-label="$t('portal.register')"
          :disabled="!validForm"
          :loading="sending"
          type="submit"
          tabindex="3"
          color="primary"
          class="btn btn-primary register-button"
          elevation="0"
          @click="sending = true">
          <span class="text-capitalize">
            {{ buttonTitle }}
          </span>
        </v-btn>
      </v-col>
    </v-row>
  </form>
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
    fullName: null,
    email: null,
    validForm: false,
    provider: null,
    resolvedEnsName: null,
    loading: false,
    sending: false,
  }),
  computed: {
    isTenantManager() {
      return this.params?.isTenantManager;
    },
    rememberme() {
      return this.params?.rememberme;
    },
    username() {
      return this.params?.username;
    },
    buttonTitle() {
      return this.validForm
        && (this.isTenantManager && this.$t('deed.register.nextStep') || this.$t('portal.register'))
        || this.$t('deed.register.gettingReady');
    },
    errorCode() {
      return this.params?.errorCode;
    },
    errorMessage() {
      if (this.errorCode === 'USERNAME_MANDATORY') {
        return this.$t('portal.register.usernameMandatory');
      } else if (this.errorCode === 'REGISTRATION_NOT_ALLOWED') {
        return this.$t('portal.register.userNotAllowedToRegister');
      } else if (this.errorCode === 'USERNAME_ALREADY_EXISTS') {
        return this.$t('UILoginForm.label.usernameAlreadyExists');
      } else if (this.errorCode === 'EMAIL_ALREADY_EXISTS') {
        return this.$t('UILoginForm.label.emailAlreadyExists');
      } else if (this.errorCode === 'REGISTRATION_ERROR') {
        return this.$t('UILoginForm.label.unknownError');
      } else if (this.errorCode === 'INVALID_CREDENTIALS') {
        return this.$t('UILoginForm.label.SigninFail');
      } else if (this.errorCode === 'FULLNAME_MANDATORY') {
        return this.$t('portal.register.fullNameMandatory');
      }
      return this.errorCode;
    },
  },
  watch: {
    fullName() {
      this.checkFormValidity();
    },
    email() {
      this.checkFormValidity();
    },
  },
  created() {
    this.fullName = this.params?.fullName;
    this.email = this.params?.email || this.params?.tenantManagerEmail || '';
    this.provider = new window.ethers.providers.Web3Provider(window.ethereum);

    if (this.username && (!this.fullName || !this.email)) {
      // Resolve ENS only when it's wasn't already resolved.
      // The user may have sent an invalid form,
      // thus no new ENS resolution should be made
      this.loading = true;
      this.resolveEnsAttributes().finally(() => this.loading = false);
    }
  },
  mounted() {
    this.$root.$applicationLoaded();
  },
  methods: {
    checkFormValidity() {
      this.$nextTick(() => {
        this.validForm = this.email?.length && this.fullName?.length && this.$refs.form?.checkValidity();
      });
    },
    resolveEnsAttributes() {
      return this.provider.lookupAddress(this.username)
        .then(ensName => {
          if (ensName) {
            this.resolvedEnsName = ensName;
            return this.provider.getResolver(ensName);
          }
        })
        .then(resolver => {
          if (resolver) {
            return Promise.all([
              resolver.getText('display')
                .then(displayName => this.fullName = this.fullName || displayName || this.resolvedEnsName),
              resolver.getText('email')
                .then(email => this.email = this.email || email)
            ]);
          }
        });
    },
  },
};
</script>