/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.tenant.metamask.authenticator;

import org.apache.commons.lang3.StringUtils;
import org.gatein.sso.agent.tomcat.ServletAccess;

import org.exoplatform.services.organization.auth.AuthenticatorPlugin;
import org.exoplatform.services.security.*;

import io.meeds.tenant.metamask.service.MetamaskLoginService;
import io.meeds.tenant.metamask.web.filter.MetamaskSignInFilter;

/**
 * An authenticator plugin to authenticate Metamask users
 */
public class MetamaskAuthenticator extends AuthenticatorPlugin {

  private MetamaskLoginService metamaskLoginService;

  public MetamaskAuthenticator(MetamaskLoginService metamaskLoginService) {
    this.metamaskLoginService = metamaskLoginService;
  }

  @Override
  public String validateUser(Credential[] credentials) { // NOSONAR
    if (credentials != null && credentials.length == 2 && credentials[0] instanceof UsernameCredential usernameCredential
        && credentials[1] instanceof PasswordCredential passwordCredential) {
      String compoundPassword = passwordCredential.getPassword();
      String[] passwordParts = StringUtils.split(compoundPassword, MetamaskSignInFilter.SEPARATOR);
      if (passwordParts != null) {
        String walletAddress;
        String rawMessage;
        String signedMessage;
        if (passwordParts.length == 2
            && MetamaskSignInFilter.METAMASK_SIGNED_MESSAGE_PREFIX.replace("@", "").equals(passwordParts[0])) {
          walletAddress = usernameCredential.getUsername();
          rawMessage = metamaskLoginService.getLoginMessage(ServletAccess.getRequest().getSession());
          signedMessage = passwordParts[1];
        } else if (passwordParts.length == 3) {
          walletAddress = passwordParts[0];
          rawMessage = passwordParts[1];
          signedMessage = passwordParts[2];
        } else {
          return null;
        }
        boolean validated = metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, signedMessage);
        if (validated) {
          return metamaskLoginService.getUserWithWalletAddress(walletAddress);
        }
      }
    }
    return null;
  }

}
