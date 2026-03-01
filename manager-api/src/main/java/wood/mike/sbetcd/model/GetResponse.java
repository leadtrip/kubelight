package wood.mike.sbetcd.model;

import wood.mike.ContainerSpec;

public record GetResponse (String message, ContainerSpec value) {
    public static GetResponse success(ContainerSpec value) { return new GetResponse("success", value); }
}
