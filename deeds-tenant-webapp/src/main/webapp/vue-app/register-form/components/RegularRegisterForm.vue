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
    <v-main>
      <v-container fluid>
        <div class="loginBGLight">
          <span></span>
        </div>
        <div class="uiLogin">
          <div class="loginContainer">
            <div class="loginContent">
              <p class="brandingComanyClass mt-5 mb-0 center">
                {{ $t('portal.register.setupUserProfile') }}
              </p>
              <div class="titleLogin">
                <div
                  v-if="errorCode"
                  class="signinFail"
                  role="alert">
                  <i class="errorIcon uiIconError"></i>{{ errorMessage }}
                </div>
                <v-card class="transparent" flat>
                  <form
                    name="registerForm"
                    action="/portal/login"
                    method="post"
                    accept-charset="ISO-8859-1"
                    enctype="application/x-www-form-urlencoded; charset=ISO-8859-1"
                    class="ma-0"
                    autocomplete="off">
                    <div class="userCredentials">
                      <input
                        name="metamaskUserRegistration"
                        type="hidden"
                        value="true">
                      <input
                        :value="rememberme"
                        name="rememberme"
                        type="hidden">
                      <label for="username" class="text-capitalize white--text mb-2 font-weight-bold">
                        {{ $t('portal.register.username') }}
                      </label>
                      <input
                        id="username"
                        :placeholder="$t('portal.register.username')"
                        :value="username"
                        name="username"
                        type="text"
                        class="ps-4 pe-8 grey lighten-2"
                        disabled>
                      <label for="username" class="text-capitalize white--text mb-2 font-weight-bold">
                        {{ $t('portal.register.displayName') }} *
                        <v-progress-circular
                          v-if="loading"
                          color="secondary"
                          indeterminate
                          size="16" />
                      </label>
                      <input
                        id="fullName"
                        :placeholder="$t('portal.register.displayNamePlaceholder')"
                        :value="fullName"
                        name="fullName"
                        tabindex="1"
                        type="text"
                        aria-required="true"
                        class="ps-4 pe-8"
                        required="required"
                        autofocus="autofocus">
                      <label for="username" class="text-capitalize white--text mb-2 font-weight-bold">
                        {{ $t('portal.register.email') }}
                        <v-progress-circular
                          v-if="loading"
                          color="secondary"
                          indeterminate
                          size="16" />
                      </label>
                      <input
                        id="email"
                        :placeholder="$t('portal.register.emailPlaceholder')"
                        :value="email"
                        name="email"
                        tabindex="2"
                        type="email"
                        class="ps-4 pe-8">
                    </div>
                    <v-card-actions class="d-flex justify-space-around">
                      <v-btn
                        :aria-label="$t('portal.register')"
                        type="submit"
                        tabindex="3"
                        class="col-4 secondary"
                        elevation="0">
                        <span class="text-capitalize">
                          {{ $t('portal.register') }}
                        </span>
                      </v-btn>
                      <v-btn
                        :aria-label="$t('portal.cancel')"
                        href="/portal/login"
                        tabindex="4"
                        class="col-4 white"
                        link
                        elevation="0">
                        <span class="text-capitalize">
                          {{ $t('portal.cancel') }}
                        </span>
                      </v-btn>
                    </v-card-actions>
                  </form>
                </v-card>
              </div>
            </div>
          </div>
        </div>
        <div class="brandingImageContent">
          <img
            :src="brandingLogo"
            class="brandingImage"
            role="presentation"
            alt="Branding Company Logo">
        </div>
      </v-container>
    </v-main>
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
    rememberme: true,
    fullName: null,
    email: null,
    provider: null,
    resolvedEnsName: null,
    loading: false,
  }),
  computed: {
    username() {
      return this.params?.username;
    },
    brandingLogo() {
      return this.params?.brandingLogo;
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
  created() {
    this.rememberme = this.params?.rememberme;
    this.fullName = this.params?.fullName;
    this.email = this.params?.email;
    this.provider = new window.ethers.providers.Web3Provider(window.ethereum);

    if (this.username && !this.fullName && !this.email) {
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
                .then(displayName => this.fullName = displayName || this.resolvedEnsName),
              resolver.getText('email')
                .then(email => this.email = email)
            ]);
          }
        });
    },
  },
};
</script>