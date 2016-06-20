package io.pivotal.security.generator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.security.*;

@Component
public class BcKeyPairGenerator extends KeyPairGenerator {

  private KeyPairGenerator myGenerator;

  public BcKeyPairGenerator() {
    super("RSA");
    Security.addProvider(new BouncyCastleProvider());
    try {
      myGenerator = KeyPairGenerator.getInstance("RSA", "BC");
      myGenerator.initialize(2048);
    } catch (Exception e) {
      // todo perhaps throw any time we are asked for a new key?
      e.printStackTrace();
    }
  }

  @Override
  public KeyPair generateKeyPair() {
    return myGenerator.generateKeyPair();
  }
}