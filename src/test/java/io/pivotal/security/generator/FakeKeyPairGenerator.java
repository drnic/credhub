package io.pivotal.security.generator;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

@Component
public class FakeKeyPairGenerator {

  private static final int KEY_LENGTH_FOR_TESTING = 1024;

  public KeyPair generate() throws NoSuchProviderException, NoSuchAlgorithmException {
    KeyPairGenerator generator = KeyPairGenerator
        .getInstance("Rsa", BouncyCastleProvider.PROVIDER_NAME);
    generator.initialize(KEY_LENGTH_FOR_TESTING);
    return generator.generateKeyPair();
  }
}
