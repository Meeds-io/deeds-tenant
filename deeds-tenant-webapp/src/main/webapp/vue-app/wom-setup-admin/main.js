/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import './initComponents.js';
import './services.js';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || 'en';
const urls = [
  `/deeds-tenant/i18n/locale.portlet.WoMSetupAdmin?lang=${lang}`
];

const appId = 'WoMSetupAdmin';

export function init() {
  exoi18n.loadLanguageAsync(lang, urls)
    .then(i18n => {
      Vue.createApp({
        data: () => ({
          cities: ['TANIT', 'RESHEF', 'ASHTARTE', 'MELQART', 'ESHMUN', 'KUSHOR', 'HAMMON'],
          cardTypes: ['COMMON', 'UNCOMMON', 'RARE', 'LEGENDARY'],
          hub: null,
        }),
        computed: {
          blockchains() {
            return {
              0: {
                name: this.$t('wom.unkownBlockchain'),
                blockexplorer: '',
                testnet: true,
              },
              1: {
                name: 'Mainnet',
                blockexplorer: 'https://etherscan.io',
                testnet: false,
              },
              5: {
                name: 'Goerli',
                blockexplorer: 'https://goerli.etherscan.io',
                testnet: true,
              },
              137: {
                name: 'Polygon',
                blockexplorer: 'https://polygonscan.com',
                testnet: false,
                chainId: '0x89',
              },
              80001: {
                name: 'Mumbai',
                blockexplorer: 'https://mumbai.polygonscan.com',
                testnet: true,
                chainId: '0x13881',
              },
            };
          },
        },
        created() {
          this.$root.$on('wom-hub-changed', hub => this.hub = hub);
        },
        template: `<wom-setup-admin id="${appId}" />`,
        i18n,
        vuetify: Vue.prototype.vuetifyOptions,
      }, `#${appId}`, 'WoM Setup administration');
    });
}