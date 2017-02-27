package io.pivotal.security.request;


import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.List;

@JsonAutoDetect
@Validated
public class AccessControlEntry {

    @NotNull
    private String actor;
    @NotNull
    private List<String> operations;

    public AccessControlEntry() {
    }

    public AccessControlEntry(String actor, List<String> operations) {
        this.actor = actor;
        this.operations = operations;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public List<String> getOperations() {
        return operations;
    }

    public void setOperations(List<String> operations) {
        this.operations = operations;
    }

}
