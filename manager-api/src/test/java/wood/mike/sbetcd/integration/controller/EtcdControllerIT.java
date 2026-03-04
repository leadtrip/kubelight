package wood.mike.sbetcd.integration.controller;

import io.etcd.jetcd.test.EtcdClusterExtension;
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
import wood.mike.sbetcd.model.KlPutRequest;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

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

    @DynamicPropertySource
    static void setEtcdProperties(DynamicPropertyRegistry registry) {
        registry.add("app.etcd.endpoints",
                () -> cluster.clientEndpoints().getFirst().toString());
        registry.add("app.etcd.timeout", () -> 5);
    }

    @Test
    public void testPutAndGetFlow() throws Exception {
        ContainerSpec containerSpec = new ContainerSpec("nginx", "nginx:latest", 8080, 80);
        KlPutRequest request = new KlPutRequest( containerSpec);

        mockMvc.perform(post("/api/put")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("message", is("success")));

        mockMvc.perform(get
                        ("/api/get?key=nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value.name", is("nginx")))
                .andExpect(jsonPath("$.message", is("success")));

        mockMvc.perform(get("/api/delete?key=nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("success")));

        mockMvc.perform(get
                        ("/api/get?key=nginx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value", is(nullValue())))
                .andExpect(jsonPath("$.message", is("success")));
    }
}
