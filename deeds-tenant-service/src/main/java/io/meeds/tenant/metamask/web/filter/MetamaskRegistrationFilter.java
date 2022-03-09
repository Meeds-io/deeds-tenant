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
import org.json.JSONObject;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.rest.UserFieldValidator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
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

  public static final Log                 LOG                            =
                                              ExoLogger.getLogger(MetamaskRegistrationFilter.class);

  public static final String              METAMASK_REGISTER_FORM         = "/WEB-INF/jsp/metamaskRegisterForm.jsp";

  public static final String              EMAIL_REQUEST_PARAM            = "email";

  public static final String              FULL_NAME_REQUEST_PARAM        = "fullName";

  public static final String              ERROR_CODE_PARAM               = "errorCode";

  public static final String              SEPARATOR                      = "@";

  public static final String              METAMASK_AUTHENTICATED         = "metamask.authenticated";

  public static final String              METAMASK_REGISTER_USER         = "metamaskUserRegistration";

  public static final String              USERNAME_REQUEST_PARAM         = "username";

  public static final String              PASSWORD_REQUEST_PARAM         = "password";

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
    try {
      HttpServletRequest request = (HttpServletRequest) servletRequest;
      HttpServletResponse response = (HttpServletResponse) servletResponse;

      String walletAddress = request.getParameter(USERNAME_REQUEST_PARAM);
      String password = request.getParameter(PASSWORD_REQUEST_PARAM);
      boolean isRegistrationRequest = request.getParameter(METAMASK_REGISTER_USER) != null;
      if (isRegistrationRequest) {
        walletAddress = (String) request.getSession().getAttribute(USERNAME_REQUEST_PARAM);
        password = (String) request.getSession().getAttribute(PASSWORD_REQUEST_PARAM);
      }

      // If user is already authenticated, no registration form is required
      if (request.getRemoteUser() == null
          && StringUtils.isNotBlank(walletAddress)
          && (metamaskLoginService.isSuperUser(walletAddress)
              || metamaskLoginService.isAllowUserRegistration(walletAddress))) {

        if (StringUtils.startsWith(password, METAMASK_SIGNED_MESSAGE_PREFIX)) {
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
            }

            String errorCode = messageValidated ? null : "INVALID_CREDENTIALS";
            forwardUserRegistrationForm(new ControllerContext(webAppController,
                                                              webAppController.getRouter(),
                                                              request,
                                                              response,
                                                              null),
                                        errorCode);
            return;
          } else {
            // Proceed to login with Metamask uing regular LoginModule
            String compoundPassword = getCompoundPassword(request);
            servletRequest = wrapRequestForLogin(request, username, compoundPassword);
          }
        } else if (isRegistrationRequest) {
          // Step 2: Proceed to user registration
          servletRequest = registerUserAndWrapRequestForLogin(request, response);
          if (servletRequest == null) {
            // An error occurred and response forwarding already handled
            return;
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Error while registering user using metamask", e);
    }
    chain.doFilter(servletRequest, servletResponse);
  }

  protected void forwardUserRegistrationForm(ControllerContext controllerContext, String errorCode) throws Exception {
    prepareDispatch(controllerContext, errorCode);
    servletContext.getRequestDispatcher(METAMASK_REGISTER_FORM)
                  .include(controllerContext.getRequest(),
                           controllerContext.getResponse());
  }

  protected boolean prepareDispatch(ControllerContext controllerContext, String errorCode) throws Exception {
    if (controllerContext == null) {
      return false;
    }
    List<String> additionalCSSModules = Collections.singletonList("portal/login");
    HttpServletRequest request = controllerContext.getRequest();

    super.prepareDispatch(controllerContext,
                          "SHARED/metamaskRegisterForm",
                          null,
                          additionalCSSModules,
                          params -> addRegisterFormParams(params, request, errorCode));
    return true;
  }

  protected void addRegisterFormParams(JSONObject params, HttpServletRequest request, String errorCode) {
    HttpSession session = request.getSession();
    try {
      params.put(USERNAME_REQUEST_PARAM,
                 session.getAttribute(USERNAME_REQUEST_PARAM));
      params.put(FULL_NAME_REQUEST_PARAM,
                 request.getParameter(FULL_NAME_REQUEST_PARAM));
      params.put(EMAIL_REQUEST_PARAM,
                 request.getParameter(EMAIL_REQUEST_PARAM));
      params.put(LoginUtils.COOKIE_NAME,
                 request.getParameter(LoginUtils.COOKIE_NAME));
      if (StringUtils.isNotBlank(errorCode)) {
        params.put(ERROR_CODE_PARAM, errorCode);
      }
    } catch (JSONException e) {
      LOG.warn("Error putting error code in parameters", e);
    }
  }

  protected HttpServletRequest registerUserAndWrapRequestForLogin(HttpServletRequest request,
                                                                  HttpServletResponse response) throws Exception {
    String walletAddress = (String) request.getSession().getAttribute(USERNAME_REQUEST_PARAM);
    String password = (String) request.getSession().getAttribute(PASSWORD_REQUEST_PARAM);
    String fullName = request.getParameter(FULL_NAME_REQUEST_PARAM);
    String email = request.getParameter(EMAIL_REQUEST_PARAM);

    try {
      if (StringUtils.isNotBlank(email)) {
        String errorCode = EMAIL_VALIDATOR.validate(request.getLocale(), email);
        if (StringUtils.isNotBlank(errorCode)) {
          throw new RegistrationException(errorCode);
        }
      }
      if (!validateCompoundPassword(walletAddress, password)) {
        throw new RegistrationException("REGISTRATION_NOT_ALLOWED");
      }
      User user = metamaskLoginService.registerUser(walletAddress, fullName, email);

      // Proceed to login with Metamask uing regular LoginModule
      return wrapRequestForLogin(request, user.getUserName(), password);
    } catch (RegistrationException e) {
      String errorCode = e.getMessage();
      forwardUserRegistrationForm(new ControllerContext(webAppController,
                                                        webAppController.getRouter(),
                                                        request,
                                                        response,
                                                        null),
                                  errorCode);
    } catch (Exception e) {
      forwardUserRegistrationForm(new ControllerContext(webAppController,
                                                        webAppController.getRouter(),
                                                        request,
                                                        response,
                                                        null),
                                  "REGISTRATION_ERROR");
    }
    return null;
  }

  private HttpServletRequestWrapper wrapRequestForLogin(HttpServletRequest request, String username, String password) {
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

  private boolean validateCompoundPassword(String walletAddress, String compoundPassword) {
    String[] passwordParts = StringUtils.split(compoundPassword, MetamaskRegistrationFilter.SEPARATOR);
    if (passwordParts != null && passwordParts.length == 3) {
      if (!StringUtils.equalsIgnoreCase(walletAddress, passwordParts[0])) {
        return false;
      }
      String rawMessage = passwordParts[1];
      String signedMessage = passwordParts[2];

      return metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, signedMessage);
    }
    return false;
  }

}
