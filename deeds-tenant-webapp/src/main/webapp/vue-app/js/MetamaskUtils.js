/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2022 Meeds Association contact@meeds.io
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
export function isMetamaskInstalled() {
  return MetaMaskOnboarding.isMetaMaskInstalled();
}

export async function signInWithMetamask(rawMessage, isMobile) {
  if (!MetaMaskOnboarding.isMetaMaskInstalled()) {
    const onboarding = new MetaMaskOnboarding();
    return onboarding.startOnboarding();
  } else {
    if (!isMobile) {
      try {
        const accounts = await window.ethereum.request({
          method: 'wallet_requestPermissions',
          params: [{
            eth_accounts: {},
          }]
        });
        const account = accounts?.length && accounts[0];
        if (!account) {
          throw new Error('No selected account');
        }
      } catch (e) {
        if (!String(e).includes('32601')) {
          throw e;
        }
      }
    }
    const address = await retrieveAddress();
    const signedMessage = await window.ethereum.request({
      method: 'personal_sign',
      params: [rawMessage, address],
    });
    return `SIGNED_MESSAGE@${signedMessage}`;
  }
}

export function retrieveAddress() {
  return window.ethereum.request({ method: 'eth_accounts' })
    .then(address => address?.length && address[0] || null);
}
