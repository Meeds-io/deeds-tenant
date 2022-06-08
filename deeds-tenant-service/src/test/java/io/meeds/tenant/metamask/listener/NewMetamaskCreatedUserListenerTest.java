package io.meeds.tenant.metamask.listener;

import io.meeds.tenant.metamask.service.MetamaskLoginService;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.idm.UserImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NewMetamaskCreatedUserListenerTest {

  @Mock
  OrganizationService  organizationService;

  @Mock
  UserHandler          userHandler;

  @Mock
  PortalContainer      container;

  @Mock
  MetamaskLoginService metamaskLoginService;

  @Before
  public void setUp() {
    container = PortalContainer.getInstance();
    reset(organizationService, userHandler);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
  }

  @Test
  public void testNewMetamaskCreatedUserListener() throws Exception {

    String username = "0x29H59f54055966197fC2442Df38B6C980ff56585";
    UserHandler userHandler = organizationService.getUserHandler();
    String finalUsername = username;
    when(userHandler.createUserInstance(any())).thenAnswer(new Answer<User>() {
      @Override
      public User answer(InvocationOnMock invocation) throws Throwable {
        return new UserImpl(finalUsername);
      }
    });

    username = StringUtils.lowerCase(username);
    User user = userHandler.createUserInstance(username);
    user.setFullName("test user");
    user.setEmail("user@test.com");
    UserEventListener listener = mock(NewMetamaskCreatedUserListener.class);
    userHandler.addUserEventListener(listener);
    doAnswer(invocation -> {
      listener.postSave(user, true);
      return null;
    }).when(metamaskLoginService).registerUser(any(), any(), any());

    metamaskLoginService.registerUser(username, "test user", "user@test.com");
    verify(listener, times(1)).postSave(user, true);
  }
}
