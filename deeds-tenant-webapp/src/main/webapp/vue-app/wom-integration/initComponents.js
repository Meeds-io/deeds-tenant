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

import DeedChip from './components/connection/common/DeedChip.vue';
import AddressIcon from './components/connection/common/AddressIcon.vue';
import Address from './components/connection/common/Address.vue';

import MetamaskButton from './components/connection/form/MetamaskButton.vue';
import DeedManagerSelector from './components/connection/form/DeedManagerSelector.vue';
import DeedSelector from './components/connection/form/DeedSelector.vue';
import RewardingReceiverSelector from './components/connection/form/RewardingReceiverSelector.vue';
import ColorPicker from './components/connection/form/ColorPicker.vue';

import WoMConnectionDrawer from './components/connection/drawer/WoMConnectionDrawer.vue';
import WoMDisconnectionDrawer from './components/connection/drawer/WoMDisconnectionDrawer.vue';
import ColorPickerDrawer from './components/connection/drawer/ColorPickerDrawer.vue';

import HubCard from './components/connection/view/HubCard.vue';

import UEMRewards from './components/rewards/UEMRewards.vue';
import UEMRewardItem from './components/rewards/UEMRewardItem.vue';

const components = {
  'wom-integration': WoMIntegration,
  'wom-integration-connection-summary': WoMConnectionSummary,
  'wom-integration-connection-drawer': WoMConnectionDrawer,
  'wom-integration-disconnection-drawer': WoMDisconnectionDrawer,
  'wom-integration-deed-manager-selector': DeedManagerSelector,
  'wom-integration-rewarding-receiver': RewardingReceiverSelector,
  'wom-integration-metamask-button': MetamaskButton,
  'wom-integration-address': Address,
  'wom-integration-address-icon': AddressIcon,
  'wom-integration-deed-selector': DeedSelector,
  'wom-integration-color-picker': ColorPicker,
  'wom-integration-color-picker-drawer': ColorPickerDrawer,
  'wom-integration-deed-chip': DeedChip,
  'wom-integration-hub-card': HubCard,
  'wom-integration-uem-rewards': UEMRewards,
  'wom-integration-uem-reward-item': UEMRewardItem,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
