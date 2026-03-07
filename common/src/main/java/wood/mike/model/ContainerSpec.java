package wood.mike.model;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record ContainerSpec(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 63, message = "Name must be between 2 and 63 characters")
        @Pattern(regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$", message = "Name must be DNS-1123 compliant")
        String name,

        @NotBlank(message = "Image is required")
        String image,

        @Min(1024) @Max(65535)
        int hostPort,

        @Min(1) @Max(65535)
        int containerPort
) {}
