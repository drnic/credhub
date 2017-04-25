package io.pivotal.security.data;

import io.pivotal.security.entity.CredentialName;
import io.pivotal.security.entity.ValueCredentialData;
import io.pivotal.security.exceptions.EntryNotFoundException;
import io.pivotal.security.repository.AccessEntryRepository;
import io.pivotal.security.repository.CredentialNameRepository;
import io.pivotal.security.request.AccessControlEntry;
import io.pivotal.security.request.AccessControlOperation;
import io.pivotal.security.util.DatabaseProfileResolver;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(SpringRunner.class)
@ActiveProfiles(value = "unit-test", resolver = DatabaseProfileResolver.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class AccessControlDataServiceTest {

  private AccessControlDataService subject;

  @Autowired
  AccessEntryRepository accessEntryRepository;

  @Autowired
  CredentialNameRepository credentialNameRepository;

  @Autowired
  TestEntityManager testEntityManager;

  private List<AccessControlEntry> aces;
  private CredentialName credentialName;

  @Before
  public void beforeEach() {
    subject = new AccessControlDataService(
        accessEntryRepository,
        credentialNameRepository
    );

    seedDatabase();
  }

  @Test
  public void getAccessControlList_givenExistingCredentialName_returnsAcl() {
    List<AccessControlEntry> accessControlEntries = subject.getAccessControlList(credentialName);

    assertThat(accessControlEntries, hasSize(3));

    assertThat(accessControlEntries, containsInAnyOrder(
        allOf(hasProperty("actor", equalTo("Luke")),
            hasProperty("allowedOperations", hasItems(AccessControlOperation.WRITE))),
        allOf(hasProperty("actor", equalTo("Leia")),
            hasProperty("allowedOperations", hasItems(AccessControlOperation.READ))),
        allOf(hasProperty("actor", equalTo("HanSolo")),
            hasProperty("allowedOperations",
                hasItems(AccessControlOperation.READ_ACL))))
    );
  }

  @Test
  public void getAccessControlList_whenGivenNonExistentCredentialName_throwsException() {
    try {
      subject.getAccessControlList(new CredentialName("/unicorn"));
    } catch (EntryNotFoundException enfe) {
      assertThat(enfe.getMessage(), Matchers.equalTo("error.resource_not_found"));
    }
  }

  @Test
  public void setAccessControlEntries_whenGivenAnExistingAce_returnsTheAcl() {
    aces = singletonList(
        new AccessControlEntry("Luke", singletonList(AccessControlOperation.READ))
    );

    List<AccessControlEntry> response = subject.setAccessControlEntries("/lightsaber", aces);

    assertThat(response, containsInAnyOrder(
        allOf(hasProperty("actor", equalTo("Luke")),
            hasProperty("allowedOperations",
                hasItems(AccessControlOperation.READ, AccessControlOperation.WRITE))),
        allOf(hasProperty("actor", equalTo("Leia")),
            hasProperty("allowedOperations", hasItems(AccessControlOperation.READ))),
        allOf(hasProperty("actor", equalTo("HanSolo")),
            hasProperty("allowedOperations",
                hasItems(AccessControlOperation.READ_ACL)))));
  }

  @Test
  public void setAccessControlEntries_whenGivenANewAce_returnsTheAcl() {
    final ValueCredentialData valueCredentialData2 = new ValueCredentialData("lightsaber2");
    final CredentialName credentialName2 = valueCredentialData2.getCredentialName();

    credentialNameRepository.saveAndFlush(credentialName2);
    aces = singletonList(
        new AccessControlEntry("Luke", singletonList(AccessControlOperation.READ)));

    List<AccessControlEntry> response = subject.setAccessControlEntries("lightsaber2", aces);

    final AccessControlEntry accessControlEntry = response.get(0);

    assertThat(response, hasSize(1));
    assertThat(accessControlEntry.getActor(), equalTo("Luke"));
    assertThat(accessControlEntry.getAllowedOperations(), hasSize(1));
    assertThat(accessControlEntry.getAllowedOperations(), hasItem(AccessControlOperation.READ));
  }

  @Test
  public void deleteAccessControlEntry_whenGivenExistingCredentialAndActor_deletesTheAcl() {

    subject.deleteAccessControlEntries("/lightsaber", "Luke");

    testEntityManager.flush();

    final List<AccessControlEntry> accessControlList = subject
        .getAccessControlList(credentialName);

    assertThat(accessControlList, hasSize(2));

    assertThat(accessControlList,
        not(contains(hasProperty("actor", equalTo("Luke")))));
  }

  @Test
  public void deleteAccessControlEntry_whenNonExistentResource_throwsException() {
    try {
      subject.deleteAccessControlEntries("/some-thing-that-is-not-here", "Luke");
    } catch (EntryNotFoundException enfe) {
      assertThat(enfe.getMessage(), Matchers.equalTo("error.resource_not_found"));
    }
  }

  @Test
  public void deleteAccessControlEntry_whenNonExistentAce_doesNothing() {
    subject.deleteAccessControlEntries("/lightsaber", "HelloKitty");
  }

  @Test
  public void hasAclReadPermission_whenActorHasAclRead_returnsTrue() {
    assertThat(subject.hasReadAclPermission("HanSolo", credentialName),
        is(true));
  }

  @Test
  public void hasAclReadPermission_whenActorHasReadButNotReadAcl_returnsFalse() {
    assertThat(subject.hasReadAclPermission("Luke", credentialName),
        is(false));
  }

  @Test
  public void hasAclReadPermission_whenActorHasNoPermissions_returnsFalse() {
    assertThat(subject.hasReadAclPermission("Chewie", credentialName),
        is(false));
  }

  @Test
  public void hasAclReadPermission_whenCredentialDoesNotExist_returnsFalse() {
    assertThat(subject.hasReadAclPermission("Luke", new CredentialName("/crossbow")),
        is(false));
  }

  @Test
  public void hasReadPermission_whenActorHasRead_returnsTrue() {
    assertThat(subject.hasReadPermission("Leia", "/lightsaber"),
        is(true));
  }

  @Test
  public void hasReadPermission_whenActorHasRead_returnsTrueRegardlessOfCredentialNameCase() {
    assertThat(subject.hasReadPermission("Leia", "/LIGHTSABER"),
        is(true));
  }

  @Test
  public void hasReadPermission_whenActorHasWriteButNotRead_returnsFalse() {
    assertThat(subject.hasReadPermission("Luke", "/lightsaber"),
        is(false));
  }

  @Test
  public void hasReadPermission_whenActorHasNoPermissions_returnsFalse() {
    assertThat(subject.hasReadPermission("Chewie", "/lightsaber"),
        is(false));
  }

  @Test
  public void hasReadPermission_whenCredentialDoesNotExist_returnsFalse() {
    assertThat(subject.hasReadPermission("Luke", "/crossbow"),
        is(false));
  }

  private void seedDatabase() {
    final ValueCredentialData valueCredentialData = new ValueCredentialData("lightsaber");
    credentialName = valueCredentialData.getCredentialName();

    credentialName = credentialNameRepository.saveAndFlush(credentialName);

    subject.setAccessControlEntries(
        "lightsaber",
        singletonList(new AccessControlEntry("Luke",
            singletonList(AccessControlOperation.WRITE)))
    );

    subject.setAccessControlEntries(
        "lightsaber",
        singletonList(new AccessControlEntry("Leia",
            singletonList(AccessControlOperation.READ)))
    );

    subject.setAccessControlEntries(
        "lightsaber",
        singletonList(new AccessControlEntry("HanSolo",
            singletonList(AccessControlOperation.READ_ACL)))
    );
  }
}
