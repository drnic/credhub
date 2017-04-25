package io.pivotal.security.service;

import io.pivotal.security.auth.UserContext;
import io.pivotal.security.data.AccessControlDataService;
import io.pivotal.security.entity.CredentialName;
import io.pivotal.security.exceptions.PermissionException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

  private AccessControlDataService accessControlDataService;

  @Value("${security.authorization.acls.enabled}")
  private boolean enforcePermissions;

  @Autowired
  public PermissionService(AccessControlDataService accessControlDataService) {
    this.accessControlDataService = accessControlDataService;
  }

  public void verifyAclReadPermission(UserContext user, CredentialName credentialName) {
    if (enforcePermissions) {
      String actor = getActorFromUserContext(user);
      if (StringUtils.isEmpty(actor) || !accessControlDataService.hasReadAclPermission(actor, credentialName)) {
        throw new PermissionException("error.acl.lacks_acl_read");
      }
    }
  }

  public void verifyReadPermission(UserContext user, CredentialName credentialName) {
    if (enforcePermissions) {
      String actor = getActorFromUserContext(user);
      if (StringUtils.isEmpty(actor) || !accessControlDataService.hasReadPermission(actor, credentialName)) {
        throw new PermissionException("error.acl.lacks_read");
      }
    }
  }

  private String getActorFromUserContext(UserContext user) {
    return user.getAclUser();
  }
}
