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
package io.meeds.tenant.metamask.web.filter;

import static org.exoplatform.web.security.security.CookieTokenService.EXTERNAL_REGISTRATION_TOKEN;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.gatein.wci.security.Credentials;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.JspBasedWebHandler;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

import io.meeds.tenant.metamask.service.MetamaskLoginService;

/**
 * A Login extension to submit Login parameters to UI for used network, contract
 * adresses ...
 */
public class MetamaskSignInFilter extends JspBasedWebHandler implements Filter {

  public static final Log            LOG                            = ExoLogger.getLogger(MetamaskSignInFilter.class);

  public static final String         METAMASK_TENANT_SETUP_FORM     = "/WEB-INF/jsp/metamaskSetupForm.jsp";

  public static final String         METAMASK_AUTHENTICATED         = "metamask.authenticated";

  public static final String         SEPARATOR                      = "@";

  public static final String         USERNAME_REQUEST_PARAM         = "username";

  public static final String         PASSWORD_REQUEST_PARAM         = "password";

  public static final String         INITIAL_URI_REQUEST_PARAM      = "initialURI";

  public static final String         METAMASK_SIGNED_MESSAGE_PREFIX = "SIGNED_MESSAGE@";

  private MetamaskLoginService       metamaskLoginService;

  private WebAppController           webAppController;

  private ServletContext             servletContext;

  private RemindPasswordTokenService remindPasswordTokenService;

  public MetamaskSignInFilter(PortalContainer container, // NOSONAR
                              RemindPasswordTokenService remindPasswordTokenService,
                              WebAppController webAppController,
                              LocaleConfigService localeConfigService,
                              BrandingService brandingService,
                              JavascriptConfigService javascriptConfigService,
                              SkinService skinService,
                              MetamaskLoginService metamaskLoginService) {
    super(localeConfigService, brandingService, javascriptConfigService, skinService);
    this.remindPasswordTokenService = remindPasswordTokenService;
    this.webAppController = webAppController;
    this.metamaskLoginService = metamaskLoginService;
    this.servletContext = container.getPortalContext();
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, // NOSONAR
                                                                                                          ServletException {
    try {
      HttpServletRequest request = (HttpServletRequest) servletRequest;
      HttpServletResponse response = (HttpServletResponse) servletResponse;

      if (isDeedTenantStep(request)) {
        forwardDeedTenantSetupForm(new ControllerContext(webAppController,
                                                         webAppController.getRouter(),
                                                         request,
                                                         response,
                                                         null));
        return;
      }

      String walletAddress = request.getParameter(USERNAME_REQUEST_PARAM);
      String password = request.getParameter(PASSWORD_REQUEST_PARAM);

      // If user is already authenticated, no registration form is required
      if (request.getRemoteUser() == null
          && StringUtils.isNotBlank(walletAddress)
          && StringUtils.startsWith(password, METAMASK_SIGNED_MESSAGE_PREFIX)
          && metamaskLoginService.isAllowUserRegistration(walletAddress)) {
        // Step 1: Forward to user registration form. The user isn't found and
        // registration of new users is allowed
        String username = metamaskLoginService.getUserWithWalletAddress(walletAddress);
        if (StringUtils.isBlank(username)) { // User not found in Database
          // Forward to user registration form after signedMessage validation
          String rawMessage = metamaskLoginService.getLoginMessage(request.getSession());
          String signedMessage = password.replace(METAMASK_SIGNED_MESSAGE_PREFIX, "");
          boolean messageValidated = metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, signedMessage);
          if (messageValidated) {
            // Preserve original username & password fields in session in
            // order to login user after registration
            request.getSession().setAttribute(USERNAME_REQUEST_PARAM, walletAddress);
            request.getSession().setAttribute(PASSWORD_REQUEST_PARAM, getCompoundPassword(request));

            String tokenId = remindPasswordTokenService.createToken(new Credentials(walletAddress, password),
                                                                    EXTERNAL_REGISTRATION_TOKEN);

            String path = servletContext.getContextPath() + "/external-registration?token=" + tokenId;
            if (metamaskLoginService.isDeedTenant() && metamaskLoginService.isTenantManager(walletAddress)) {
              path += "&" + INITIAL_URI_REQUEST_PARAM + "=/portal/tenantSetup";
            }
            response.sendRedirect(path);
            return;
          }
        } else {
          // Proceed to login with Metamask using regular LoginModule
          String compoundPassword = getCompoundPassword(request);
          servletRequest = wrapRequestForLogin(request, username, compoundPassword, false);
        }
      }
    } catch (Exception e) {
      LOG.warn("Error while registering user using metamask", e);
    }
    chain.doFilter(servletRequest, servletResponse);
  }

  protected void forwardDeedTenantSetupForm(ControllerContext controllerContext) throws Exception {
    super.prepareDispatch(controllerContext,
                          "SHARED/metamaskSetupForm",
                          null,
                          Collections.singletonList("portal/login"),
                          null);
    servletContext.getRequestDispatcher(METAMASK_TENANT_SETUP_FORM)
                  .include(controllerContext.getRequest(), controllerContext.getResponse());
  }

  private HttpServletRequestWrapper wrapRequestForLogin(HttpServletRequest request,
                                                        String username,
                                                        String password,
                                                        boolean redirectToSetup) {

    return new HttpServletRequestWrapper(request) {
      @Override
      public String getParameter(String name) {
        if (StringUtils.equals(name, USERNAME_REQUEST_PARAM)) {
          return username;
        } else if (StringUtils.equals(name, PASSWORD_REQUEST_PARAM)) {
          return password;
        } else if (redirectToSetup && StringUtils.equals(name, INITIAL_URI_REQUEST_PARAM)) {
          return getContextPath() + "/tenantSetup";
        } else {
          return super.getParameter(name);
        }
      }

      @Override
      public String getRequestURI() {
        return getContextPath() + "/login";
      }
    };
  }

  protected String getCompoundPassword(HttpServletRequest request) {
    String walletAddress = request.getParameter(USERNAME_REQUEST_PARAM);
    String password = request.getParameter(PASSWORD_REQUEST_PARAM);
    String rawMessage = metamaskLoginService.getLoginMessage(request.getSession());
    String signedMessage = password.replace(METAMASK_SIGNED_MESSAGE_PREFIX, "");
    return walletAddress + SEPARATOR + rawMessage + SEPARATOR + signedMessage;
  }

  private boolean isDeedTenantStep(HttpServletRequest request) {
    String walletAddress = request.getRemoteUser();
    return StringUtils.isNotBlank(walletAddress)
        && StringUtils.equals(request.getRequestURI(), request.getContextPath() + "/tenantSetup")
        && metamaskLoginService.isDeedTenant()
        && metamaskLoginService.isTenantManager(walletAddress);
  }

}
