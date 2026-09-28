package vn.qahub.api.analysis;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateFindingStatusRequest {

    @NotNull
    private FindingStatus status;
}
