package io.pivotal.security.view;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.helper.JsonHelper.serializeToString;
import static io.pivotal.security.helper.SpectrumHelper.json;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.domain.Encryptor;
import io.pivotal.security.domain.NamedJsonSecret;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.runner.RunWith;

@RunWith(Spectrum.class)
public class JsonViewTest {

  private NamedJsonSecret entity;
  private UUID uuid;
  private Encryptor encryptor;
  private Map<String, Object> value;
  private String serializedValue;

  {
    beforeEach(() -> {
      value = new HashMap<>();
      value.put("string", "something");
      value.put("num", 10);
      value.put("camelCase", "blabla");

      serializedValue = serializeToString(value);

      encryptor = mock(Encryptor.class);
      uuid = UUID.randomUUID();
      entity = new NamedJsonSecret("/foo")
          .setEncryptor(encryptor)
          .setUuid(uuid);
      entity.setEncryptedValue("fake-encrypted-value".getBytes());
      entity.setNonce("fake-nonce".getBytes());

      when(encryptor.decrypt(any(UUID.class), any(byte[].class), any(byte[].class)))
          .thenReturn(serializedValue);
    });

    it("can create view from entity", () -> {
      JsonView actual = (JsonView) JsonView.fromEntity(entity);
      assertThat(json(actual), equalTo("{"
          + "\"type\":\"json\","
          + "\"version_created_at\":null,"
          + "\"id\":\"" + uuid.toString() + "\","
          + "\"name\":\"/foo\","
          + "\"value\":" + serializedValue
          + "}"));
    });

    it("has version_created_at in the view", () -> {
      Instant now = Instant.now();
      entity.setVersionCreatedAt(now);

      JsonView actual = (JsonView) JsonView.fromEntity(entity);

      assertThat(actual.getVersionCreatedAt(), equalTo(now));
    });

    it("has type in the view", () -> {
      JsonView actual = (JsonView) JsonView.fromEntity(entity);

      assertThat(actual.getType(), equalTo("json"));
    });

    it("has a uuid in the view", () -> {
      JsonView actual = (JsonView) JsonView.fromEntity(entity);

      assertThat(actual.getUuid(), equalTo(uuid.toString()));
    });
  }
}
