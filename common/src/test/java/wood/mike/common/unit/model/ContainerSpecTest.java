package wood.mike.common.unit.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wood.mike.model.ContainerSpec;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ContainerSpecTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldFailWhenNameIsTooShort() {
        ContainerSpec spec = new ContainerSpec("a", "nginx:latest", 100, 802911);
        var violations = validator.validate(spec);

        List<String> allViolations = List.of(
                "Name must be between 2 and 63 characters",
                "must be greater than or equal to 1024",
                "must be less than or equal to 65535");

        List<String> v = violations.stream()
                .map(ConstraintViolation::getMessage)
                .toList();

        assertEquals(allViolations, v);
    }

    @Test
    void shouldPassWhenSpecIsValid() {
        ContainerSpec spec = new ContainerSpec("web-server", "nginx:latest", 8080, 80);
        var violations = validator.validate(spec);

        assertThat(violations).isEmpty();
    }
}
