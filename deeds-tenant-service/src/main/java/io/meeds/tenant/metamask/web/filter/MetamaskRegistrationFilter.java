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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.rest.UserFieldValidator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.JspBasedWebHandler;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.web.login.LoginUtils;

import io.meeds.tenant.metamask.RegistrationException;
import io.meeds.tenant.metamask.service.MetamaskLoginService;

/**
 * A Login extension to submit Login parameters to UI for used network, contract
 * adresses ...
 */
public class MetamaskRegistrationFilter extends JspBasedWebHandler implements Filter {

  private static final String             EMAIL_REQUEST_PARAM            = "email";

  private static final String             FULL_NAME_REQUEST_PARAM        = "fullName";

  public static final Log                 LOG                            =
                                              ExoLogger.getLogger(MetamaskRegistrationFilter.class);

  public static final String              SEPARATOR                      = "@";

  public static final String              METAMASK_AUTHENTICATED         = "metamask.authenticated";

  public static final String              METAMASK_REGISTER_USER         = "metamaskUserRegistration";

  private static final String             USERNAME_REQUEST_PARAM         = "username";

  private static final String             PASSWORD_REQUEST_PARAM         = "password";

  public static final String              METAMASK_SIGNED_MESSAGE_PREFIX = "SIGNED_MESSAGE@";

  private static final UserFieldValidator EMAIL_VALIDATOR                =
                                                          new UserFieldValidator(EMAIL_REQUEST_PARAM, false, false);

  private MetamaskLoginService            metamaskLoginService;

  private WebAppController                webAppController;

  private ServletContext                  servletContext;

