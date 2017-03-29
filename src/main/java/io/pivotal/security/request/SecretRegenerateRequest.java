package io.pivotal.security.request;

import static io.pivotal.security.request.BaseSecretRequest.STARTS_WITH_SLASH_AND_AT_LEAST_ONE_NONSLASH_AND_HAS_NO_DOUBLE_SLASHES;

import javax.validation.constraints.Pattern;
import org.hibernate.validator.constraints.NotEmpty;

public class SecretRegenerateRequest {

  @NotEmpty(message = "error.missing_name")
  @Pattern(regexp = STARTS_WITH_SLASH_AND_AT_LEAST_ONE_NONSLASH_AND_HAS_NO_DOUBLE_SLASHES,
      message = "error.invalid_name_has_slash")
  private String name;
  private boolean regenerate;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void validate() {
    // Fill me out when switching to using this object.
  }

  public void setRegenerate(boolean regenerate) {
    this.regenerate = regenerate;
  }
}
