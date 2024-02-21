/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import WoMAdminSetup from './components/WoMAdminSetup.vue';

import WoMConnectionStepper from './components/connection/stepper/WoMConnectionStepper.vue';
import WoMDisconnectionStepper from './components/connection/stepper/WoMDisconnectionStepper.vue';

import ConnectedLabel from './components/connection/text/ConnectedLabel.vue';
import DisconnectedLabel from './components/connection/text/DisconnectedLabel.vue';
import NotConnectedLabel from './components/connection/text/NotConnectedLabel.vue';
import BridgingLabel from './components/connection/text/BridgingLabel.vue';
import ConnectionLabel from './components/connection/text/ConnectionLabel.vue';
import ConnectingLabel from './components/connection/text/ConnectingLabel.vue';

import MetamaskButton from './components/connection/form/MetamaskButton.vue';
import ConnectButton from './components/connection/form/ConnectButton.vue';
import DisconnectButton from './components/connection/form/DisconnectButton.vue';
import DeedManagerSelector from './components/connection/form/DeedManagerSelector.vue';
import DeedSelector from './components/connection/form/DeedSelector.vue';
import DeedItem from './components/connection/form/DeedItem.vue';

import WoMSetupDrawer from './components/connection/WoMSetupDrawer.vue';

import HubCard from './components/connection/view/HubCard.vue';

import HubRewards from './components/rewards/HubRewards.vue';
import HubRewardItem from './components/rewards/HubRewardItem.vue';
import HubRewardItemMenu from './components/rewards/HubRewardItemMenu.vue';
import HubRewardStatus from './components/rewards/HubRewardStatus.vue';
import HubRewardUsersList from './components/rewards/HubRewardUsersList.vue';

import DeedChip from './components/common/DeedChip.vue';
import AddressIcon from './components/common/AddressIcon.vue';
import Address from './components/common/Address.vue';
import BlockchainChip from './components/common/BlockchainChip.vue';

const components = {
  'wom-setup-admin': WoMAdminSetup,
  'wom-setup-drawer': WoMSetupDrawer,
  'wom-setup-connection-stepper': WoMConnectionStepper,
  'wom-setup-disconnection-stepper': WoMDisconnectionStepper,
  'wom-setup-deed-manager-selector': DeedManagerSelector,
  'wom-setup-metamask-button': MetamaskButton,
  'wom-setup-connect-button': ConnectButton,
  'wom-setup-disconnect-button': DisconnectButton,
  'wom-setup-deed-chip': DeedChip,
  'wom-setup-hub-card': HubCard,

  'wom-connected-label': ConnectedLabel,
  'wom-disconnected-label': DisconnectedLabel,
  'wom-not-connected-label': NotConnectedLabel,
  'wom-bridging-label': BridgingLabel,
  'wom-connection-label': ConnectionLabel,
  'wom-connecting-label': ConnectingLabel,

  'wom-setup-hub-rewards': HubRewards,
  'wom-setup-hub-reward-item': HubRewardItem,
  'wom-setup-hub-reward-item-menu': HubRewardItemMenu,
  'wom-setup-hub-reward-status': HubRewardStatus,
  'wom-setup-hub-reward-users-list': HubRewardUsersList,

  'wom-setup-address': Address,
  'wom-setup-address-icon': AddressIcon,
  'wom-setup-deed-selector': DeedSelector,
  'wom-setup-deed-item': DeedItem,
  'wom-setup-hub-blockchain-chip': BlockchainChip,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
