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
    outlined
    elevation="0"
    class="white-background primary-border-color"
    @click="switchMetamaskNetwork">
    <v-img
      src="/deeds-tenant/images/metamask.svg"
      max-height="25px"
      max-width="25px" />
    <span class="primary--text py-2 ms-2 text-truncate text-none">
      {{ $t('wom.switchMetamaskNetworkTo', {0: targetNetworkName}) }}
    </span>
  </v-btn>
  <v-btn
    v-else-if="validNetwork && !claimed"
    :loading="claiming"
    color="primary"
    elevation="0"
    @click="claim">
    {{ $t('uem.claim') }}
  </v-btn>
</template>
<script>
export default {
  props: {
    hub: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    metamaskNetworkId: null,
    claiming: false,
    claimed: false,
    blockchains: {
      137: {
        name: 'Polygon',
        blockexplorer: 'https://polygonscan.com',
        chainId: '0x89',
        testnet: false,
      },
      80001: {
        name: 'Mumbai',
        chainId: '0x13881',
        blockexplorer: 'https://mumbai.polygonscan.com',
        testnet: true,
      },
    },
    claimAbi: [
      'function claim(address _receiver, uint256 _amount)',
    ],
  }),
  computed: {
    uemAddress() {
      return this.hub?.uemAddress;
    },
    targetNetworkId() {
      return this.hub?.networkId;
    },
    targetBlockchain() {
      return this.targetNetworkId && this.blockchains[this.targetNetworkId];
    },
    targetNetworkName() {
      return this.targetBlockchain?.name;
    },
    validNetwork() {
      return this.targetNetworkId === this.metamaskNetworkId;
    },
    onlyDeedManagerCanClaimRewardsLabel() {
      return this.$t('uem.onlyDeedManagerCanClaimRewards', {
        0: this.hubDeedId,
        1: this.deedManagerAddress,
      });
    },
    hubDeedId() {
      return this.hub?.deedId;
    },
    adminAddress() {
      return this.hub?.adminAddress;
    },
    deedManagerAddress() {
      return this.hub?.deedManagerAddress;
    },
  },
  watch: {
    claiming() {
      this.$emit('claiming', this.claiming);
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
    claim() {
      if (!MetaMaskOnboarding.isMetaMaskInstalled()) {
        const onboarding = new MetaMaskOnboarding();
        return onboarding.startOnboarding();
      } else {
        return this.$metamaskUtils.chooseAccount(this.isMobile)
          .then(address => {
            if (address?.toLowerCase?.() === this.deedManagerAddress.toLowerCase()) {
              return this.sendTransaction();
            } else {
              this.$root.$emit('alert-message', this.onlyDeedManagerCanClaimRewardsLabel, 'error');
            }
          })
          .catch(console.debug) // eslint-disable-line no-console
          .finally(() => this.claiming = false);
      }
    },
    sendTransaction() {
      this.claiming = true;
      const provider = new window.ethers.providers.Web3Provider(window.ethereum);
      return this.$tenantUtils.sendTransaction(
        provider,
        new window.ethers.Contract(
          this.uemAddress,
          this.claimAbi,
          provider
        ),
        'claim(address,uint256)',
        {gasLimit: '300000'},
        [this.adminAddress, 0]
      )
        .then(receipt => {
          if (receipt?.wait) {
            return receipt.wait(1);
          } else {
            throw new Error('uem.errorClaimingRewards');
          }
        })
        .then(receipt => {
          if (receipt?.status) {
            this.$root.$emit('alert-message-html-confeti', this.$t('uem.claimedRewardsSuccessfully'), 'success');
            this.$root.$emit('uem-claim-success');
            this.claiming = false;
            this.claimed = true;
          } else {
            throw new Error('uem.errorClaimingRewards');
          }
        })
        .catch(e => {
          this.claiming = false;
          const error = (e?.data?.message || e?.message || e?.cause || String(e));
          let errorMessageKey = error.includes('wom.') && `wom.${error.split('wom.')[1].split(/[^A-Za-z0-9]/g)[0]}` || error;
          if (!this.$te(errorMessageKey)) {
            errorMessageKey = 'wom.errorConnectingToWom';
          }
          this.$root.$emit('alert-message', this.$t(errorMessageKey), 'error');
        });
    },
  },
};
</script>