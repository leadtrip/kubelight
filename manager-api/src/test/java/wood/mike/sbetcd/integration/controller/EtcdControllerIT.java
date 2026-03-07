package wood.mike.sbetcd.integration.controller;

import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import wood.mike.model.ContainerSpec;
import wood.mike.model.NodeStatus;
import wood.mike.service.EtcdService;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
public class EtcdControllerIT {

    @RegisterExtension
    public static final io.etcd.jetcd.test.EtcdClusterExtension cluster = EtcdClusterExtension.builder()
            .withNodes(1)
            .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EtcdService etcdService;

    @DynamicPropertySource
    static void setEtcdProperties(DynamicPropertyRegistry registry) {
        registry.add("app.etcd.endpoints",
                () -> cluster.clientEndpoints().getFirst().toString());
        registry.add("app.etcd.timeout", () -> 5);
        registry.add("container-prefix", () -> "/registry/containers/specs/");
        registry.add("container-status-prefix", () -> "/registry/containers/status/");
        registry.add("node-status-prefix", () -> "/registry/nodes/status/");
    }

    @BeforeEach
    public void setup() {
        etcdService.put("/registry/nodes/status/", new NodeStatus("node-1", "running", System.currentTimeMillis(), "1.0.0"));
    }

    @Test
    public void testPutAndGetFlow() throws Exception {
        ContainerSpec containerSpec = new ContainerSpec("nginx", "nginx:latest", 8080, 80);

        mockMvc.perform(post("/api/put")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(containerSpec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("message", is("success")));

        mockMvc.perform(get
                        ("/api/get?key=/registry/containers/specs/node-1/nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value.name", is("nginx")))
                .andExpect(jsonPath("$.message", is("success")));

        mockMvc.perform(get("/api/delete?key=/registry/containers/specs/node-1/nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("success")));

        mockMvc.perform(get
                        ("/api/get?key=/registry/containers/specs/node-1/nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value", is(nullValue())))
                .andExpect(jsonPath("$.message", is("success")));
    }
}
