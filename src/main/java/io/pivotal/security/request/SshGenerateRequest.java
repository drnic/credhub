package io.pivotal.security.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.pivotal.security.credential.SshKey;
import io.pivotal.security.service.GeneratorService;

public class SshGenerateRequest extends BaseCredentialGenerateRequest {

  @JsonProperty("parameters")
  private SshGenerationParameters generationParameters;

  public SshGenerationParameters getGenerationParameters() {
    if (generationParameters == null) {
      generationParameters = new SshGenerationParameters();
    }
    return generationParameters;
  }

  public void setGenerationParameters(SshGenerationParameters generationParameters) {
    this.generationParameters = generationParameters;
  }

  @Override
  public void validate() {
    super.validate();

    getGenerationParameters().validate();
  }

  public BaseCredentialSetRequest generateSetRequest(GeneratorService generatorService) {
    SshSetRequest sshSetRequest = new SshSetRequest();
    SshKey sshKey = generatorService.generateSshKeys(getGenerationParameters());
    sshSetRequest.setSshKeyValue(sshKey);
    sshSetRequest.setType(getType());
    sshSetRequest.setName(getName());
    sshSetRequest.setOverwrite(isOverwrite());
    sshSetRequest.setAccessControlEntries(getAccessControlEntries());

    return sshSetRequest;
  }
}
