package io.pivotal.security.repository;


import io.pivotal.security.entity.AccessEntryData;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface AccessEntryRepository extends JpaRepository<AccessEntryData, UUID> {

  List<AccessEntryData> findAllByCredentialNameUuid(UUID name);

  @Transactional
  int deleteByCredentialNameUuidAndActor(UUID secretNameUuid, String actor);
}
