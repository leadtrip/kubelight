package wood.mike.model;

public record NodeStatus(
        String nodeName,
        String status,
        Long lastHeartbeat,
        String version
) {}