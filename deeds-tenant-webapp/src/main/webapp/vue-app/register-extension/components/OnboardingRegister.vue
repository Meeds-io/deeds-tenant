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
  <div v-if="onboardingRegisterEnabled">
    <form
      ref="onboardingForm"
      action="/portal/register"
      method="post"
      class="center">
      <input
        name="onboardingRegister"
        type="hidden"
        :value="true">
      <input
        name="onboardingRegisterToken"
        type="hidden"
        :value="onboardingRegisterToken">
      <v-card flat>
        <v-card-title class="justify-center pt-0 primary--text">{{ $t('deed.onboarding.emailTitle') }}</v-card-title>
      </v-card>
      <v-text-field
        id="email"
        v-model="email"
        :loading="loading"
        :placeholder="$t('deed.onboarding.emailPlaceholder')"
        :autofocus="fullName && 'autofocus' || ''"
        class="register-email border-box-sizing pt-0"
        name="email"
        type="email"
        pattern="[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[a-z]{2,4}$"
        tabindex="2"
        aria-required="true"
        required="required"
        outlined
        dense />
      <v-btn
        :loading="sending"
        :aria-label="$t('portal.register')"
        type="submit"
        tabindex="3"
        class="col-4 primary my-8 register-button"
        elevation="0"
        @click="sending = $onboardingForm.reportValidity()">
        <span class="text-capitalize">
          {{ $t('portal.confirm') }}
        </span>
      </v-btn>
      <v-alert
        v-if="onBoardingEmailSent"
        type="success"
        class="position-static my-2 white"
        dense
        outlined
        rounded>
        {{ $t('deed.onboarding.registerSuccess') }}
      </v-alert>
      <v-alert
        v-if="errorMessage"
        type="error"
        class="position-static my-2 white"
        dense
        outlined
        rounded>
        {{ errorMessage }}
      </v-alert>
    </form>
  </div>
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
    email: null,
    sending: false,
  }),
  computed: {
    onboardingRegisterToken() {
      return this.params && this.params.onboardingRegisterToken;
    },
    onboardingRegisterEnabled() {
      return this.params && this.params.onboardingRegisterEnabled;
    },
    onBoardingEmailSent() {
      return this.params && this.params.onBoardingEmailSent;
    },
    errorCode() {
      return this.params && this.params.errorCode;
    },
    errorMessage() {
      if (this.errorCode === 'REGISTRATION_ERROR') {
        return this.$t('UILoginForm.label.unknownError');
      } else if (this.errorCode === 'EMAIL_ALREADY_EXISTS') {
        return this.$t('UILoginForm.label.emailAlreadyExists');
      } else if (this.errorCode === 'USER_ALREADY_EXISTS') {
        return this.$t('UILoginForm.label.usernameAlreadyExists');
      } else if (this.errorCode === 'EMAIL_MANDATORY') {
        return this.$t('UILoginForm.label.emailMandatory');
      }
      return this.errorCode;
    },
  },
};
</script>