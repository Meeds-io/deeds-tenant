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

import static io.meeds.tenant.metamask.web.filter.MetamaskSignInFilter.METAMASK_SIGNED_MESSAGE_PREFIX;
import static io.meeds.tenant.metamask.web.filter.MetamaskSignInFilter.METAMASK_TENANT_SETUP_FORM;
import static io.meeds.tenant.metamask.web.filter.MetamaskSignInFilter.PASSWORD_REQUEST_PARAM;
import static io.meeds.tenant.metamask.web.filter.MetamaskSignInFilter.USERNAME_REQUEST_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.impl.LocaleConfigImpl;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.web.filter.FilterDefinition;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

import io.meeds.tenant.metamask.FakeTestException;
import io.meeds.tenant.metamask.service.MetamaskLoginService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
public class MetamaskSignInFilterTest {

  private static final String        CONTEXT_PATH = "/portal";  // NOSONAR

  private static final String        USERNAME     = "fakeUser";

  @Mock
  private WebAppController           webAppController;

  @Mock
  private LocaleConfigService        localeConfigService;

  @Mock
  private RemindPasswordTokenService remindPasswordTokenService;

  @Mock
  private BrandingService            brandingService;

  @Mock
  private JavascriptConfigService    javascriptConfigService;

  @Mock
  private SkinService                skinService;

  @Mock
  private MetamaskLoginService       metamaskLoginService;

  @Mock
  private ControllerContext          controllerContext;

  @Mock
  private HttpServletRequest         request;

  @Mock
  private HttpServletResponse        response;

  @Mock
  private FilterChain                chain;

  @Mock
  private ServletContext             servletContext;

  @Mock
  private HttpSession                session;

  @Mock
  private RequestDispatcher          requestDispatcher;

  private PortalContainer            container;

  private MetamaskSignInFilter       filter;

  @BeforeEach
  public void setUp() {
    Mockito.reset(metamaskLoginService,
                  request,
                  response,
                  chain,
                  servletContext,
                  requestDispatcher);

    container = mock(PortalContainer.class);
    when(container.getPortalContext()).thenReturn(servletContext);
    filter = spy(new MetamaskSignInFilter(container,
                                          remindPasswordTokenService,
                                          webAppController,
                                          localeConfigService,
                                          brandingService,
                                          javascriptConfigService,
                                          skinService,
                                          metamaskLoginService));
  }

  @Test
  public void testFilterDefinition() {
    MetamaskSignInFilterDefinition filterDefinition = new MetamaskSignInFilterDefinition(container,
                                                                                         remindPasswordTokenService,
                                                                                         webAppController,
                                                                                         localeConfigService,
                                                                                         brandingService,
                                                                                         javascriptConfigService,
                                                                                         skinService,
                                                                                         metamaskLoginService);
    Filter testFilter = filterDefinition.getFilter();
    assertNotNull(testFilter);
    assertEquals(MetamaskSignInFilter.class, testFilter.getClass());

    List<FilterDefinition> filterDefinitions = filterDefinition.getFilterDefinitions();
    assertNotNull(filterDefinitions);
    assertEquals(1, filterDefinitions.size());
    assertNotNull(filterDefinitions.get(0));
    assertEquals(MetamaskSignInFilter.class, filterDefinitions.get(0).getFilter().getClass());
  }

  @Test
  public void testNotForwardToSetupWhenNotDeedOwner() throws IOException, ServletException {
    lenient().when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);
    when(request.getContextPath()).thenReturn(CONTEXT_PATH);
    when(request.getRequestURI()).thenReturn("/portal/tenantSetup");
    when(request.getRemoteUser()).thenReturn(USERNAME);
    when(metamaskLoginService.isDeedHub()).thenReturn(true);
    filter.doFilter(request, response, chain);