  public MetamaskRegistrationFilter(PortalContainer container, // NOSONAR
                                    WebAppController webAppController,
                                    LocaleConfigService localeConfigService,
                                    BrandingService brandingService,
                                    JavascriptConfigService javascriptConfigService,
                                    SkinService skinService,
                                    MetamaskLoginService metamaskLoginService) {
    super(localeConfigService, brandingService, javascriptConfigService, skinService);
    this.webAppController = webAppController;
    this.metamaskLoginService = metamaskLoginService;
    this.servletContext = container.getPortalContext();
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, // NOSONAR
                                                                                                          ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    // If user is already authenticated, no registration form is required
    if (request.getRemoteUser() != null) {
      chain.doFilter(request, response);
      return;
    }

    String walletAddress = request.getParameter(USERNAME_REQUEST_PARAM);
    String password = request.getParameter(PASSWORD_REQUEST_PARAM);

    try {
      if (request.getParameter(METAMASK_REGISTER_USER) != null) {
        // Proceed to user registration
        request = registerUserAndWrapRequestForLogin(request, response);
        if (request == null) {// Error happened and response already
                              // handled
          return;
        }
      } else if (StringUtils.isNotBlank(walletAddress) && StringUtils.startsWith(password, METAMASK_SIGNED_MESSAGE_PREFIX)) {
        // Forward to user registration form is user isn't found and
        // registration of new users is allowed
        String username = metamaskLoginService.getUserWithWalletAddress(walletAddress);
        if (StringUtils.isBlank(username)) { // User not found
          // Check whether user registration is allowed or not
          if (metamaskLoginService.isAllowUserRegistration(walletAddress)) {
            // Forward to user registration form after signedMessage validation
            String rawMessage = metamaskLoginService.getLoginMessage(request.getSession());
            String signedMessage = password.replace(METAMASK_SIGNED_MESSAGE_PREFIX, "");

            boolean messageValidated = metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, signedMessage);
            if (messageValidated) {
              // Preserve original username & password fields in session in
              // order to login user after registration
              request.getSession().setAttribute(USERNAME_REQUEST_PARAM, walletAddress);
              request.getSession().setAttribute(PASSWORD_REQUEST_PARAM, getCompoundPassword(request));

              forwardUserRegistrationForm(new ControllerContext(webAppController,
                                                                webAppController.getRouter(),
                                                                request,
                                                                response,
                                                                null),
                                          null);
              return;
            }
          }
        } else {
          // Proceed to login with Metamask uing regular LoginModule
          request = wrapPasswordForLogin(request, username);
        }
      }
    } catch (Exception e) {
      LOG.warn("Error while authenticating user using metamask", e);
    }
    chain.doFilter(request, response);
  }

  private void forwardUserRegistrationForm(ControllerContext controllerContext, String errorCode) throws Exception {
    HttpServletRequest request = controllerContext.getRequest();
    HttpServletResponse response = controllerContext.getResponse();

    List<String> additionalCSSModules = Collections.singletonList("portal/login");
    super.prepareDispatch(controllerContext,
                          "SHARED/metamaskRegisterForm",
                          null,
                          additionalCSSModules,
                          params -> {
                            try {
                              params.put(USERNAME_REQUEST_PARAM, request.getSession().getAttribute(USERNAME_REQUEST_PARAM));
                              params.put(FULL_NAME_REQUEST_PARAM, request.getParameter(FULL_NAME_REQUEST_PARAM));
                              params.put(EMAIL_REQUEST_PARAM, request.getParameter(EMAIL_REQUEST_PARAM));
                              params.put(LoginUtils.COOKIE_NAME, request.getParameter(LoginUtils.COOKIE_NAME));
                              if (StringUtils.isNotBlank(errorCode)) {
                                params.put("errorCode", errorCode);
                              }
                            } catch (JSONException e) {
                              LOG.warn("Error putting error code in parameters", e);
                            }
                          });
    servletContext.getRequestDispatcher("/WEB-INF/jsp/metamaskRegisterForm.jsp").include(request, response);
  }

  private HttpServletRequest registerUserAndWrapRequestForLogin(HttpServletRequest request,
                                                                HttpServletResponse response) throws Exception {
    String username = (String) request.getSession().getAttribute(USERNAME_REQUEST_PARAM);
    String fullName = request.getParameter(FULL_NAME_REQUEST_PARAM);
    String email = request.getParameter(EMAIL_REQUEST_PARAM);

    try {
      if (StringUtils.isBlank(username)) {
        throw new RegistrationException("USERNAME_MANDATORY");
      } else if (!metamaskLoginService.isAllowUserRegistration(username)) {
        throw new RegistrationException("REGISTRATION_NOT_ALLOWED");
      } else if (StringUtils.isNotBlank(email)) {
        String errorCode = EMAIL_VALIDATOR.validate(request.getLocale(), email);
        if (StringUtils.isNotBlank(errorCode)) {
          throw new RegistrationException(errorCode);
        }
      }

      metamaskLoginService.registerUser(username, fullName, email);

      // Proceed to login with Metamask uing regular LoginModule
      return wrapCredentialsForLogin(request);
    } catch (RegistrationException e) {
      String errorCode = e.getMessage();
      forwardUserRegistrationForm(new ControllerContext(webAppController,
                                                        webAppController.getRouter(),
                                                        request,
                                                        response,
                                                        null),
                                  errorCode);
      return null;
    } catch (Exception e) {
      forwardUserRegistrationForm(new ControllerContext(webAppController,
                                                        webAppController.getRouter(),
                                                        request,
                                                        response,
                                                        null),
                                  "REGISTRATION_ERROR");
      return null;
    }
  }

  /**
   * Replaces original request by a wrapped one to give the raw & signed
   * messages in password field
   * 
   * @param request {@link HttpServletRequest}
   * @param username
   * @return {@link HttpServletRequestWrapper}
   */
  private HttpServletRequest wrapPasswordForLogin(HttpServletRequest request, String username) {
    String compoundPassword = getCompoundPassword(request);
    return new HttpServletRequestWrapper(request) {
      @Override
      public String getParameter(String name) {
        if (StringUtils.equals(name, PASSWORD_REQUEST_PARAM)) {
          return compoundPassword;
        }
        if (StringUtils.equals(name, USERNAME_REQUEST_PARAM)) {
          return username;
        }
        return super.getParameter(name);
      }
    };
  }

  /**
   * Replaces original request by a wrapped one to give the password & username
   * fields to LoginModule
   * 
   * @param request {@link HttpServletRequest}
   * @return {@link HttpServletRequestWrapper}
   */
  private HttpServletRequest wrapCredentialsForLogin(HttpServletRequest request) {
    String username = (String) request.getSession().getAttribute(USERNAME_REQUEST_PARAM);
    String password = (String) request.getSession().getAttribute(PASSWORD_REQUEST_PARAM);

    return new HttpServletRequestWrapper(request) {
      @Override
      public String getParameter(String name) {
        if (StringUtils.equals(name, USERNAME_REQUEST_PARAM)) {
          return username;
        }
        if (StringUtils.equals(name, PASSWORD_REQUEST_PARAM)) {
          return password;
        }
        return super.getParameter(name);
      }
    };
  }

  private String getCompoundPassword(HttpServletRequest request) {
    String walletAddress = request.getParameter(USERNAME_REQUEST_PARAM);
    String password = request.getParameter(PASSWORD_REQUEST_PARAM);
    String rawMessage = metamaskLoginService.getLoginMessage(request.getSession());
    String signedMessage = password.replace(METAMASK_SIGNED_MESSAGE_PREFIX, "");
    return walletAddress + SEPARATOR + rawMessage + SEPARATOR + signedMessage;
  }

}
