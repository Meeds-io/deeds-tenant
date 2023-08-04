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
import WoMIntegration from './components/WoMIntegration.vue';

import WoMConnectionSummary from './components/connection/WoMConnectionSummary.vue';

import MetamaskButton from './components/connection/form/MetamaskButton.vue';
import DeedManagerSelector from './components/connection/form/DeedManagerSelector.vue';
import DeedSelector from './components/connection/form/DeedSelector.vue';
import RewardingReceiverSelector from './components/connection/form/RewardingReceiverSelector.vue';
import ColorPicker from './components/connection/form/ColorPicker.vue';

import WoMConnectionDrawer from './components/connection/drawer/WoMConnectionDrawer.vue';
import WoMDisconnectionDrawer from './components/connection/drawer/WoMDisconnectionDrawer.vue';
import ColorPickerDrawer from './components/connection/drawer/ColorPickerDrawer.vue';

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
  'wom-integration': WoMIntegration,
  'wom-integration-connection-summary': WoMConnectionSummary,
  'wom-integration-connection-drawer': WoMConnectionDrawer,
  'wom-integration-disconnection-drawer': WoMDisconnectionDrawer,
  'wom-integration-deed-manager-selector': DeedManagerSelector,
  'wom-integration-rewarding-receiver': RewardingReceiverSelector,
  'wom-integration-metamask-button': MetamaskButton,
  'wom-integration-color-picker': ColorPicker,
  'wom-integration-color-picker-drawer': ColorPickerDrawer,
  'wom-integration-deed-chip': DeedChip,
  'wom-integration-hub-card': HubCard,

  'wom-integration-hub-rewards': HubRewards,
  'wom-integration-hub-reward-item': HubRewardItem,
  'wom-integration-hub-reward-item-menu': HubRewardItemMenu,
  'wom-integration-hub-reward-status': HubRewardStatus,
  'wom-integration-hub-reward-users-list': HubRewardUsersList,

  'wom-integration-address': Address,
  'wom-integration-address-icon': AddressIcon,
  'wom-integration-deed-selector': DeedSelector,
  'wom-integration-hub-blockchain-chip': BlockchainChip,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
