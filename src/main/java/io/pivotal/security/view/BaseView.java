package io.pivotal.security.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class BaseView {

  private Instant versionCreatedAt;

  BaseView() { /* Jackson */ }

  BaseView(Instant versionCreatedAt) {
    this.versionCreatedAt = versionCreatedAt;
  }

  @JsonProperty("version_created_at")
  public Instant getVersionCreatedAt() {
    return versionCreatedAt;
  }
}
