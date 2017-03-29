package io.pivotal.security.controller.v1.secret;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.helper.JsonHelper.serializeToString;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.data.SecretDataService;
import io.pivotal.security.domain.NamedJsonSecret;
import io.pivotal.security.helper.JsonHelper;
import io.pivotal.security.request.JsonSetRequest;
import io.pivotal.security.service.AuditLogService;
import io.pivotal.security.service.AuditRecordBuilder;
import io.pivotal.security.util.DatabaseProfileResolver;
import io.pivotal.security.view.SecretView;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(Spectrum.class)
@ActiveProfiles(profiles = {"unit-test"}, resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredentialManagerApp.class)
public class SecretsControllerJsonSetTest {

  private final String secretName = "/my-namespace/secretForSetTest/secret-name";
  @Autowired
  WebApplicationContext webApplicationContext;
  @Autowired
  SecretsController subject;
  @SpyBean
  AuditLogService auditLogService;
  @SpyBean
  SecretDataService secretDataService;
  private MockMvc mockMvc;
  private ResultActions response;

  {
    wireAndUnwire(this);

    beforeEach(() -> {
      mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

      resetAuditLogMock();
    });

    describe("and the type is json", () -> {
      describe("via parameter in request body", () -> {
        it("returns the secret as json", () -> {
          Map<String, Object> nestedValue = new HashMap<>();
          nestedValue.put("num", 10);
          String[] value = {"foo", "bar"};

          Map<String, Object> jsonValue = new HashMap<>();
          jsonValue.put("key", "value");
          jsonValue.put("fancy", nestedValue);
          jsonValue.put("array", value);

          JsonSetRequest request = new JsonSetRequest();
          request.setName(secretName);
          request.setValue(jsonValue);
          request.setType("json");

          final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content(serializeToString(request));

          response = mockMvc.perform(put);

          NamedJsonSecret expected = (NamedJsonSecret) secretDataService.findMostRecent(secretName);

          MockHttpServletResponse result = response.andExpect(status().isOk()).andReturn()
              .getResponse();
          SecretView secretView = JsonHelper
              .deserialize(result.getContentAsString(), SecretView.class);

          assertThat(secretView.getUuid(), equalTo(expected.getUuid().toString()));
          assertThat(secretView.getType(), equalTo("json"));
          assertThat(secretView.getVersionCreatedAt().getEpochSecond(),
              equalTo(expected.getVersionCreatedAt().getEpochSecond()));
          assertThat(secretView.getValue(), equalTo(expected.getValue()));
        });
      });
    });
  }

  private void resetAuditLogMock() throws Exception {
    reset(auditLogService);
    doAnswer(invocation -> {
      final Supplier action = invocation.getArgumentAt(1, Supplier.class);
      return action.get();
    }).when(auditLogService)
        .performWithAuditing(isA(AuditRecordBuilder.class), isA(Supplier.class));
  }
}
