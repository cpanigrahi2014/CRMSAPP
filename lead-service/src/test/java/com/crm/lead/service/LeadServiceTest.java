package com.crm.lead.service;

import com.crm.common.dto.PagedResponse;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.BadRequestException;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.security.TenantContext;
import com.crm.lead.dto.*;
import com.crm.lead.entity.Lead;
import com.crm.lead.mapper.LeadMapper;
import com.crm.lead.repository.LeadRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadMapper leadMapper;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private LeadService leadService;

    private static final String TENANT_ID = "test-tenant";
    private static final String USER_ID = "test-user-id";
    private UUID leadId;
    private Lead lead;
    private LeadResponse leadResponse;
    private CreateLeadRequest createRequest;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        leadId = UUID.randomUUID();

        lead = Lead.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .company("Acme Corp")
                .title("CTO")
                .status(Lead.LeadStatus.NEW)
                .source(Lead.LeadSource.WEB)
                .leadScore(0)
                .build();
        lead.setId(leadId);
        lead.setTenantId(TENANT_ID);

        leadResponse = LeadResponse.builder()
                .id(leadId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .company("Acme Corp")
                .title("CTO")
                .status(Lead.LeadStatus.NEW)
                .source(Lead.LeadSource.WEB)
                .leadScore(0)
                .tenantId(TENANT_ID)
                .build();

        createRequest = CreateLeadRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .company("Acme Corp")
                .title("CTO")
                .source(Lead.LeadSource.WEB)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create lead successfully")
    void shouldCreateLead() {
        when(leadMapper.toEntity(any(CreateLeadRequest.class))).thenReturn(lead);
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(leadResponse);

        LeadResponse result = leadService.createLead(createRequest, USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getCompany()).isEqualTo("Acme Corp");
        assertThat(result.getStatus()).isEqualTo(Lead.LeadStatus.NEW);

        verify(leadRepository).save(any(Lead.class));
        verify(eventPublisher).publish(eq("lead-events"), eq(TENANT_ID), eq(USER_ID),
                eq("Lead"), anyString(), eq("LEAD_CREATED"), any());
    }

    @Test
    @DisplayName("Should get lead by ID successfully")
    void shouldGetLeadById() {
        when(leadRepository.findByIdAndTenantIdAndDeletedFalse(leadId, TENANT_ID))
                .thenReturn(Optional.of(lead));
        when(leadMapper.toResponse(lead)).thenReturn(leadResponse);

        LeadResponse result = leadService.getLeadById(leadId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(leadId);
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(leadRepository).findByIdAndTenantIdAndDeletedFalse(leadId, TENANT_ID);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when lead not found")
    void shouldThrowWhenLeadNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(leadRepository.findByIdAndTenantIdAndDeletedFalse(nonExistentId, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.getLeadById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update lead successfully")
    void shouldUpdateLead() {
        UpdateLeadRequest updateRequest = UpdateLeadRequest.builder()
                .firstName("Jane")
                .status(Lead.LeadStatus.CONTACTED)
                .leadScore(75)
                .build();

        when(leadRepository.findByIdAndTenantIdAndDeletedFalse(leadId, TENANT_ID))
                .thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(
                LeadResponse.builder()
                        .id(leadId)
                        .firstName("Jane")
                        .status(Lead.LeadStatus.CONTACTED)
                        .leadScore(75)
                        .build()
        );

        LeadResponse result = leadService.updateLead(leadId, updateRequest, USER_ID);

        assertThat(result).isNotNull();
        verify(leadRepository).save(any(Lead.class));
        verify(eventPublisher).publish(eq("lead-events"), eq(TENANT_ID), eq(USER_ID),
                eq("Lead"), anyString(), eq("LEAD_UPDATED"), any());
    }

    @Test
    @DisplayName("Should get all leads with pagination")
    void shouldGetAllLeadsWithPagination() {
        Page<Lead> leadPage = new PageImpl<>(List.of(lead));
        when(leadRepository.findByTenantIdAndDeletedFalse(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(leadPage);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(leadResponse);

        PagedResponse<LeadResponse> result = leadService.getAllLeads(0, 20, "createdAt", "desc");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should assign lead to user")
    void shouldAssignLead() {
        UUID assigneeId = UUID.randomUUID();

        when(leadRepository.findByIdAndTenantIdAndDeletedFalse(leadId, TENANT_ID))
                .thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(leadResponse);

        LeadResponse result = leadService.assignLead(leadId, assigneeId, USER_ID);

        assertThat(result).isNotNull();
        verify(leadRepository).save(any(Lead.class));
        verify(eventPublisher).publish(eq("lead-events"), eq(TENANT_ID), eq(USER_ID),
                eq("Lead"), anyString(), eq("LEAD_ASSIGNED"), any());
    }

    @Test
    @DisplayName("Should convert lead to opportunity")
    void shouldConvertLead() {
        ConvertLeadRequest convertRequest = ConvertLeadRequest.builder()
                .opportunityName("Acme Deal")
                .build();

        when(leadRepository.findByIdAndTenantIdAndDeletedFalse(leadId, TENANT_ID))
                .thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(leadResponse);

        LeadResponse result = leadService.convertLead(leadId, convertRequest, USER_ID);

        assertThat(result).isNotNull();
        verify(leadRepository).save(any(Lead.class));
        verify(eventPublisher).publish(eq("lead-events"), eq(TENANT_ID), eq(USER_ID),
                eq("Lead"), anyString(), eq("LEAD_CONVERTED"), any());
    }

    @Test
    @DisplayName("Should throw when converting already converted lead")
    void shouldThrowWhenConvertingAlreadyConvertedLead() {
        lead.setConverted(true);
        ConvertLeadRequest convertRequest = ConvertLeadRequest.builder()
                .opportunityName("Acme Deal")
                .build();

        when(leadRepository.findByIdAndTenantIdAndDeletedFalse(leadId, TENANT_ID))
                .thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> leadService.convertLead(leadId, convertRequest, USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already converted");
    }

    @Test
    @DisplayName("Should soft delete lead")
    void shouldSoftDeleteLead() {
        when(leadRepository.findByIdAndTenantIdAndDeletedFalse(leadId, TENANT_ID))
                .thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);

        leadService.deleteLead(leadId, USER_ID);

        verify(leadRepository).save(argThat(savedLead -> savedLead.isDeleted()));
        verify(eventPublisher).publish(eq("lead-events"), eq(TENANT_ID), eq(USER_ID),
                eq("Lead"), anyString(), eq("LEAD_DELETED"), isNull());
    }

    @Test
    @DisplayName("Should search leads")
    void shouldSearchLeads() {
        Page<Lead> leadPage = new PageImpl<>(List.of(lead));
        when(leadRepository.searchLeads(eq(TENANT_ID), eq("john"), any(Pageable.class)))
                .thenReturn(leadPage);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(leadResponse);

        PagedResponse<LeadResponse> result = leadService.searchLeads("john", 0, 20);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }
}
