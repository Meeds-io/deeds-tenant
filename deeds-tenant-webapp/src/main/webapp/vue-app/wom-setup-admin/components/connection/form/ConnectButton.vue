<!--
 This file is part of the Meeds project (https://meeds.io/).
 
 Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 
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
  <v-btn
    v-if="!validNetwork && targetBlockchain"
    color="primary"
    elevation="0"
    @click="switchMetamaskNetwork">
    <v-img
      src="/deeds-tenant/images/metamask.svg"
      max-height="25px"
      max-width="25px" />
    <span class="white--text py-2 ms-2 text-truncate text-none">
      {{ $t('wom.switchMetamaskNetworkTo', {0: targetNetworkName}) }}
    </span>
  </v-btn>
  <v-btn
    v-else-if="validNetwork && !connected"
    :loading="connecting"
    color="primary"
    elevation="0"
    @click="connect">
    {{ $t('wom.connect') }}
  </v-btn>
</template>
<script>
export default {
  props: {
    deedManagerAddress: {
      type: String,
      default: null,
    },
    deed: {
      type: Object,
      default: null,
    },
    womConnectionParams: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    metamaskNetworkId: null,
    connecting: false,
    connected: false,
    connectAbi: [
      'function connect(address _hubAddress, uint256 _deedId)',
    ],
  }),
  computed: {
    hubAddress() {
      return this?.womConnectionParams?.hubAddress;
    },
    deedId() {
      return this?.womConnectionParams?.deedId;
    },
    womAddress() {
      return this?.womConnectionParams?.womAddress;
    },
    targetNetworkId() {
      return this.womConnectionParams?.networkId;
    },
    targetBlockchain() {
      return this.targetNetworkId && this.$root.blockchains[this.targetNetworkId];
    },
    targetNetworkName() {
      return this.targetBlockchain?.name;
    },
    validNetwork() {
      return this.targetNetworkId === this.metamaskNetworkId;
    },
  },
  watch: {
    connecting() {
      this.$emit('connecting', this.connecting);
    },
  },
  created() {
    window.ethereum.on('chainChanged', this.retrieveMetamaskNetwork);
    this.retrieveMetamaskNetwork();
  },
  methods: {
    retrieveMetamaskNetwork() {
      window.ethereum.request({
        method: 'eth_chainId'
      }).then(networkId => this.metamaskNetworkId = window.ethers.BigNumber.from(networkId).toNumber());
    },
    switchMetamaskNetwork() {
      return window.ethereum.request({
        method: 'wallet_switchEthereumChain',
        params: [{ chainId: this.targetBlockchain.chainId }],
      });
    },
    connect() {
      const provider = new window.ethers.providers.Web3Provider(window.ethereum);
      this.connecting = true;
      return this.$tenantUtils.sendTransaction(
        provider,
        new window.ethers.Contract(
          this.womAddress,
          this.connectAbi,
          provider
        ),
        'connect(address,uint256)',
        {gasLimit: '200000'},
        [this.hubAddress, this.deedId]
      )
        .then(receipt => {
          if (receipt?.wait) {
            return receipt.wait(1);
          } else {
            throw new Error('wom.errorConnectingToWom');
          }
        })
        .then(receipt => {
          if (receipt?.status) {
            this.$root.$emit('alert-message-html-confeti', this.$t('wom.connectedToWoMSuccessfully'), 'success');
            this.$root.$emit('wom-connection-success');
            this.connecting = false;
            this.connected = true;
          } else {
            throw new Error('wom.errorConnectingToWom');
          }
        })
        .catch(e => {
          if (e?.code !== 4001) { // User denied transaction signature
            this.connecting = false;
            const error = (e?.data?.message || e?.message || e?.cause || String(e));
            let errorMessageKey = error.includes('wom.') && `wom.${error.split('wom.')[1].split(/[^A-Za-z0-9]/g)[0]}` || error;
            if (!this.$te(errorMessageKey)) {
              errorMessageKey = 'wom.errorConnectingToWom';
            }
            this.$root.$emit('alert-message', this.$t(errorMessageKey), 'error');
          }
        });
    },
  },
};
</script>