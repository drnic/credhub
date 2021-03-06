package io.pivotal.security.generator;

import io.pivotal.security.credential.RsaKey;
import io.pivotal.security.request.RsaGenerationParameters;
import io.pivotal.security.util.CertificateFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.KeyPair;

@Component
public class RsaGenerator implements CredentialGenerator<RsaGenerationParameters, RsaKey> {

  private final LibcryptoRsaKeyPairGenerator keyGenerator;

  @Autowired
  RsaGenerator(LibcryptoRsaKeyPairGenerator keyGenerator) {
    this.keyGenerator = keyGenerator;
  }

  @Override
  public RsaKey generateCredential(RsaGenerationParameters parameters) {
    try {
      final KeyPair keyPair = keyGenerator.generateKeyPair(parameters.getKeyLength());
      return new RsaKey(CertificateFormatter.pemOf(keyPair.getPublic()),
          CertificateFormatter.pemOf(keyPair.getPrivate()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
