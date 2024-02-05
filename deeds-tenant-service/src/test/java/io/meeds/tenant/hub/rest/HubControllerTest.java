/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package io.meeds.tenant.hub.rest;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;
import io.meeds.tenant.hub.model.HubTenant;
import io.meeds.tenant.hub.service.HubService;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.WomConnectionRequest;

import jakarta.servlet.Filter;
import lombok.SneakyThrows;

@SpringBootTest(classes = {
                            HubController.class,
                            PortalAuthenticationManager.class,
})
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
                                  WebSecurityConfiguration.class
})
class HubControllerTest {

  private static final String      TEST_USER     = "testUser";

  private static final String      TEST_PASSWORD = "testPassword";

  public static final ObjectMapper OBJECT_MAPPER;

  static {
    // Workaround when Jackson is defined in shared library with different
    // version and without artifact jackson-datatype-jsr310
    OBJECT_MAPPER = JsonMapper.builder()
                              .configure(JsonReadFeature.ALLOW_MISSING_VALUES, true)
                              .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                              .build();
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
  }

  @MockBean
  private IdentityManager       identityManager;

  @MockBean
  private HubService            hubService;

  @Autowired
  private SecurityFilterChain   filterChain;

  @Autowired
  private WebApplicationContext context;

  @Mock
  private HubTenant             hub;

  private MockMvc               mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .addFilters(filterChain.getFilters().toArray(new Filter[0]))
                             .build();
  }

  @Test
  public void testGetHubWhenAnonymous() throws Exception {
    ResultActions response = mockMvc.perform(get("/hub"));
    response.andExpect(status().isForbidden());
  }

  @Test
  public void testGetHubWhenExistsAndAnonymous() throws Exception {
    lenient().when(hubService.getHub(false)).thenReturn(hub);
    ResultActions response = mockMvc.perform(get("/hub"));
    response.andExpect(status().isForbidden());
  }

  @Test
  public void testGetHubNotExistsWhenAdministrator() throws Exception {
    ResultActions response = mockMvc.perform(get("/hub").with(testAdministrator()));
    response.andExpect(status().isNotFound());
  }

  @Test
  public void testGetHubWhenExists() throws Exception {
    when(hubService.getHub(false)).thenReturn(hub);
    ResultActions response = mockMvc.perform(get("/hub").with(testAdministrator()));
    response.andExpect(status().isOk());
  }

  @Test
  public void testConnectToWomWithoutBody() throws Exception {
    ResultActions response = mockMvc.perform(post("/hub").with(testAdministrator())
                                                         .contentType(MediaType.APPLICATION_JSON)
                                                         .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isBadRequest());
  }

  @Test
  public void testConnectToWomWithEmptyBody() throws Exception {
    WomConnectionRequest connectionRequest = new WomConnectionRequest();
    ResultActions response = mockMvc.perform(post("/hub").with(testAdministrator())
                                                         .content(asJsonString(connectionRequest))
                                                         .contentType(MediaType.APPLICATION_JSON)
                                                         .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isBadRequest());
  }

  @Test
  public void testConnectToWomWithEmptyMandatoryField() throws Exception {
    WomConnectionRequest connectionRequest = new WomConnectionRequest();
    connectionRequest.setDeedId(2l);
    ResultActions response = mockMvc.perform(post("/hub").with(testAdministrator())
                                                         .content(asJsonString(connectionRequest))
                                                         .contentType(MediaType.APPLICATION_JSON)
                                                         .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isBadRequest());

    connectionRequest.setDeedManagerAddress("0x");
    response = mockMvc.perform(post("/hub").with(testAdministrator())
                                           .content(asJsonString(connectionRequest))
                                           .contentType(MediaType.APPLICATION_JSON)
                                           .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isBadRequest());

    connectionRequest.setSignedMessage("signedMessage");
    response = mockMvc.perform(post("/hub").with(testAdministrator())
                                           .content(asJsonString(connectionRequest))
                                           .contentType(MediaType.APPLICATION_JSON)
                                           .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isBadRequest());

    connectionRequest.setToken("token");
    response = mockMvc.perform(post("/hub").with(testAdministrator())
                                           .content(asJsonString(connectionRequest))
                                           .contentType(MediaType.APPLICATION_JSON)
                                           .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());
  }

  @Test
  public void testConnectToWomWhenException() throws Exception {
    WomConnectionRequest connectionRequest = new WomConnectionRequest();
    connectionRequest.setDeedId(2l);
    connectionRequest.setDeedManagerAddress("0x");
    connectionRequest.setSignedMessage("signedMessage");
    connectionRequest.setToken("token");
    when(hubService.connectToWoM(connectionRequest)).thenThrow(WomException.class);

    ResultActions response = mockMvc.perform(post("/hub").with(testAdministrator())
                                                         .content(asJsonString(connectionRequest))
                                                         .contentType(MediaType.APPLICATION_JSON)
                                                         .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isServiceUnavailable());
  }

  private RequestPostProcessor testAdministrator() {
    return user(TEST_USER).password(TEST_PASSWORD)
                          .authorities(new SimpleGrantedAuthority("rewarding"));
  }

  @SneakyThrows
  public static String asJsonString(final Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

}
