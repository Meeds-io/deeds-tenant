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
  <exo-confirm-dialog
    ref="confirmDialog"
    :title="$t('wom.disconnectFromWomConfirmTitle')"
    :message="$t('wom.disconnectFromWomConfirmMessage')"
    :ok-label="$t('wom.confirm')"
    :cancel-label="$t('wom.cancel')"
    @ok="disconnect" />
</template>
<script>
export default {
  props: {
    hub: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    disconnecting: false,
    disconnectAbi: [
      'function disconnect(address _hubAddress)',
    ],
  }),
  computed: {
    hubAddress() {
      return this.hub?.address;
    },
    womAddress() {
      return this.hub?.womAddress;
    },
  },
  watch: {
    disconnecting() {
      this.$emit('disconnecting', this.disconnecting);
    },
  },
  methods: {
    open() {
      this.$refs.confirmDialog.open();
    },
    disconnect() {
      this.disconnecting = true;
      const provider = new window.ethers.providers.Web3Provider(window.ethereum);
      return this.$tenantUtils.sendTransaction(
        provider,
        new window.ethers.Contract(
          this.womAddress,
          this.disconnectAbi,
          provider
        ),
        'disconnect(address)',
        {gasLimit: '200000'},
        [this.hubAddress]
      )
        .then(receipt => {
          if (receipt?.wait) {
            return receipt.wait(1);
          } else {
            throw new Error('wom.errorDisconnectingToWom');
          }
        })
        .then(receipt => {
          if (receipt?.status) {
            this.$root.$emit('alert-message', this.$t('wom.disconnectedFromWoMSuccessfully'), 'success');
            this.$root.$emit('wom-disconnection-success');
            this.disconnecting = false;
          } else {
            throw new Error('wom.errorDisconnectingToWom');
          }
        })
        .catch(e => {
          this.disconnecting = false;
          const error = (e?.data?.message || e?.message || e?.cause || String(e));
          let errorMessageKey = error.includes('wom.') && `wom.${error.split('wom.')[1].split(/[^A-Za-z0-9]/g)[0]}` || error;
          if (!this.$te(errorMessageKey)) {
            errorMessageKey = 'wom.errorDisconnectingToWom';
          }
          this.$root.$emit('alert-message', this.$t(errorMessageKey), 'error');
        });
    },
  },
};
</script>