package wood.mike.sbetcd.model;

import wood.mike.model.ContainerSpec;

public record PutRequest(String key, ContainerSpec value) {
}
