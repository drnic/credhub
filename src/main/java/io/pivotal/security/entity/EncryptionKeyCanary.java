package io.pivotal.security.entity;

import io.pivotal.security.util.DefensiveCopier;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.pivotal.security.constants.EncryptionConstants.ENCRYPTED_BYTES;
import static io.pivotal.security.constants.EncryptionConstants.NONCE_SIZE;
import static io.pivotal.security.constants.EncryptionConstants.SALT_SIZE;
import static io.pivotal.security.constants.UuidConstants.UUID_BYTES;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.ArrayUtils.toObject;

@Entity
@Table(name = "EncryptionKeyCanary")
public class EncryptionKeyCanary implements EncryptedValueContainer {

  // Use VARBINARY to make all 3 DB types happy.
  // H2 doesn't distinguish between "binary" and "varbinary" - see
  // https://hibernate.atlassian.net/browse/HHH-9835 and
  // https://github.com/h2database/h2database/issues/345
  @Id
  @Column(length = UUID_BYTES, columnDefinition = "VARBINARY")
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  private UUID uuid;

  @Column(length = ENCRYPTED_BYTES + NONCE_SIZE, name = "encrypted_value")
  private byte[] encryptedValue;

  @Column(length = NONCE_SIZE)
  private byte[] nonce;

  @Column(length = SALT_SIZE)
  private byte[] salt;

  @Transient
  protected final static DefensiveCopier DEFENSIVE_COPIER = new DefensiveCopier();

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  @Override
  public byte[] getEncryptedValue() {
    return Arrays.copyOf(encryptedValue, encryptedValue.length);
  }

  @Override
  public void setEncryptedValue(byte[] encryptedValue) {
    this.encryptedValue = DEFENSIVE_COPIER.copyByteArray(encryptedValue);
  }

  @Override
  public byte[] getNonce() {
    return Arrays.copyOf(nonce, nonce.length);
  }

  @Override
  public void setNonce(byte[] nonce) {
    this.nonce = DEFENSIVE_COPIER.copyByteArray(nonce);
  }

  @Override
  public UUID getEncryptionKeyUuid() {
    return uuid;
  }

  @Override
  public void setEncryptionKeyUuid(UUID encryptionKeyUuid) {
    setUuid(encryptionKeyUuid);
  }

  public List<Byte> getSalt() {
    return salt == null ? null : unmodifiableList(asList(toObject(salt)));
  }

  public void setSalt(byte[] salt) {
    this.salt = Arrays.copyOf(salt, salt.length);
  }
}
