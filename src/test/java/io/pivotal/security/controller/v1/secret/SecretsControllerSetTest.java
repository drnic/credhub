package io.pivotal.security.controller.v1.secret;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.data.SecretDataService;
import io.pivotal.security.domain.Encryptor;
import io.pivotal.security.domain.NamedCertificateSecret;
import io.pivotal.security.domain.NamedSecret;
import io.pivotal.security.domain.NamedSshSecret;
import io.pivotal.security.domain.NamedValueSecret;
import io.pivotal.security.helper.TestConstants;
import io.pivotal.security.service.AuditLogService;
import io.pivotal.security.service.AuditRecordBuilder;
import io.pivotal.security.util.DatabaseProfileResolver;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.entity.AuditingOperationCode.CREDENTIAL_ACCESS;
import static io.pivotal.security.entity.AuditingOperationCode.CREDENTIAL_UPDATE;
import static io.pivotal.security.helper.SpectrumHelper.mockOutCurrentTimeProvider;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(Spectrum.class)
@ActiveProfiles(profiles = { "unit-test", "UseRealAuditLogService" }, resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredentialManagerApp.class)
public class SecretsControllerSetTest {

  @Autowired
  WebApplicationContext webApplicationContext;

  @Autowired
  SecretsController subject;

  @Autowired
  private Encryptor encryptor;

  @SpyBean
  AuditLogService auditLogService;

  @SpyBean
  SecretDataService secretDataService;

  private MockMvc mockMvc;

  private Instant frozenTime = Instant.ofEpochSecond(1400011001L);

  private final Consumer<Long> fakeTimeSetter;

  private final String secretName = "/my-namespace/secretForSetTest/secret-name";

  private ResultActions response;

  private UUID uuid;
  final String secretValue = "secret-value";

  private ResultActions[] responses;

  {
    wireAndUnwire(this);

    fakeTimeSetter = mockOutCurrentTimeProvider(this);

    beforeEach(() -> {
      fakeTimeSetter.accept(frozenTime.toEpochMilli());
      mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

      resetAuditLogMock();
    });

    describe("setting secrets in parallel", () -> {
      beforeEach(() -> {
        responses = new ResultActions[2];

        Thread thread1 = new Thread("thread 1") {
          @Override
          public void run() {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"type\":\"value\"," +
                    "  \"name\":\"" + secretName + this.getName() + "\"," +
                    "  \"value\":\"" + secretValue + this.getName() + "\"" +
                    "}");

            try {
              responses[0] = mockMvc.perform(put);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        };
        Thread thread2 = new Thread("thread 2") {
          @Override
          public void run() {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"type\":\"value\"," +
                    "  \"name\":\"" + secretName + this.getName() + "\"," +
                    "  \"value\":\"" + secretValue + this.getName() + "\"" +
                    "}");

            try {
              responses[1] = mockMvc.perform(put);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        };

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
      });

      it("test", () -> {
        responses[0].andExpect(jsonPath("$.value").value(secretValue + "thread 1"));
        responses[1].andExpect(jsonPath("$.value").value(secretValue + "thread 2"));
      });
    });

    describe("setting a secret", () -> {
      describe("via parameter in request body", () -> {
        beforeEach(() -> {
          final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                  "  \"type\":\"value\"," +
                  "  \"name\":\"" + secretName + "\"," +
                  "  \"value\":\"" + secretValue + "\"" +
                  "}");

          response = mockMvc.perform(put);
        });

        it("returns the secret as json", () -> {
          NamedSecret expected = secretDataService.findMostRecent(secretName);

          response.andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
              .andExpect(jsonPath("$.type").value("value"))
              .andExpect(jsonPath("$.value").value(secretValue))
              .andExpect(jsonPath("$.id").value(expected.getUuid().toString()))
              .andExpect(jsonPath("$.version_created_at").value(expected.getVersionCreatedAt().toString()));
        });

        it("asks the data service to persist the secret", () -> {
          ArgumentCaptor<NamedValueSecret> argumentCaptor = ArgumentCaptor.forClass(NamedValueSecret.class);

          verify(secretDataService, times(1)).save(argumentCaptor.capture());

          NamedValueSecret namedValueSecret = argumentCaptor.getValue();
          assertThat(namedValueSecret.getValue(), equalTo(secretValue));
        });

        it("persists an audit entry", () -> {
          ArgumentCaptor<AuditRecordBuilder> auditRecordParamsCaptor = ArgumentCaptor.forClass(AuditRecordBuilder.class);
          verify(auditLogService).performWithAuditing(auditRecordParamsCaptor.capture(), any(Supplier.class));

          assertThat(auditRecordParamsCaptor.getValue().getOperationCode(), equalTo(CREDENTIAL_UPDATE));
        });

        describe("error handling", () -> {
          it("returns 400 when the handler raises an exception", () -> {
            NamedValueSecret namedValueSecret = new NamedValueSecret(secretName);
            namedValueSecret.setEncryptor(encryptor);
            namedValueSecret.setValue(secretValue);
            doReturn(
                namedValueSecret
            ).when(secretDataService).findMostRecent(secretName);

            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"type\":\"password\"," +
                    "  \"name\":\"" + secretName + "\"," +
                    "  \"value\":\"some password\"" +
                    "}");

            mockMvc.perform(put)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("The credential type cannot be modified. Please delete the credential if you wish to create it with a different type."));
          });

          it("returns 400 when name is empty", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"type\":\"password\"," +
                    "  \"name\":\"\"," +
                    "  \"value\":\"some password\"" +
                    "}");

            mockMvc.perform(put)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("A credential name must be provided. Please validate your input and retry your request."));
          });

          it("returns 400 when name contains double slash (//)", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"type\":\"password\"," +
                    "  \"name\":\"pass//word\"," +
                    "  \"value\":\"some password\"" +
                    "}");

            mockMvc.perform(put)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("A credential name cannot end with a '/' character or contain '//'. Credential names should be in the form of /[path]/[name] or [path]/[name]. Please update and retry your request."));
          });

