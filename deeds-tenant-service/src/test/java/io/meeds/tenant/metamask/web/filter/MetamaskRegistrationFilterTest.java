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

import static io.meeds.tenant.metamask.web.filter.MetamaskRegistrationFilter.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.exoplatform.container.*;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.web.filter.FilterDefinition;

import io.meeds.tenant.metamask.service.MetamaskLoginService;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore(
  {
      "com.sun.*",
      "org.w3c.*",
      "javax.xml.*",
      "javax.management.*",
      "org.xml.*",
  }
)
public class MetamaskRegistrationFilterTest {

  @Mock
  private WebAppController           webAppController;

  @Mock
  private LocaleConfigService        localeConfigService;

  @Mock
  private BrandingService            brandingService;

  @Mock
  private JavascriptConfigService    javascriptConfigService;

  @Mock
  private SkinService                skinService;

  @Mock
  private MetamaskLoginService       metamaskLoginService;

  @Mock
  private HttpServletRequest         request;

  @Mock
  private HttpServletResponse        response;

  @Mock
  private FilterChain                chain;

  @Mock
  private ServletContext             context;

  @Mock
  private HttpSession                session;

  @Mock
  private RequestDispatcher          requestDispatcher;

  private PortalContainer            container;

  private MetamaskRegistrationFilter filter;

  private JSONObject                 forwardParameters;

