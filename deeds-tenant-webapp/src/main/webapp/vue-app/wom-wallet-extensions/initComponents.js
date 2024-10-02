/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
import WoMRewardExtension from './components/WoMRewardExtension.vue';
import WoMStatusRewardExtension from './components/WoMStatusRewardExtension.vue';
import WoMWalletExtension from './components/WoMWalletExtension.vue';

import UEMClaimDrawer from './components/wallet/UEMClaimDrawer.vue';
import ClaimButton from './components/wallet/ClaimButton.vue';
import WoMSetupDrawer from '../wom-setup-admin/components/connection/WoMSetupDrawer.vue';
import WoMConnectionStepper from '../wom-setup-admin/components/connection/stepper/WoMConnectionStepper.vue';
import WoMDisconnectionStepper from '../wom-setup-admin/components/connection/stepper/WoMDisconnectionStepper.vue';
import DeedManagerSelector from '../wom-setup-admin/components/connection/form/DeedManagerSelector.vue';
import MetamaskButton from '../wom-setup-admin/components/connection/form/MetamaskButton.vue';
import ConnectButton from '../wom-setup-admin/components/connection/form/ConnectButton.vue';
import DisconnectButton from '../wom-setup-admin/components/connection/form/DisconnectButton.vue';
import DeedChip from '../wom-setup-admin/components/common/DeedChip.vue';
import HubCard from '../wom-setup-admin/components/connection/view/HubCard.vue';
import ConnectedLabel from '../wom-setup-admin/components/connection/text/ConnectedLabel.vue';
import DisconnectedLabel from '../wom-setup-admin/components/connection/text/DisconnectedLabel.vue';
import NotConnectedLabel from '../wom-setup-admin/components/connection/text/NotConnectedLabel.vue';
import BridgingLabel from '../wom-setup-admin/components/connection/text/BridgingLabel.vue';
import ConnectionLabel from '../wom-setup-admin/components/connection/text/ConnectionLabel.vue';
import ConnectingLabel from '../wom-setup-admin/components/connection/text/ConnectingLabel.vue';
import HubRewards from '../wom-setup-admin/components/rewards/HubRewards.vue';
import HubRewardItem from '../wom-setup-admin/components/rewards/HubRewardItem.vue';
import HubRewardItemMenu from '../wom-setup-admin/components/rewards/HubRewardItemMenu.vue';
import HubRewardStatus from '../wom-setup-admin/components/rewards/HubRewardStatus.vue';
import HubRewardUsersList from '../wom-setup-admin/components/rewards/HubRewardUsersList.vue';
import Address from '../wom-setup-admin/components/common/Address.vue';
import AddressIcon from '../wom-setup-admin/components/common/AddressIcon.vue';
import DeedSelector from '../wom-setup-admin/components/connection/form/DeedSelector.vue';
import DeedItem from '../wom-setup-admin/components/connection/form/DeedItem.vue';
import BlockchainChip from '../wom-setup-admin/components/common/BlockchainChip.vue';

const components = {
  'wom-reward-extension': WoMRewardExtension,
  'wom-status-reward-extension': WoMStatusRewardExtension,
  'wom-wallet-extension': WoMWalletExtension,
  'uem-claim-drawer': UEMClaimDrawer,
  'uem-claim-button': ClaimButton,

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