    verify(servletContext, never()).getRequestDispatcher(any());
  }

  @Test
  public void testForwardToSetupWhenDeedOwner() throws Exception {
    lenient().when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);
    when(request.getContextPath()).thenReturn(CONTEXT_PATH);
    when(request.getRequestURI()).thenReturn("/portal/tenantSetup");
    when(request.getRemoteUser()).thenReturn(USERNAME);
    when(metamaskLoginService.isDeedManager(USERNAME)).thenReturn(true);
    when(metamaskLoginService.isDeedHub()).thenReturn(true);
    when(servletContext.getRequestDispatcher(any())).thenReturn(requestDispatcher);
    when(request.getContextPath()).thenReturn(CONTEXT_PATH);
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(new LocaleConfigImpl());
    when(javascriptConfigService.getJSConfig(any(), any())).thenReturn(new JSONObject());

    filter.doFilter(request, response, chain);
    verify(servletContext, times(1)).getRequestDispatcher(METAMASK_TENANT_SETUP_FORM);
  }

  @Test
  public void testCannotDisplayRegisterFormWhenSingedIn() throws IOException, ServletException {
    lenient().when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);
    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn(USERNAME);
    when(request.getParameter(PASSWORD_REQUEST_PARAM)).thenReturn(METAMASK_SIGNED_MESSAGE_PREFIX + "fakePassword");
    when(request.getRemoteUser()).thenReturn(USERNAME);
    filter.doFilter(request, response, chain);
    verifyNoInteractions(servletContext);
    verify(chain, times(1)).doFilter(request, response);
  }

  @Test
  public void testContinueFilterChainEvenWhenErrorOccurs() throws IOException, ServletException {
    lenient().when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);
    when(request.getParameter(any())).thenThrow(new FakeTestException());
    filter.doFilter(request, response, chain);
    verifyNoInteractions(servletContext);
    verify(chain, times(1)).doFilter(request, response);
  }

  @Test
  public void testCannotDisplayRegisterFormWhenDisabled() throws Exception {//NOSONAR
    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn(USERNAME);
    filter.doFilter(request, response, chain);
    verify(chain, times(1)).doFilter(request, response);
    verifyNoInteractions(servletContext);
    verify(response, never()).sendRedirect(any());
  }

  @Test
  public void testCannotDisplayRegisterFormWhenCredentialsNotValidated() throws Exception {//NOSONAR
    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn(USERNAME);
    when(request.getParameter(PASSWORD_REQUEST_PARAM)).thenReturn("fakePassword");
    filter.doFilter(request, response, chain);
    verify(chain, times(1)).doFilter(request, response);
    verifyNoInteractions(servletContext);
    verify(response, never()).sendRedirect(any());
  }

  @Test
  public void testProceedToLoginWhenUserAlreadyRegistered() throws Exception {// NOSONAR
    String walletAddress = USERNAME;
    String rawMessageToSign = "rawMessage";
    String signedMessage = "signedMessage";

    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(request.getParameter(PASSWORD_REQUEST_PARAM)).thenReturn(METAMASK_SIGNED_MESSAGE_PREFIX + signedMessage);
    when(metamaskLoginService.getUserWithWalletAddress(walletAddress)).thenReturn(walletAddress);
    when(metamaskLoginService.getLoginMessage(any())).thenReturn(rawMessageToSign);

    String compoundPassword = filter.getCompoundPassword(request);

    filter.doFilter(request, response, chain);
    verify(response, never()).sendRedirect(any());
    verify(servletContext, never()).getRequestDispatcher(any());
    verify(chain,
           times(1)).doFilter(argThat(servletRequest -> StringUtils.equals(walletAddress, servletRequest.getParameter(USERNAME_REQUEST_PARAM))
                                                        && StringUtils.equals(compoundPassword,
                                                                              servletRequest.getParameter(PASSWORD_REQUEST_PARAM))),

                              any());
  }

  @Test
  public void testFailToRegisterWhenNoUsernameInSession() throws Exception {// NOSONAR
    lenient().when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);

    String walletAddress = USERNAME;
    String rawMessageToSign = "rawMessage";
    String signedMessage = "signedMessage";

    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(request.getParameter(PASSWORD_REQUEST_PARAM)).thenReturn(METAMASK_SIGNED_MESSAGE_PREFIX + signedMessage);
    when(metamaskLoginService.getLoginMessage(any())).thenReturn(rawMessageToSign);

    filter.doFilter(request, response, chain);
    verify(chain, times(1)).doFilter(request, response);
    verifyNoInteractions(servletContext);
    verify(response, never()).sendRedirect(any());
  }

}
