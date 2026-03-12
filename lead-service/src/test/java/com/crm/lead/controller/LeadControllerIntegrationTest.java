package com.crm.lead.controller;

import com.crm.common.security.JwtTokenProvider;
import com.crm.lead.dto.CreateLeadRequest;
import com.crm.lead.entity.Lead;
import com.crm.lead.repository.LeadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        jwtToken = jwtTokenProvider.generateToken(
                UUID.randomUUID().toString(),
                "test-tenant",
                "admin@test.com",
                List.of("ADMIN")
        );
    }

    @Test
    @DisplayName("Should create lead via REST API")
    void shouldCreateLead() throws Exception {
        CreateLeadRequest request = CreateLeadRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .company("Acme Corp")
                .source(Lead.LeadSource.WEB)
                .build();

        mockMvc.perform(post("/api/v1/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Tenant-ID", "test-tenant")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("Should return 401 without JWT token")
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/leads"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() throws Exception {
        CreateLeadRequest request = CreateLeadRequest.builder().build();

        mockMvc.perform(post("/api/v1/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Tenant-ID", "test-tenant")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
