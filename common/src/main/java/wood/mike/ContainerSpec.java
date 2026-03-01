package wood.mike;

public record ContainerSpec(
        String name,
        String image,
        int hostPort,
        int containerPort
) {}