          it("returns 400 when name ends with a slash", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"type\":\"password\"," +
                    "  \"name\":\"password/\"," +
                    "  \"value\":\"some password\"" +
                    "}");

            mockMvc.perform(put)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("A credential name cannot end with a '/' character or contain '//'. Credential names should be in the form of /[path]/[name] or [path]/[name]. Please update and retry your request."));
          });

          it("returns 400 when name is missing", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"type\":\"password\"," +
                    "  \"value\":\"some password\"" +
                    "}");

            mockMvc.perform(put)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("A credential name must be provided. Please validate your input and retry your request."));
          });

          it("returns 400 when type is missing", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"name\":\"some-name\"," +
                    "  \"value\":\"some password\"" +
                    "}");

            mockMvc.perform(put)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("The request does not include a valid type. Valid values include 'value', 'password', 'certificate', 'ssh' and 'rsa'."));
          });

          it("returns 400 when value is missing", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"name\":\"some-name\"," +
                    "  \"type\":\"password\"" +
                    "}");

            mockMvc.perform(put)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("A non-empty value must be specified for the credential. Please validate and retry your request."));
          });

          it("returns an error message when an unknown top-level key is present", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"type\":\"value\"," +
                    "  \"name\":\"" + secretName + "\"," +
                    "  \"response_error\":\"invalid key\"," +
                    "  \"value\":\"THIS REQUEST some value\"" +
                    "}");

            mockMvc.perform(put)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("The request includes an unrecognized parameter 'response_error'. Please update or remove this parameter and retry your request."));
          });

          it("returns errors from the auditing service auditing fails", () -> {
            doReturn(new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR))
                .when(auditLogService).performWithAuditing(isA(AuditRecordBuilder.class), isA(Supplier.class));

            final MockHttpServletRequestBuilder put = put("/api/v1/data")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{" +
                    "  \"type\":\"value\"," +
                    "  \"name\":\"" + secretName + "\"," +
                    "  \"value\":\"some value\"" +
                    "}");

            mockMvc.perform(put)
                .andExpect(status().isInternalServerError());
          });
        });

        it("allows secret with '.' in the name", () -> {
          final String testSecretNameWithDot = "test.response";

          mockMvc.perform(put("/api/v1/data")
              .content("{\"type\":\"value\",\"name\":\"" + testSecretNameWithDot + "\",\"value\":\"" + "def" + "\"}")
              .contentType(MediaType.APPLICATION_JSON_UTF8))
              .andExpect(status().isOk());
        });
      });

      describe("when name does not have a leading slash", () -> {
        beforeEach(() -> {
          final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                  "  \"type\":\"value\"," +
                  "  \"name\":\"" + StringUtils.stripStart(secretName, "/") + "\"," +
                  "  \"value\":\"" + secretValue + "\"" +
                  "}");

          response = mockMvc.perform(put);
        });

        it("returns the secret as json with a slash added to the name", () -> {
          NamedSecret expected = secretDataService.findMostRecent(secretName);

          response.andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value(secretName))
            .andExpect(jsonPath("$.type").value("value"))
            .andExpect(jsonPath("$.value").value(secretValue))
            .andExpect(jsonPath("$.id").value(expected.getUuid().toString()))
            .andExpect(jsonPath("$.version_created_at").value(expected.getVersionCreatedAt().toString()));
        });
      });

      describe("when another thread wins a race to write a new value", () -> {
        beforeEach(() -> {
          uuid = UUID.randomUUID();

          NamedValueSecret valueSecret = new NamedValueSecret(secretName);
          valueSecret.setEncryptor(encryptor);
          valueSecret.setValue(secretValue);
          valueSecret.setUuid(uuid);
          valueSecret.setVersionCreatedAt(frozenTime);

          doReturn(null)
              .doReturn(valueSecret)
              .when(secretDataService).findMostRecent(anyString());

          doThrow(new DataIntegrityViolationException("we already have one of those"))
              .when(secretDataService).save(any(NamedSecret.class));

          final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                  "  \"type\":\"value\"," +
                  "  \"name\":\"" + secretName + "\"," +
                  "  \"value\":\"" + secretValue + "\"" +
                  "}");

          response = mockMvc.perform(put);
        });

        it("retries and finds the value written by the other thread", () -> {
          verify(secretDataService).save(any(NamedSecret.class));
          response.andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
              .andExpect(jsonPath("$.type").value("value"))
              .andExpect(jsonPath("$.value").value(secretValue))
              .andExpect(jsonPath("$.id").value(uuid.toString()))
              .andExpect(jsonPath("$.version_created_at").value(frozenTime.toString()));
        });
      });

      describe("setting certificates", () -> {
        describe("when required values are passed", () -> {
          beforeEach(() -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                "  \"type\":\"certificate\"," +
                "  \"name\":\"" + secretName + "\"," +
                "  \"value\": {" +
                "    \"ca\": \"-----BEGIN CERTIFICATE-----...-----END CERTIFICATE-----\"," +
                "    \"certificate\": \"-----BEGIN CERTIFICATE-----...-----END CERTIFICATE-----\"," +
                "    \"private_key\": \"-----BEGIN RSA PRIVATE KEY-----...-----END RSA PRIVATE KEY-----\"" +
                "  }" +
                "}");

            response = mockMvc.perform(put);
          });

          it("returns the secret as json", () -> {
            NamedCertificateSecret expected = (NamedCertificateSecret) secretDataService.findMostRecent(secretName);

            assertThat(expected.getPrivateKey(), equalTo("-----BEGIN RSA PRIVATE KEY-----...-----END RSA PRIVATE KEY-----"));

            response.andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
              .andExpect(jsonPath("$.type").value("certificate"))
              .andExpect(jsonPath("$.value.private_key").value("-----BEGIN RSA PRIVATE KEY-----...-----END RSA PRIVATE KEY-----"))
              .andExpect(jsonPath("$.id").value(expected.getUuid().toString()))
              .andExpect(jsonPath("$.version_created_at").value(expected.getVersionCreatedAt().toString()));
          });
        });

        describe("when all values are empty", () -> {
          it("should return an error with the message 'At least one certificate attribute must be set. Please validate your input and retry your request.'", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                "  \"type\":\"certificate\"," +
                "  \"name\":\"" + secretName + "\"," +
                "  \"value\": {" +
                "    \"certificate\": \"\"" +
                "  }" +
                "}");
            final String errorMessage = "At least one certificate attribute must be set. Please validate your input and retry your request.";
            mockMvc.perform(put)
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.error").value(errorMessage));
          });
        });

        describe("when the value is an empty hash", () -> {
          it("should return an error with the message 'At least one certificate attribute must be set. Please validate your input and retry your request.'", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                "  \"type\":\"certificate\"," +
                "  \"name\":\"" + secretName + "\"," +
                "  \"value\": {}" +
                "}");
            final String errorMessage = "At least one certificate attribute must be set. Please validate your input and retry your request.";
            mockMvc.perform(put)
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.error").value(errorMessage));
          });
        });

        describe("when the value contains unknown keys", () -> {
          it("should return an error", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                "  \"type\":\"certificate\"," +
                "  \"name\":\"" + secretName + "\"," +
                "  \"value\": {" +
                "    \"foo\":\"bar\"" +
                "  }" +
                "}");
            final String errorMessage = "The request includes an unrecognized parameter 'foo'. Please update or remove this parameter and retry your request.";
            mockMvc.perform(put)
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.error").value(errorMessage));
          });
        });
      });

      describe("setting SSH keys", () -> {
        describe("when required values are passed", () -> {
          beforeEach(() -> {
            JSONObject obj = new JSONObject();
            obj.put("public_key", TestConstants.PUBLIC_KEY_OF_LENGTH_4096_WITH_COMMENT);
            obj.put("private_key", TestConstants.PRIVATE_KEY_OF_LENGTH_4096);

            final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                "  \"type\":\"ssh\"," +
                "  \"name\":\"" + secretName + "\"," +
                "  \"value\":" + obj.toString() +
                "}");

            response = mockMvc.perform(put);
          });

          it("returns the secret as json", () -> {
            NamedSshSecret expected = (NamedSshSecret) secretDataService.findMostRecent(secretName);

            assertThat(expected.getPrivateKey(), equalTo(TestConstants.PRIVATE_KEY_OF_LENGTH_4096));

            response.andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
              .andExpect(jsonPath("$.type").value("ssh"))
              .andExpect(jsonPath("$.value.public_key").value(TestConstants.PUBLIC_KEY_OF_LENGTH_4096_WITH_COMMENT))
              .andExpect(jsonPath("$.value.private_key").value(TestConstants.PRIVATE_KEY_OF_LENGTH_4096))
              .andExpect(jsonPath("$.id").value(expected.getUuid().toString()))
              .andExpect(jsonPath("$.version_created_at").value(expected.getVersionCreatedAt().toString()));
          });
        });

        describe("when all values are empty", () -> {
          it("should return an error message", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                "  \"type\":\"ssh\"," +
                "  \"name\":\"" + secretName + "\"," +
                "  \"value\": { \"public_key\":\"\", \"private_key\":\"\" }" +
                "}");
            final String errorMessage = "At least one key value must be set. Please validate your input and retry your request.";
            mockMvc.perform(put)
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.error").value(errorMessage));
          });
        });

        describe("when the value is an empty hash", () -> {
          it("should return an error message", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                "  \"type\":\"certificate\"," +
                "  \"name\":\"" + secretName + "\"," +
                "  \"value\": {}" +
                "}");
            final String errorMessage = "At least one certificate attribute must be set. Please validate your input and retry your request.";
            mockMvc.perform(put)
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.error").value(errorMessage));
          });
        });

        describe("when the value contains unknown keys", () -> {
          it("should return an error", () -> {
            final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                "  \"type\":\"ssh\"," +
                "  \"name\":\"" + secretName + "\"," +
                "  \"value\": {" +
                "    \"foo\":\"bar\"" +
                "  }" +
                "}");
            final String errorMessage = "The request includes an unrecognized parameter 'foo'. Please update or remove this parameter and retry your request.";
            mockMvc.perform(put)
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.error").value(errorMessage));
          });
        });
      });
    });

    describe("updating a secret", () -> {
      beforeEach(() -> {
        putSecretInDatabase(secretName, "original value");
        resetAuditLogMock();
      });

      it("should validate requests", () -> {
        final MockHttpServletRequestBuilder put = put("/api/v1/data")
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .content("{" +
                "  \"type\":\"value\"," +
                "  \"name\":\"" + secretName + "\"," +
                "  \"value\":\"original value\"," +
                "  \"bogus\":\"yargablabla\"" +
                "}");

        final String errorMessage = "The request includes an unrecognized parameter 'bogus'. Please update or remove this parameter and retry your request.";
        mockMvc.perform(put)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(errorMessage));
      });

      it("should return 400 when trying to update a secret with a mismatching type", () -> {
        final MockHttpServletRequestBuilder put = put("/api/v1/data")
            .accept(APPLICATION_JSON)
            .contentType(APPLICATION_JSON)
            .content("{" +
                "  \"type\":\"password\"," +
                "  \"name\":\"" + secretName.toUpperCase() + "\"," +
                "  \"value\":\"my-password\"," +
                "  \"overwrite\":true" +
                "}");
        final String errorMessage = "The credential type cannot be modified. Please delete the credential if you wish to create it with a different type.";
        mockMvc.perform(put)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(errorMessage));
      });

      describe("with the overwrite flag set to true case-insensitively", () -> {
        final String specialValue = "special value";

        beforeEach(() -> {
          fakeTimeSetter.accept(frozenTime.plusSeconds(10).toEpochMilli());

          final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                  "  \"type\":\"value\"," +
                  "  \"name\":\"" + secretName.toUpperCase() + "\"," +
                  "  \"value\":\"" + specialValue + "\"," +
                  "  \"overwrite\":true" +
                  "}");

          response = mockMvc.perform(put);
        });

        it("should return the updated value", () -> {
          ArgumentCaptor<NamedSecret> argumentCaptor = ArgumentCaptor.forClass(NamedSecret.class);

          verify(secretDataService, times(1)).save(argumentCaptor.capture());

          // Because the data service mutates the original entity, the UUID should be set
          // on the original object during the save.
          UUID originalUuid = uuid;
          UUID expectedUuid = argumentCaptor.getValue().getUuid();

          response
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.value").value(specialValue))
              .andExpect(jsonPath("$.id").value(expectedUuid.toString()))
              .andExpect(jsonPath("$.name").value(secretName))
              .andExpect(jsonPath("$.version_created_at").value(frozenTime.plusSeconds(10).toString()));

          assertNotNull(expectedUuid);
          assertThat(expectedUuid, not(equalTo(originalUuid)));
        });

        it("should retain the previous value at the previous id", () -> {
          mockMvc.perform(get("/api/v1/data/" + uuid.toString()))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.value").value("original value"))
              .andExpect(jsonPath("$.version_created_at").value(frozenTime.toString()));
        });

        it("persists an audit entry", () -> {
          ArgumentCaptor<AuditRecordBuilder> auditRecordParamsCaptor = ArgumentCaptor.forClass(AuditRecordBuilder.class);
          verify(auditLogService).performWithAuditing(auditRecordParamsCaptor.capture(), any(Supplier.class));

          assertThat(auditRecordParamsCaptor.getValue().getOperationCode(), equalTo(CREDENTIAL_UPDATE));
        });
      });

      describe("with the overwrite flag set to false", () -> {
        beforeEach(() -> {
          final MockHttpServletRequestBuilder put = put("/api/v1/data")
              .accept(APPLICATION_JSON)
              .contentType(APPLICATION_JSON)
              .content("{" +
                  "  \"type\":\"value\"," +
                  "  \"name\":\"" + secretName + "\"," +
                  "  \"value\":\"special value\"" +
                  "}");

          response = mockMvc.perform(put);
        });

        it("should return the expected response", () -> {
          response.andExpect(status().isOk())
              .andExpect(jsonPath("$.value").value("original value"));
        });

        it("persists an audit entry", () -> {
          ArgumentCaptor<AuditRecordBuilder> auditRecordParamsCaptor = ArgumentCaptor.forClass(AuditRecordBuilder.class);
          verify(auditLogService).performWithAuditing(auditRecordParamsCaptor.capture(), any(Supplier.class));

          assertThat(auditRecordParamsCaptor.getValue().getOperationCode(), equalTo(CREDENTIAL_ACCESS));
        });
      });
    });
  }

  private void putSecretInDatabase(String name, String value) throws Exception {
    final MockHttpServletRequestBuilder put = put("/api/v1/data")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content("{" +
            "  \"type\":\"value\"," +
            "  \"name\":\"" + name + "\"," +
            "  \"value\":\"" + value + "\"" +
            "}");

    response = mockMvc.perform(put);

    uuid = secretDataService.findMostRecent(name).getUuid();
    reset(secretDataService);
  }

  private void resetAuditLogMock() throws Exception {
    reset(auditLogService);
    doAnswer(invocation -> {
      final Supplier action = invocation.getArgumentAt(1, Supplier.class);
      return action.get();
    }).when(auditLogService).performWithAuditing(isA(AuditRecordBuilder.class), isA(Supplier.class));
  }
}
