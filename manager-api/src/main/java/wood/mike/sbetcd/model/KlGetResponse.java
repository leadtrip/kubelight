package wood.mike.sbetcd.model;

import wood.mike.model.ContainerSpec;

public record KlGetResponse(String message, ContainerSpec value) {
    public static KlGetResponse success(ContainerSpec value) { return new KlGetResponse("success", value); }
}