  @Before
  public void setUp() throws Exception {
    // Try to not start a portal container statically
    // And register a fake StandaloneContainer as top container
    try {
      URL url = this.getClass().getClassLoader().getResource("exo-configuration.xml");
      StandaloneContainer.setConfigurationPath(url.getPath());
      StandaloneContainer fakeStandaloneContainer = StandaloneContainer.getInstance();
      ExoContainerContext.setCurrentContainer(fakeStandaloneContainer);
    } catch (Exception e) {
      // Nothing to do
    }

    Mockito.reset(metamaskLoginService,
                  request,
                  response,
                  chain,
                  context,
                  requestDispatcher);

    container = mock(PortalContainer.class);
    when(container.getPortalContext()).thenReturn(context);
    when(context.getRequestDispatcher(any())).thenReturn(requestDispatcher);
    when(request.getContextPath()).thenReturn("/portal");
    when(request.getSession()).thenReturn(session);
    filter = spy(new MetamaskRegistrationFilter(container,
                                                webAppController,
                                                localeConfigService,
                                                brandingService,
                                                javascriptConfigService,
                                                skinService,
                                                metamaskLoginService));
    when(filter.prepareDispatch(any(), any())).thenAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        String errorCode = invocation.getArgument(1);
        forwardParameters = new JSONObject();
        filter.addRegisterFormParams(forwardParameters, request, errorCode);
        return true;
      }
    });
  }

  @Test
  public void testFilterDefinition() {
    InitParams params = mock(InitParams.class);
    MetamaskRegistrationFilterDefinition filterDefinition = new MetamaskRegistrationFilterDefinition(container,
                                                                                                     webAppController,
                                                                                                     localeConfigService,
                                                                                                     brandingService,
                                                                                                     javascriptConfigService,
                                                                                                     skinService,
                                                                                                     metamaskLoginService,
                                                                                                     params);
    Filter filter = filterDefinition.getFilter();
    assertNotNull(filter);
    assertEquals(MetamaskRegistrationFilter.class, filter.getClass());

    List<FilterDefinition> filterDefinitions = filterDefinition.getFilterDefinitions();
    assertNotNull(filterDefinitions);
    assertEquals(1, filterDefinitions.size());
    assertNotNull(filterDefinitions.get(0));
    assertEquals(MetamaskRegistrationFilter.class, filterDefinitions.get(0).getFilter().getClass());
  }

  @Test
  public void testCannotDisplayRegisterFormWhenSingedIn() throws IOException, ServletException {
    lenient().when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);
    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn("fakeUser");
    when(request.getParameter(PASSWORD_REQUEST_PARAM)).thenReturn(METAMASK_SIGNED_MESSAGE_PREFIX + "fakePassword");
    when(request.getRemoteUser()).thenReturn("fakeUser");
    filter.doFilter(request, response, chain);
    verifyNoInteractions(context);
    verify(chain, times(1)).doFilter(request, response);
  }

  @Test
  public void testContinueFilterChainEvenWhenErrorOccurs() throws IOException, ServletException {
    lenient().when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);
    when(request.getParameter(any())).thenThrow(new IllegalStateException("Fake Exception For Test!"));
    filter.doFilter(request, response, chain);
    verifyNoInteractions(context);
    verify(chain, times(1)).doFilter(request, response);
  }

  @Test
  public void testCannotDisplayRegisterFormWhenDisabled() throws Exception {
    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn("fakeUser");
    filter.doFilter(request, response, chain);
    verify(chain, times(1)).doFilter(request, response);
    verifyNoInteractions(context);
    verify(filter, times(0)).forwardUserRegistrationForm(any(), any());
  }

  @Test
  public void testCannotDisplayRegisterFormWhenCredentialsNotValidated() throws Exception {
    when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);
    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn("fakeUser");
    when(request.getParameter(PASSWORD_REQUEST_PARAM)).thenReturn("fakePassword");
    filter.doFilter(request, response, chain);
    verify(chain, times(1)).doFilter(request, response);
    verifyNoInteractions(context);
    verify(filter, times(0)).forwardUserRegistrationForm(any(), any());
  }

  @Test
  public void testDenyRegisterFormWhenHasUnrecognizedCredentials() throws Exception {
    when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);
    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn("fakeUser");
    when(request.getParameter(PASSWORD_REQUEST_PARAM)).thenReturn(METAMASK_SIGNED_MESSAGE_PREFIX + "fakePassword");
    filter.doFilter(request, response, chain);
    verify(filter, times(1)).forwardUserRegistrationForm(any(),
                                                         eq("INVALID_CREDENTIALS"));
    verify(context, times(1)).getRequestDispatcher(METAMASK_REGISTER_FORM);
    verifyNoInteractions(chain);
    assertNotNull(forwardParameters);
    assertTrue(forwardParameters.has(ERROR_CODE_PARAM));
    assertEquals("INVALID_CREDENTIALS", forwardParameters.getString(ERROR_CODE_PARAM));
  }

  @Test
  public void testDisplayRegisterFormWhenHasRecognizedCredentials() throws Exception {
    when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);
    String walletAddress = "fakeUser";
    String rawMessageToSign = "rawMessage";
    String signedMessage = "signedMessage";

    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(request.getParameter(PASSWORD_REQUEST_PARAM)).thenReturn(METAMASK_SIGNED_MESSAGE_PREFIX + signedMessage);
    when(metamaskLoginService.getLoginMessage(any())).thenReturn(rawMessageToSign);
    when(metamaskLoginService.validateSignedMessage(walletAddress, rawMessageToSign, signedMessage)).thenReturn(true);
    when(session.getAttribute(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);

    String compoundPassword = filter.getCompoundPassword(request);

    filter.doFilter(request, response, chain);
    verify(filter, times(1)).forwardUserRegistrationForm(any(),
                                                         eq(null));
    verify(context, times(1)).getRequestDispatcher(METAMASK_REGISTER_FORM);
    verifyNoInteractions(chain);
    verify(context, times(1)).getRequestDispatcher(METAMASK_REGISTER_FORM);
    verify(session, times(1)).setAttribute(USERNAME_REQUEST_PARAM, walletAddress);
    verify(session, times(1)).setAttribute(PASSWORD_REQUEST_PARAM, compoundPassword);
    assertNotNull(forwardParameters);
    assertEquals(walletAddress, forwardParameters.getString(USERNAME_REQUEST_PARAM));
    assertFalse(forwardParameters.has(ERROR_CODE_PARAM));
  }

  @Test
  public void testProceedToLoginWhenUserAlreadyRegistered() throws Exception {
    when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);

    String walletAddress = "fakeUser";
    String rawMessageToSign = "rawMessage";
    String signedMessage = "signedMessage";

    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(request.getParameter(PASSWORD_REQUEST_PARAM)).thenReturn(METAMASK_SIGNED_MESSAGE_PREFIX + signedMessage);
    when(metamaskLoginService.getUserWithWalletAddress(walletAddress)).thenReturn(walletAddress);
    when(metamaskLoginService.getLoginMessage(any())).thenReturn(rawMessageToSign);

    String compoundPassword = filter.getCompoundPassword(request);

    filter.doFilter(request, response, chain);
    verify(filter, times(0)).forwardUserRegistrationForm(any(), any());
    verify(context, times(0)).getRequestDispatcher(METAMASK_REGISTER_FORM);
    verify(context, times(0)).getRequestDispatcher(METAMASK_REGISTER_FORM);
    verify(chain, times(1)).doFilter(argThat(new ArgumentMatcher<ServletRequest>() {
      public boolean matches(ServletRequest argument) {
        return StringUtils.equals(walletAddress, argument.getParameter(USERNAME_REQUEST_PARAM))
            && StringUtils.equals(compoundPassword, argument.getParameter(PASSWORD_REQUEST_PARAM));
      }
    }), any());
  }

  @Test
  public void testFailToRegisterWhenNoUsernameInSession() throws Exception {
    lenient().when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);

    String walletAddress = "fakeUser";
    String rawMessageToSign = "rawMessage";
    String signedMessage = "signedMessage";

    when(request.getParameter(METAMASK_REGISTER_USER)).thenReturn("true");
    when(request.getParameter(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(request.getParameter(PASSWORD_REQUEST_PARAM)).thenReturn(METAMASK_SIGNED_MESSAGE_PREFIX + signedMessage);
    when(metamaskLoginService.getLoginMessage(any())).thenReturn(rawMessageToSign);

    String compoundPassword = filter.getCompoundPassword(request);
    when(session.getAttribute(PASSWORD_REQUEST_PARAM)).thenReturn(compoundPassword);

    filter.doFilter(request, response, chain);
    verify(chain, times(1)).doFilter(request, response);
    verifyNoInteractions(context);
    verify(filter, times(0)).forwardUserRegistrationForm(any(), any());
  }

  @Test
  @PrepareForTest({ ExoContainerContext.class })
  public void testFailToRegisterWhenInvalidEmail() throws Exception {
    when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);

    String walletAddress = "fakeUser";
    String compoundPassword = "compoundPassword";

    when(request.getParameter(METAMASK_REGISTER_USER)).thenReturn("true");
    when(request.getParameter(EMAIL_REQUEST_PARAM)).thenReturn("invalidEmail");
    when(session.getAttribute(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(session.getAttribute(PASSWORD_REQUEST_PARAM)).thenReturn(compoundPassword);

    ExoContainerContext.setCurrentContainer(container);

    ResourceBundleService resourceBundleService = mock(ResourceBundleService.class);
    ResourceBundle resourceBundle = mock(ResourceBundle.class);

    when(container.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(resourceBundleService);
    when(resourceBundleService.getResourceBundle(any(String[].class), any())).thenReturn(resourceBundle);

    filter.doFilter(request, response, chain);
    verify(filter, times(1)).forwardUserRegistrationForm(any(), eq("EmailAddressValidator.msg.Invalid-input"));
    verifyNoInteractions(chain);
  }

  @Test
  @PrepareForTest({ ExoContainerContext.class })
  public void testFailToRegisterWhenInvalidPassword() throws Exception {
    when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);

    String walletAddress = "fakeUser";
    String email = "testmail@mail.test";
    String compoundPassword = "compoundPassword";

    when(request.getParameter(METAMASK_REGISTER_USER)).thenReturn("true");
    when(request.getParameter(EMAIL_REQUEST_PARAM)).thenReturn(email);
    when(session.getAttribute(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(session.getAttribute(PASSWORD_REQUEST_PARAM)).thenReturn(compoundPassword);

    ExoContainerContext.setCurrentContainer(container);

    ResourceBundleService resourceBundleService = mock(ResourceBundleService.class);
    ResourceBundle resourceBundle = mock(ResourceBundle.class);

    when(container.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(resourceBundleService);
    when(resourceBundleService.getResourceBundle(any(String[].class), any())).thenReturn(resourceBundle);

    filter.doFilter(request, response, chain);
    verify(filter, times(1)).forwardUserRegistrationForm(any(), eq("REGISTRATION_NOT_ALLOWED"));
    verifyNoInteractions(chain);
  }

  @Test
  @PrepareForTest({ ExoContainerContext.class })
  public void testFailToRegisterWhenInvalidPasswordWithGoodStructure() throws Exception {
    when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);

    String walletAddress = "fakeUser";
    String rawMessageToSign = "rawMessage";
    String signedMessage = "signedMessage";
    String email = "testmail@mail.test";
    String compoundPassword = "fakeUser2" + SEPARATOR + rawMessageToSign + SEPARATOR + signedMessage;

    when(request.getParameter(METAMASK_REGISTER_USER)).thenReturn("true");
    when(request.getParameter(EMAIL_REQUEST_PARAM)).thenReturn(email);
    when(session.getAttribute(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(session.getAttribute(PASSWORD_REQUEST_PARAM)).thenReturn(compoundPassword);

    ExoContainerContext.setCurrentContainer(container);

    ResourceBundleService resourceBundleService = mock(ResourceBundleService.class);
    ResourceBundle resourceBundle = mock(ResourceBundle.class);

    when(container.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(resourceBundleService);
    when(resourceBundleService.getResourceBundle(any(String[].class), any())).thenReturn(resourceBundle);

    filter.doFilter(request, response, chain);
    verify(filter, times(1)).forwardUserRegistrationForm(any(), eq("REGISTRATION_NOT_ALLOWED"));
    verifyNoInteractions(chain);
  }

  @Test
  @PrepareForTest({ ExoContainerContext.class })
  public void testFailToRegisterWhenInvalidPasswordWithGoodStructure2() throws Exception {
    when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);

    String walletAddress = "fakeUser";
    String rawMessageToSign = "rawMessage";
    String signedMessage = "signedMessage";
    String email = "testmail@mail.test";
    String compoundPassword = walletAddress + SEPARATOR + "rawMessage2" + SEPARATOR + signedMessage;

    when(request.getParameter(METAMASK_REGISTER_USER)).thenReturn("true");
    when(request.getParameter(EMAIL_REQUEST_PARAM)).thenReturn(email);
    when(session.getAttribute(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(session.getAttribute(PASSWORD_REQUEST_PARAM)).thenReturn(compoundPassword);

    ExoContainerContext.setCurrentContainer(container);

    ResourceBundleService resourceBundleService = mock(ResourceBundleService.class);
    ResourceBundle resourceBundle = mock(ResourceBundle.class);

    when(container.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(resourceBundleService);
    when(resourceBundleService.getResourceBundle(any(String[].class), any())).thenReturn(resourceBundle);

    lenient().when(metamaskLoginService.validateSignedMessage(walletAddress, rawMessageToSign, signedMessage)).thenReturn(true);

    filter.doFilter(request, response, chain);
    verify(filter, times(1)).forwardUserRegistrationForm(any(), eq("REGISTRATION_NOT_ALLOWED"));
    verifyNoInteractions(chain);
  }

  @Test
  @PrepareForTest({ ExoContainerContext.class })
  public void testFailToRegisterWhenInvalidPasswordWithGoodStructure3() throws Exception {
    when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);

    String walletAddress = "fakeUser";
    String rawMessageToSign = "rawMessage";
    String signedMessage = "signedMessage";
    String email = "testmail@mail.test";
    String compoundPassword = walletAddress + SEPARATOR + rawMessageToSign + SEPARATOR + "signedMessage2";

    when(request.getParameter(METAMASK_REGISTER_USER)).thenReturn("true");
    when(request.getParameter(EMAIL_REQUEST_PARAM)).thenReturn(email);
    when(session.getAttribute(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(session.getAttribute(PASSWORD_REQUEST_PARAM)).thenReturn(compoundPassword);

    ExoContainerContext.setCurrentContainer(container);

    ResourceBundleService resourceBundleService = mock(ResourceBundleService.class);
    ResourceBundle resourceBundle = mock(ResourceBundle.class);

    when(container.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(resourceBundleService);
    when(resourceBundleService.getResourceBundle(any(String[].class), any())).thenReturn(resourceBundle);

    lenient().when(metamaskLoginService.validateSignedMessage(walletAddress, rawMessageToSign, signedMessage)).thenReturn(true);

    filter.doFilter(request, response, chain);
    verify(filter, times(1)).forwardUserRegistrationForm(any(), eq("REGISTRATION_NOT_ALLOWED"));
    verifyNoInteractions(chain);
  }

  @Test
  @PrepareForTest({ ExoContainerContext.class })
  public void testFailToRegister() throws Exception {
    when(metamaskLoginService.isAllowUserRegistration(any())).thenReturn(true);

    String walletAddress = "fakeUser";
    String fullName = "fullName";
    String rawMessageToSign = "rawMessage";
    String signedMessage = "signedMessage";
    String email = "testmail@mail.test";
    String compoundPassword = walletAddress + SEPARATOR + rawMessageToSign + SEPARATOR + signedMessage;

    when(request.getParameter(METAMASK_REGISTER_USER)).thenReturn("true");
    when(request.getParameter(EMAIL_REQUEST_PARAM)).thenReturn(email);
    when(request.getParameter(FULL_NAME_REQUEST_PARAM)).thenReturn(fullName);
    when(session.getAttribute(USERNAME_REQUEST_PARAM)).thenReturn(walletAddress);
    when(session.getAttribute(PASSWORD_REQUEST_PARAM)).thenReturn(compoundPassword);

    ExoContainerContext.setCurrentContainer(container);

    ResourceBundleService resourceBundleService = mock(ResourceBundleService.class);
    ResourceBundle resourceBundle = mock(ResourceBundle.class);

    when(container.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(resourceBundleService);
    when(resourceBundleService.getResourceBundle(any(String[].class), any())).thenReturn(resourceBundle);

    when(metamaskLoginService.validateSignedMessage(walletAddress, rawMessageToSign, signedMessage)).thenReturn(true);

    filter.doFilter(request, response, chain);
    verify(metamaskLoginService, times(1)).registerUser(walletAddress, fullName, email);
    verify(filter, times(0)).forwardUserRegistrationForm(any(), any());
    verify(chain, times(1)).doFilter(argThat(new ArgumentMatcher<ServletRequest>() {
      public boolean matches(ServletRequest argument) {
        return StringUtils.equals(walletAddress, argument.getParameter(USERNAME_REQUEST_PARAM))
            && StringUtils.equals(compoundPassword, argument.getParameter(PASSWORD_REQUEST_PARAM))
            && StringUtils.equals(email, argument.getParameter(EMAIL_REQUEST_PARAM))
            && StringUtils.equals("/portal/login", ((HttpServletRequest) argument).getRequestURI());
      }
    }), any());
  }

}
