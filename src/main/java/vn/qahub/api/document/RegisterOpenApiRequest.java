package vn.qahub.api.document;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterOpenApiRequest {

    @NotBlank
    private String url;
}
