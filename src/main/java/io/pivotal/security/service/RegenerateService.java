package io.pivotal.security.service;

import static io.pivotal.security.audit.AuditingOperationCode.CREDENTIAL_UPDATE;

import io.pivotal.security.audit.EventAuditRecordParameters;
import io.pivotal.security.data.CredentialDataService;
import io.pivotal.security.domain.Credential;
import io.pivotal.security.domain.PasswordCredential;
import io.pivotal.security.exceptions.EntryNotFoundException;
import io.pivotal.security.request.AccessControlEntry;
import io.pivotal.security.request.CredentialRegenerateRequest;
import io.pivotal.security.service.regeneratables.CertificateCredentialRegeneratable;
import io.pivotal.security.service.regeneratables.NotRegeneratable;
import io.pivotal.security.service.regeneratables.PasswordCredentialRegeneratable;
import io.pivotal.security.service.regeneratables.Regeneratable;
import io.pivotal.security.service.regeneratables.RsaCredentialRegeneratable;
import io.pivotal.security.service.regeneratables.SshCredentialRegeneratable;
import io.pivotal.security.view.CredentialView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class RegenerateService {

  private CredentialDataService credentialDataService;
  private GenerateService generateService;
  private Map<String, Supplier<Regeneratable>> regeneratableTypes;

  RegenerateService(
      CredentialDataService credentialDataService,
      GenerateService generateService
  ) {
    this.credentialDataService = credentialDataService;
    this.generateService = generateService;

    this.regeneratableTypes = new HashMap<>();
    this.regeneratableTypes.put("password", PasswordCredentialRegeneratable::new);
    this.regeneratableTypes.put("ssh", SshCredentialRegeneratable::new);
    this.regeneratableTypes.put("rsa", RsaCredentialRegeneratable::new);
    this.regeneratableTypes.put("certificate", CertificateCredentialRegeneratable::new);
  }

  public CredentialView performRegenerate(
      List<EventAuditRecordParameters> parametersList,
      CredentialRegenerateRequest requestBody,
      AccessControlEntry currentUserAccessControlEntry) {
    Credential credential = credentialDataService.findMostRecent(requestBody.getName());
    if (credential == null) {
      parametersList.add(new EventAuditRecordParameters(CREDENTIAL_UPDATE, requestBody.getName()));
      throw new EntryNotFoundException("error.credential_not_found");
    }

    Regeneratable regeneratable = regeneratableTypes
        .getOrDefault(credential.getCredentialType(), NotRegeneratable::new)
        .get();

    if (credential instanceof PasswordCredential && ((PasswordCredential) credential).getGenerationParameters() == null) {
      parametersList.add(new EventAuditRecordParameters(CREDENTIAL_UPDATE, requestBody.getName()));
    }

    return generateService
        .performGenerate(parametersList, regeneratable.createGenerateRequest(credential), currentUserAccessControlEntry);
  }
}
