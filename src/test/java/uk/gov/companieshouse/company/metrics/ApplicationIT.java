package uk.gov.companieshouse.company.metrics;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.logging.util.LogContextProperties.REQUEST_ID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldStartApplication() {
        Executable executable = () -> CompanyMetricsConsumerApplication.main(new String[0]);
        assertDoesNotThrow(executable);
    }

    @Test
    void shouldReturn200FromGetHealthEndpoint() throws Exception {
        this.mockMvc.perform(get("/company-appointments-consumer/healthcheck")
                        .header(REQUEST_ID.value(), "request_id"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\":\"UP\"}"));
    }
}
