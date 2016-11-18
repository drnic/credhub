package io.pivotal.security.controller.v1.secret;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.CredentialManagerTestContextBootstrapper;
import io.pivotal.security.data.SecretDataService;
import io.pivotal.security.entity.NamedValueSecret;
import io.pivotal.security.service.AuditLogService;
import io.pivotal.security.service.AuditRecordParameters;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.google.common.collect.Lists.newArrayList;
import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.entity.AuditingOperationCode.CREDENTIAL_ACCESS;
import static io.pivotal.security.helper.SpectrumHelper.mockOutCurrentTimeProvider;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RunWith(Spectrum.class)
@SpringApplicationConfiguration(classes = CredentialManagerApp.class)
@WebAppConfiguration
@BootstrapWith(CredentialManagerTestContextBootstrapper.class)
@ActiveProfiles("unit-test")
public class SecretsControllerGetTest {

  @Autowired
  WebApplicationContext webApplicationContext;

  @Autowired
  @InjectMocks
  SecretsController subject;

  @Spy
  @Autowired
  @InjectMocks
  AuditLogService auditLogService;

  @Spy
  @Autowired
  SecretDataService secretDataService;

  private MockMvc mockMvc;

  private Instant frozenTime = Instant.ofEpochSecond(1400011001L);

  private final Consumer<Long> fakeTimeSetter;

  private final String secretName = "my-namespace/subTree/secret-name";

  private ResultActions response;
  private UUID uuid;

  {
    wireAndUnwire(this);
    fakeTimeSetter = mockOutCurrentTimeProvider(this);

    beforeEach(() -> {
      fakeTimeSetter.accept(frozenTime.toEpochMilli());
      mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

      resetAuditLogMock();
    });

    describe("getting a secret", () -> {
      final String secretValue = "my value";

      beforeEach(() -> {
        uuid = UUID.randomUUID();
        NamedValueSecret valueSecret = new NamedValueSecret(secretName, secretValue).setUuid(uuid).setUpdatedAt(frozenTime);
        NamedValueSecret valueSecret2 = new NamedValueSecret(secretName, secretValue).setUuid(uuid).setUpdatedAt(frozenTime);
        doReturn(
            valueSecret
        ).when(secretDataService).findMostRecent(secretName);
        doReturn(
            newArrayList(valueSecret, valueSecret2)
        ).when(secretDataService).findAllByName(secretName.toUpperCase());
        doReturn(
            valueSecret
        ).when(secretDataService).findMostRecent(secretName.toUpperCase());
        doReturn(
            valueSecret
        ).when(secretDataService).findByUuid(uuid.toString());
      });

      describe("getting a secret by name case-insensitively (with old-style URLs)", makeGetByNameBlock(secretValue, "/api/v1/data/" + secretName.toUpperCase(), "/api/v1/data/invalid_name", "$"));

      describe("getting a secret by name case-insensitively (with name query param)", makeGetByNameBlock(secretValue, "/api/v1/data?name=" + secretName.toUpperCase(), "/api/v1/data?name=invalid_name", "$.data[0]"));

      describe("when passing a 'current' query parameter", () -> {
        it("when true should return only the most recent version", () -> {
          mockMvc.perform(get("/api/v1/data?current=true&name=" + secretName.toUpperCase())
              .accept(APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
              .andExpect(jsonPath("$.data", hasSize(1)));

          verify(secretDataService).findMostRecent(secretName.toUpperCase());
        });

        it("when false should return all versions", () -> {
          mockMvc.perform(get("/api/v1/data?current=false&name=" + secretName.toUpperCase())
              .accept(APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
              .andExpect(jsonPath("$.data", hasSize(greaterThan(1))));
        });

        it("when omitted should return all versions", () -> {
          mockMvc.perform(get("/api/v1/data?name=" + secretName.toUpperCase())
              .accept(APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
              .andExpect(jsonPath("$.data", hasSize(greaterThan(1))));
        });
      });

      describe("getting a secret by id", () -> {
        beforeEach(() -> {
          final MockHttpServletRequestBuilder get = get("/api/v1/data?id=" + uuid)
              .accept(APPLICATION_JSON);

          this.response = mockMvc.perform(get);
        });

        it("should return the secret", () -> {
          this.response.andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
              .andExpect(jsonPath("$.type").value("value"))
              .andExpect(jsonPath("$.value").value(secretValue))
              .andExpect(jsonPath("$.id").value(uuid.toString()))
              .andExpect(jsonPath("$.updated_at").value(frozenTime.toString()));
        });

        it("persists an audit entry", () -> {
          ArgumentCaptor<AuditRecordParameters> captor = ArgumentCaptor.forClass(AuditRecordParameters.class);
          verify(auditLogService, times(1)).performWithAuditing(captor.capture(), any(Supplier.class));
          AuditRecordParameters parameters = captor.getValue();
          assertThat(parameters.getOperationCode(), equalTo(CREDENTIAL_ACCESS));
        });
      });
    });
  }

  private Spectrum.Block makeGetByNameBlock(String secretValue, String validUrl, String invalidUrl, String jsonPathPrefix) {
    return () -> {
      beforeEach(() -> {
        final MockHttpServletRequestBuilder get = get(validUrl)
            .accept(APPLICATION_JSON);

        this.response = mockMvc.perform(get);
      });

      it("should return the secret", () -> {
        this.response.andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath(jsonPathPrefix + ".type").value("value"))
            .andExpect(jsonPath(jsonPathPrefix + ".value").value(secretValue))
            .andExpect(jsonPath(jsonPathPrefix + ".id").value(uuid.toString()))
            .andExpect(jsonPath(jsonPathPrefix + ".updated_at").value(frozenTime.toString()));
      });

      it("persists an audit entry", () -> {
        ArgumentCaptor<AuditRecordParameters> captor = ArgumentCaptor.forClass(AuditRecordParameters.class);
        verify(auditLogService).performWithAuditing(captor.capture(), any(Supplier.class));
        AuditRecordParameters parameters = captor.getValue();
        assertThat(parameters.getOperationCode(), equalTo(CREDENTIAL_ACCESS));
      });

      it("returns NOT_FOUND when the secret does not exist", () -> {
        final MockHttpServletRequestBuilder get = get(invalidUrl)
            .accept(APPLICATION_JSON);

        mockMvc.perform(get)
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Credential not found. Please validate your input and retry your request."));
      });
    };
  }

  private void resetAuditLogMock() throws Exception {
    Mockito.reset(auditLogService);
    doAnswer(invocation -> {
      final Supplier action = invocation.getArgumentAt(1, Supplier.class);
      return action.get();
    }).when(auditLogService).performWithAuditing(isA(AuditRecordParameters.class), isA(Supplier.class));
  }
}
