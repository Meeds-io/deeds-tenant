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
                <div>
                  <form
                    name="registerForm"
                    action="/portal/login"
                    method="post"
                    class="ma-0"
                    autocomplete="off">
                    <div class="userCredentials">
                      <input
                        name="metamaskUserRegistration"
                        type="hidden"
                        value="true">
                      <label for="username" class="text-capitalize white--text mb-2">
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
                      <label for="username" class="text-capitalize white--text mb-2">
                        {{ $t('portal.register.displayName') }} *
                      </label>
                      <input
                        id="displayName"
                        :placeholder="$t('portal.register.displayNamePlaceholder')"
                        name="fullName"
                        tabindex="1"
                        type="text"
                        aria-required="true"
                        class="ps-4 pe-8"
                        required="required"
                        autofocus="autofocus">
                      <label for="username" class="text-capitalize white--text mb-2">
                        {{ $t('portal.register.email') }}
                      </label>
                      <input
                        id="email"
                        :placeholder="$t('portal.register.emailPlaceholder')"
                        name="email"
                        tabindex="2"
                        type="text"
                        class="ps-4 pe-8">
                    </div>
                    <div id="UIPortalLoginFormAction" class="loginButton">
                      <button
                        :aria-label="$t('portal.register')"
                        tabindex="3">
                        {{ $t('portal.register') }}
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="brandingImageContent">
          <img
            :src="brandingLogo"
            class="brandingImage"
            role="presentation">
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
  }),
  computed: {
    companyName() {
      return this.params && this.params.companyName;
    },
    username() {
      return this.params && this.params.username;
    },
    forgotPasswordPath() {
      return this.params && this.params.forgotPasswordPath;
    },
    brandingLogo() {
      return this.params && this.params.brandingLogo;
    },
    errorCode() {
      return this.params && this.params.errorCode;
    },
    errorMessage() {
      return this.errorCode && this.$t(`UILoginForm.label.${this.errorCode}`);
    },
  },
  created() {
    document.title = this.$t('portal.register');
  },
  mounted() {
    this.$root.$applicationLoaded();
  },
};
</script>