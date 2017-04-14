package io.pivotal.security.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;

import io.pivotal.security.entity.EncryptionKeyCanary;
import io.pivotal.security.repository.EncryptionKeyCanaryRepository;
import io.pivotal.security.util.DatabaseProfileResolver;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles(value = {"unit-test"}, resolver = DatabaseProfileResolver.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class EncryptionKeyCanaryDataServiceTest {

  @Autowired
  EncryptionKeyCanaryRepository encryptionKeyCanaryRepository;

  EncryptionKeyCanaryDataService subject;

  @Before
  public void beforeEach() {
    subject = new EncryptionKeyCanaryDataService(encryptionKeyCanaryRepository);
  }

  @Test
  public void save_savesTheEncryptionCanary() {
    EncryptionKeyCanary encryptionKeyCanary = new EncryptionKeyCanary();
    encryptionKeyCanary.setNonce("test-nonce".getBytes());
    encryptionKeyCanary.setEncryptedValue("test-value".getBytes());
    subject.save(encryptionKeyCanary);

    List<EncryptionKeyCanary> canaries = subject.findAll();

    assertThat(canaries, hasSize(1));

    EncryptionKeyCanary actual = canaries.get(0);

    assertNotNull(actual.getUuid());
    assertThat(actual.getUuid(), equalTo(encryptionKeyCanary.getUuid()));
    assertThat(actual.getNonce(), equalTo("test-nonce".getBytes()));
    assertThat(actual.getEncryptedValue(), equalTo("test-value".getBytes()));
  }

  @Test
  public void findAll_whenThereAreNoCanaries_returnsEmptyList() {
    assertThat(subject.findAll(), hasSize(0));
  }

  @Test
  public void findAll_whenThereAreCanaries_returnsCanariesAsAList() {

    EncryptionKeyCanary firstCanary = new EncryptionKeyCanary();
    EncryptionKeyCanary secondCanary = new EncryptionKeyCanary();

    subject.save(firstCanary);
    subject.save(secondCanary);

    List<EncryptionKeyCanary> canaries = subject.findAll();
    List<UUID> uuids = canaries.stream().map(canary -> canary.getUuid())
        .collect(Collectors.toList());

    assertThat(canaries, hasSize(2));
    assertThat(uuids, containsInAnyOrder(firstCanary.getUuid(), secondCanary.getUuid()));
  }

  @Test
  public void delete_whenThereAreCanaries_deletesTheRequestedCanaries() {
    EncryptionKeyCanary firstCanary = new EncryptionKeyCanary();
    EncryptionKeyCanary secondCanary = new EncryptionKeyCanary();

    subject.save(firstCanary);
    subject.save(secondCanary);

    List<EncryptionKeyCanary> canaries = subject.findAll();

    List<UUID> uuids = canaries.stream().map(canary -> canary.getUuid())
        .collect(Collectors.toList());

    assertThat(canaries, hasSize(2));
    assertThat(uuids, containsInAnyOrder(firstCanary.getUuid(), secondCanary.getUuid()));

    subject.delete(secondCanary);

    canaries = subject.findAll();
    uuids = canaries.stream().map(canary -> canary.getUuid())
        .collect(Collectors.toList());

    assertThat(canaries, hasSize(1));
    assertThat(uuids, containsInAnyOrder(firstCanary.getUuid()));
  }

  @Test
  public void delete_whenThereAreNoCanaries_doesNothing() {
    assertThat(subject.findAll(), hasSize(0));

    subject.delete(new EncryptionKeyCanary());

    assertThat(subject.findAll(), hasSize(0));
  }
}
