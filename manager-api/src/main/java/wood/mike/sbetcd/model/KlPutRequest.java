package wood.mike.sbetcd.model;

import wood.mike.model.ContainerSpec;

public record KlPutRequest(String key, ContainerSpec value) {
}
