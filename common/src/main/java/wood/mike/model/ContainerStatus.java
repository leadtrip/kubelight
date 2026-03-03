package wood.mike.model;

public record ContainerStatus(
        String name,
        String state,
        String imageId,
        String uptime,
        String lastReported
) {}
