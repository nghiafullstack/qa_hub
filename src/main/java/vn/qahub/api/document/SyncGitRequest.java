package vn.qahub.api.document;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SyncGitRequest {

    private String branch = "developer";

    @NotEmpty
    private List<String> globs;
}
