package io.pivotal.security.controller.v1;

import io.pivotal.security.exceptions.ParameterizedValidationException;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RsaSshSecretParameters implements RequestParameters {

  private int keyLength = 2048;
  private List<Integer> validKeyLengths = Arrays.asList(2048, 3072, 4096);

  public void validate() {
    if (!validKeyLengths.contains(keyLength)) {
      throw new ParameterizedValidationException("error.invalid_key_length");
    }
  }

  public Integer getKeyLength() {
    return keyLength;
  }

  public void setKeyLength(int keyLength) {
    this.keyLength = keyLength;
  }
}
