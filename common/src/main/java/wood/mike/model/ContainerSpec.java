package wood.mike.model;

public record ContainerSpec(
        String name,
        String image,
        int hostPort,
        int containerPort
) {}
