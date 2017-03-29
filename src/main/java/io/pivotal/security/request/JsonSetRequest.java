package io.pivotal.security.request;

import io.pivotal.security.domain.Encryptor;
import io.pivotal.security.domain.NamedJsonSecret;
import io.pivotal.security.domain.NamedSecret;
import java.util.Map;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.context.ApplicationContext;

public class JsonSetRequest extends BaseSecretSetRequest {

  @NotEmpty(message = "error.missing_value")
  private Map<String, Object> value;

  public Map<String, Object> getValue() {
    return value;
  }

  public void setValue(Map<String, Object> value) {
    this.value = value;
  }

  @Override
  public NamedSecret createNewVersion(NamedSecret existing, Encryptor encryptor,
      ApplicationContext applicationContext) {
    return NamedJsonSecret
        .createNewVersion((NamedJsonSecret) existing, getName(), this.getValue(), encryptor,
            this.getAccessControlEntries());
  }
}
