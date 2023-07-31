<!--
  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2023 Meeds Association contact@meeds.io

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
    class="WoMConnectionDrawer"
    eager
    @opened="init">
    <template slot="title">
      <span class="pb-2"> {{ $t('wom.connectionDrawerTitle') }} </span>
    </template>
    <template v-if="!loading && drawer" #content>
      <v-stepper
        v-model="stepper"
        vertical
        class="ma-0 py-0"
        flat>
        <div class="flex-grow-1 flex-shrink-0">
          <v-stepper-step
            :complete="stepper > 1"
            step="1"
            class="ma-0">
            <span class="font-weight-bold dark-grey-color text-subtitle-1">
              {{ $t('wom.connectWallet') }}
            </span>
          </v-stepper-step>
          <v-slide-y-transition>
            <wom-integration-deed-manager-selector
              v-show="stepper === 1"
              ref="managerSelector"
              :token="token"
              :address.sync="address"
              :signature.sync="signature"
              class="px-6" />
          </v-slide-y-transition>
        </div>
        <div class="flex-grow-1 flex-shrink-0">
          <v-stepper-step
            :complete="stepper > 2"
            step="2"
            class="ma-0">
            <span class="font-weight-bold dark-grey-color text-subtitle-1">
              {{ $t('wom.chooseDeed') }}
            </span>
          </v-stepper-step>
          <v-slide-y-transition>
            <wom-integration-deed-selector
              v-show="stepper === 2"
              :address="address"
              :deed-id.sync="deedId"
              class="px-6" />
          </v-slide-y-transition>
        </div>
        <div class="flex-grow-1 flex-shrink-0">
          <v-stepper-step
            :complete="stepper > 3"
            step="3"
            class="ma-0">
            <span class="font-weight-bold dark-grey-color text-subtitle-1">
              {{ $t('wom.chooseRewardingReceiver') }}
            </span>
          </v-stepper-step>
          <v-slide-y-transition>
            <wom-integration-rewarding-receiver
              v-show="stepper === 3"
              :address.sync="receiverAddress"
              class="px-6" />
          </v-slide-y-transition>
        </div>
      </v-stepper>
    </template>
    <template v-if="!loading && drawer" #footer>
      <div class="d-flex">
        <v-btn
          v-if="stepper > 1"
          class="btn me-2"
          @click="previous">
          <template>
            {{ $t('wom.previous') }}
          </template>
        </v-btn>
        <v-spacer />
        <v-btn
          class="btn me-2"
          @click="cancel">
          <template>
            {{ $t('wom.cancel') }}
          </template>
        </v-btn>
        <v-btn
          v-if="stepper < 3"
          :disabled="!step1Valid || !step2Valid"
          class="btn primary"
          @click="next">
          <template>
            {{ $t('wom.next') }}
          </template>
        </v-btn>
        <v-btn
          v-else
          :disabled="!step3Valid"
          class="btn primary"
          @click="connect">
          <template>
            {{ $t('wom.connect') }}
          </template>
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    drawer: false,
    loading: false,
    stepper: 1,
    address: null,
    receiverAddress: null,
    token: null,
    signature: null,
    deedId: null,
  }),
  computed: {
    step1Valid() {
      return this.stepper !== 1 || this.token && this.address && this.signature;
    },
    step2Valid() {
      return this.stepper !== 2 || this.deedId;
    },
    step3Valid() {
      return this.stepper !== 3 || this.receiverAddress;
    },
  },
  watch: {
    loading() {
      if (this.loading) {
        this.$refs.drawer.startLoading();
      } else {
        this.$refs.drawer.endLoading();
      }
    },
  },
  methods: {
    open() {
      this.$refs.drawer.open();
    },
    init() {
      this.stepper = 1;

      this.loading = true;
      this.$tenantService.getConfiguration()
        .then(configuration => {
          this.token = configuration.token;
          this.receiverAddress = configuration.adminWallet;
        })
        .finally(() => this.loading = false);
    },
    reset() {
      this.address = null;
      this.signature = null;
      this.deedId = null;
      this.$refs?.managerSelector?.reset();
    },
    cancel() {
      this.$refs.drawer.close();
      this.reset();
    },
    next() {
      if (this.stepper === 1 && this.step1Valid) {
        this.stepper = 2;
      } else if (this.stepper === 2 && this.step2Valid) {
        this.stepper = 3;
      }
    },
    previous() {
      if (this.stepper > 1) {
        this.stepper--;
      }
    },
    connect() {
      // TODO
    },
  },
};
</script>