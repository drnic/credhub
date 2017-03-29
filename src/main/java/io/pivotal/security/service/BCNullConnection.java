package io.pivotal.security.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
@ConditionalOnProperty(value = "encryption.provider", havingValue = "internal")
public class BcNullConnection implements RemoteEncryptionConnectable {

  @Override
  public void reconnect(Exception reasonForReconnect) throws Exception {
    if (reasonForReconnect == null) {
      return;
    }
    throw reasonForReconnect;
  }
}
