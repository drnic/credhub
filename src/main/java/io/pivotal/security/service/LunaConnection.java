package io.pivotal.security.service;

import io.pivotal.security.config.LunaProviderProperties;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.KeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "encryption.provider", havingValue = "hsm", matchIfMissing = true)
class LunaConnection implements RemoteEncryptionConnectable, KeyGeneratingConnection {

  private final LunaProviderProperties lunaProviderProperties;
  private Provider provider;
  private Object lunaSlotManager;
  private KeyStore keyStore;
  private SecureRandom secureRandom;
  private KeyGenerator aesKeyGenerator;
  private ReentrantReadWriteLock readWriteLock;

  @Autowired
  public LunaConnection(LunaProviderProperties lunaProviderProperties) throws Exception {
    this.lunaProviderProperties = lunaProviderProperties;
    provider = (Provider) Class.forName("com.safenetinc.luna.provider.LunaProvider").newInstance();
    Security.addProvider(provider);
    lunaSlotManager = Class.forName("com.safenetinc.luna.LunaSlotManager")
        .getDeclaredMethod("getInstance").invoke(null);

    secureRandom = SecureRandom.getInstance("LunaRNG");
    aesKeyGenerator = KeyGenerator.getInstance("AES", provider);
    aesKeyGenerator.init(128);

    readWriteLock = new ReentrantReadWriteLock();
  }

  @Override
  public synchronized void reconnect(Exception reasonForReconnect) {
    if (!isLoggedIn()) {
      try {
        reinitialize();
        login(lunaProviderProperties.getPartitionName(),
            lunaProviderProperties.getPartitionPassword());
        makeKeyStore();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public Provider getProvider() {
    return provider;
  }

  SecureRandom getSecureRandom() {
    return secureRandom;
  }

  public KeyGenerator getKeyGenerator() {
    return aesKeyGenerator;
  }

  public KeyStore getKeyStore() {
    return keyStore;
  }

  private void makeKeyStore() throws Exception {
    keyStore = KeyStore.getInstance("Luna", provider);
    keyStore.load(null, null);
  }

  private void login(String partitionName, String partitionPassword) {
    try {
      lunaSlotManager.getClass().getMethod("login", String.class, String.class)
          .invoke(lunaSlotManager, partitionName, partitionPassword);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void reinitialize() {
    try {
      lunaSlotManager.getClass().getMethod("reinitialize").invoke(lunaSlotManager);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Boolean isLoggedIn() {
    try {
      return (Boolean) lunaSlotManager.getClass().getMethod("isLoggedIn").invoke(lunaSlotManager);
    } catch (IllegalAccessException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      return false;
    }
  }
}
