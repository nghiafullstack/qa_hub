package vn.qahub.api.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "slug chỉ gồm chữ thường/số, nối bằng dấu -")
    private String slug;

    private String gitRepoUrl;

    private String description;
}
