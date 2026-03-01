package wood.mike.sbetcd.integration.controller;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class EtcdTimeoutIT {

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void setManualProperties(DynamicPropertyRegistry registry) {
        // A non-routable IP that will cause the timeout to trigger
        registry.add("app.etcd.endpoints", () -> "http://10.255.255.1:80");
        registry.add("app.etcd.timeout", () -> 1);
    }

    @Test
    public void testPutTimeout() throws Exception {
        String jsonRequest = "{\"key\": \"timeout-test\", \"value\": \"fail\"}";

        mockMvc.perform(post("/put")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Failed to put")));
    }
}