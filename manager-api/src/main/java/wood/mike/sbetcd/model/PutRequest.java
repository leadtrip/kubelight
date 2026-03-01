package wood.mike.sbetcd.model;

import wood.mike.ContainerSpec;

public record PutRequest(String key, ContainerSpec value) {
}
