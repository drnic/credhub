package io.pivotal.security.domain;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.request.KeySetRequestFields;
import io.pivotal.security.service.Encryption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.runner.RunWith;

@RunWith(Spectrum.class)
public class NamedRsaSecretTest {

  private NamedRsaSecret subject;

  private Encryptor encryptor;

  private UUID canaryUuid;

  {
    beforeEach(() -> {
      canaryUuid = UUID.randomUUID();
      encryptor = mock(Encryptor.class);
      subject = new NamedRsaSecret("/Foo");
    });

    it("returns type rsa", () -> {
      assertThat(subject.getSecretType(), equalTo("rsa"));
    });

    describe("#copyInto", () -> {
      it("should copy the correct properties into the other object", () -> {
        Instant frozenTime = Instant.ofEpochSecond(1400000000L);
        UUID uuid = UUID.randomUUID();
        UUID encryptionKeyUuid = UUID.randomUUID();

        subject = new NamedRsaSecret("/foo");
        subject.setPublicKey("fake-public-key");
        subject.setEncryptedValue("fake-private-key".getBytes());
        subject.setNonce("fake-nonce".getBytes());
        subject.setUuid(uuid);
        subject.setVersionCreatedAt(frozenTime);
        subject.setEncryptionKeyUuid(encryptionKeyUuid);

        NamedRsaSecret copy = new NamedRsaSecret();
        subject.copyInto(copy);

        assertThat(copy.getName(), equalTo("/foo"));
        assertThat(copy.getPublicKey(), equalTo("fake-public-key"));
        assertThat(copy.getEncryptedValue(), equalTo("fake-private-key".getBytes()));
        assertThat(copy.getNonce(), equalTo("fake-nonce".getBytes()));
        assertThat(copy.getEncryptionKeyUuid(), equalTo(encryptionKeyUuid));

        assertThat(copy.getUuid(), not(equalTo(uuid)));
        assertThat(copy.getVersionCreatedAt(), not(equalTo(frozenTime)));
      });
    });

    describe(".createNewVersion", () -> {
      beforeEach(() -> {
        byte[] encryptedValue = "new-fake-encrypted".getBytes();
        byte[] nonce = "new-fake-nonce".getBytes();
        when(encryptor.encrypt("new private key"))
            .thenReturn(new Encryption(canaryUuid, encryptedValue, nonce));
        when(encryptor.decrypt(any(UUID.class), eq(encryptedValue), eq(nonce)))
            .thenReturn("new private key");

        subject = new NamedRsaSecret("/existingName");
        subject.setEncryptor(encryptor);
        subject.setEncryptedValue("old encrypted private key".getBytes());
      });

      it("copies name from existing", () -> {
        KeySetRequestFields fields = new KeySetRequestFields("new private key", "public key");
        NamedRsaSecret newSecret = (NamedRsaSecret) NamedRsaSecret
            .createNewVersion(subject, "anything I AM IGNORED", fields, encryptor,
                new ArrayList<>());

        assertThat(newSecret.getName(), equalTo("/existingName"));
        assertThat(newSecret.getPrivateKey(), equalTo("new private key"));
        assertThat(newSecret.getPublicKey(), equalTo("public key"));
      });

      it("creates new if no existing", () -> {
        KeySetRequestFields fields = new KeySetRequestFields("new private key", "public key");
        NamedRsaSecret newSecret = (NamedRsaSecret) NamedRsaSecret
            .createNewVersion(null, "/newName", fields, encryptor, new ArrayList<>());

        assertThat(newSecret.getName(), equalTo("/newName"));
        assertThat(newSecret.getPrivateKey(), equalTo("new private key"));
        assertThat(newSecret.getPublicKey(), equalTo("public key"));
      });
    });
  }
}
