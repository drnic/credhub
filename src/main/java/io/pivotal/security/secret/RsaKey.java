package io.pivotal.security.secret;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RsaKey implements Secret {

  private final String publicKey;
  private final String privateKey;

  public RsaKey(String publicKey, String privateKey) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }

  @JsonProperty("public_key")
  public String getPublicKey() {
    return publicKey;
  }

  @JsonProperty("private_key")
  public String getPrivateKey() {
    return privateKey;
  }
}
