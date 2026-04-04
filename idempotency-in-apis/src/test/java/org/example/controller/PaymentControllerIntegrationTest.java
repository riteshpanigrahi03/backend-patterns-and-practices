package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.PaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "app.payment.processing-delay-ms=0")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreatePaymentForNewIdempotencyKey() throws Exception {
        PaymentRequest request = new PaymentRequest("ORD-101", BigDecimal.valueOf(500));

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "abc-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentRef").value("PAY-1001"))
                .andExpect(jsonPath("$.orderId").value("ORD-101"))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void shouldReturnExistingPaymentForSameKeyAndSamePayload() throws Exception {
        PaymentRequest request = new PaymentRequest("ORD-202", BigDecimal.valueOf(700));

        mockMvc.perform(post("/payments")
                .header("Idempotency-Key", "retry-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "retry-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentRef").value("PAY-1001"))
                .andExpect(jsonPath("$.orderId").value("ORD-202"))
                .andExpect(jsonPath("$.amount").value(700))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void shouldRejectSameKeyWithDifferentPayload() throws Exception {
        PaymentRequest originalRequest = new PaymentRequest("ORD-303", BigDecimal.valueOf(900));
        PaymentRequest changedRequest = new PaymentRequest("ORD-303", BigDecimal.valueOf(950));

        mockMvc.perform(post("/payments")
                .header("Idempotency-Key", "dup-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalRequest)));

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "dup-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changedRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Idempotency key already used with different request payload"));
    }
}
