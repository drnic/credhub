package io.pivotal.security.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.request.PasswordGenerationParameters;
import io.pivotal.security.data.EncryptionKeyCanaryDataService;
import io.pivotal.security.data.SecretDataService;
import io.pivotal.security.domain.Encryptor;
import io.pivotal.security.domain.NamedCertificateSecret;
import io.pivotal.security.domain.NamedPasswordSecret;
import io.pivotal.security.domain.NamedSecret;
import io.pivotal.security.entity.EncryptionKeyCanary;
import io.pivotal.security.service.Encryption;
import io.pivotal.security.service.EncryptionKeyCanaryMapper;
import io.pivotal.security.service.EncryptionKeyRotator;
import io.pivotal.security.service.EncryptionService;
import io.pivotal.security.service.PasswordBasedKeyProxy;
import io.pivotal.security.util.DatabaseProfileResolver;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.security.Key;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static io.pivotal.security.service.EncryptionKeyCanaryMapper.CANARY_VALUE;
import static io.pivotal.security.service.PasswordBasedKeyProxy.generateSalt;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.when;

@ActiveProfiles(value = "unit-test", resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredentialManagerApp.class)
@RunWith(Spectrum.class)
public class EncryptionKeyRotatorTest {
  private NamedCertificateSecret secretWithCurrentKey;
  private NamedSecret secretWithOldKey;
  private NamedCertificateSecret secretWithUnknownKey;
  private NamedPasswordSecret password;
  @SpyBean
  SecretDataService secretDataService;

  @SpyBean
  EncryptionKeyCanaryMapper encryptionKeyCanaryMapper;

  @Autowired
  EncryptionKeyRotator encryptionKeyRotator;

  @Autowired
  EncryptionKeyCanaryDataService encryptionKeyCanaryDataService;

  @Autowired
  EncryptionService encryptionService;

  @Autowired
  Encryptor encryptor;

  private EncryptionKeyCanary unknownCanary;
  private EncryptionKeyCanary oldCanary;

  private String passwordName;

  {
    wireAndUnwire(this);

    describe("when data exists that is encrypted with an unknown key", () -> {
      beforeEach(() -> {
          secretWithCurrentKey = new NamedCertificateSecret("/current-key");
          secretWithCurrentKey
                  .setEncryptor(encryptor)
                  .setCa("my-ca")
                  .setCertificate("my-cert")
                  .setPrivateKey("cert-private-key");

          secretDataService.save(secretWithCurrentKey);
          final PasswordBasedKeyProxy keyProxy = new PasswordBasedKeyProxy("old-password", encryptionService);
          Key oldKey = keyProxy.deriveKey(generateSalt());
          oldCanary = new EncryptionKeyCanary();
          final Encryption canaryEncryption = encryptionService.encrypt(oldKey, CANARY_VALUE);
          oldCanary.setEncryptedValue(canaryEncryption.encryptedValue);
          oldCanary.setNonce(canaryEncryption.nonce);
          oldCanary = encryptionKeyCanaryDataService.save(oldCanary);
          when(encryptionKeyCanaryMapper.getKeyForUuid(oldCanary.getUuid())).thenReturn(oldKey);
          when(encryptionKeyCanaryMapper.getCanaryUuidsWithKnownAndInactiveKeys()).thenReturn(singletonList(oldCanary.getUuid()));

          secretWithOldKey = new NamedCertificateSecret("/old-key");
          final Encryption encryption = encryptionService.encrypt(oldKey, "old-certificate-private-key");
          secretWithOldKey.setEncryptedValue(encryption.encryptedValue);
          secretWithOldKey.setNonce(encryption.nonce);
          secretWithOldKey.setEncryptionKeyUuid(oldCanary.getUuid());
          secretDataService.save(secretWithOldKey);

          unknownCanary = new EncryptionKeyCanary();
          unknownCanary.setEncryptedValue("bad-encrypted-value".getBytes());
          unknownCanary.setNonce("bad-nonce".getBytes());
          unknownCanary = encryptionKeyCanaryDataService.save(unknownCanary);

          secretWithUnknownKey = new NamedCertificateSecret("/unknown-key");
          secretWithUnknownKey
                  .setEncryptor(encryptor)
                  .setPrivateKey("cert-private-key");
          secretWithUnknownKey.setEncryptionKeyUuid(unknownCanary.getUuid());
          secretDataService.save(secretWithUnknownKey);

          passwordName = "/test-password";
          password = new NamedPasswordSecret(passwordName);
          final Encryption secretEncryption = encryptionService.encrypt(oldKey, "test-password-plaintext");
          password.setEncryptedValue(secretEncryption.encryptedValue);
          password.setNonce(secretEncryption.nonce);
          PasswordGenerationParameters parameters = new PasswordGenerationParameters();
          parameters.setExcludeNumber(true);
          final Encryption parameterEncryption = encryptionService.encrypt(oldKey, new ObjectMapper().writeValueAsString(parameters));
          password.setEncryptedGenerationParameters(parameterEncryption.encryptedValue);
          password.setParametersNonce(parameterEncryption.nonce);
          password.setEncryptionKeyUuid(oldCanary.getUuid());

          secretDataService.save(password);

      });

      it("should rotate data that it can decrypt (and it shouldn't loop forever!)", () -> {
        Slice<NamedSecret> beforeRotation = secretDataService.findEncryptedWithAvailableInactiveKey();
        int numberToRotate = beforeRotation.getNumberOfElements();

        encryptionKeyRotator.rotate();

        Slice<NamedSecret> afterRotation = secretDataService.findEncryptedWithAvailableInactiveKey();
        int numberToRotateWhenDone = afterRotation.getNumberOfElements();

        assertThat(numberToRotate, equalTo(2));
        assertThat(numberToRotateWhenDone, equalTo(0));

        List<UUID> uuids = beforeRotation.getContent().stream().map(secret -> secret.getUuid()).collect(Collectors.toList());

        // Gets updated to use current key:
        assertThat(secretDataService.findByUuid(secretWithOldKey.getUuid()).getEncryptionKeyUuid(), equalTo(encryptionKeyCanaryMapper.getActiveUuid()));
        assertThat(uuids, hasItem(secretWithOldKey.getUuid()));

        assertThat(secretDataService.findByUuid(password.getUuid()).getEncryptionKeyUuid(), equalTo(encryptionKeyCanaryMapper.getActiveUuid()));
        assertThat(uuids, hasItem(password.getUuid()));

        // Unchanged because we don't have the key:
        assertThat(secretDataService.findByUuid(secretWithUnknownKey.getUuid()).getEncryptionKeyUuid(), equalTo(unknownCanary.getUuid()));
        assertThat(uuids, not(hasItem(secretWithUnknownKey.getUuid())));

        // Unchanged because it's already up to date:
        assertThat(secretDataService.findByUuid(secretWithCurrentKey.getUuid()).getEncryptionKeyUuid(), equalTo(encryptionKeyCanaryMapper.getActiveUuid()));
        assertThat(uuids, not(hasItem(secretWithCurrentKey.getUuid())));

        NamedPasswordSecret rotatedPassword = (NamedPasswordSecret) secretDataService.findMostRecent(passwordName);
        assertThat(rotatedPassword.getPassword(), equalTo("test-password-plaintext"));
        assertThat(rotatedPassword.getGenerationParameters(), samePropertyValuesAs(
          new PasswordGenerationParameters()
            .setExcludeNumber(true)
            .setLength(23))
        );
      });
    });
  }
}
