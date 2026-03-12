package com.crm.opportunity.mapper;

import com.crm.opportunity.dto.*;
import com.crm.opportunity.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OpportunityMapper {

    Opportunity toEntity(CreateOpportunityRequest request);

    OpportunityResponse toResponse(Opportunity opportunity);

    void updateEntity(UpdateOpportunityRequest request, @MappingTarget Opportunity opportunity);

    ProductResponse toProductResponse(OpportunityProduct product);

    CompetitorResponse toCompetitorResponse(OpportunityCompetitor competitor);

    ActivityResponse toActivityResponse(OpportunityActivity activity);

    CollaboratorResponse toCollaboratorResponse(OpportunityCollaborator collaborator);

    NoteResponse toNoteResponse(OpportunityNote note);

    ReminderResponse toReminderResponse(OpportunityReminder reminder);

    SalesQuotaResponse toQuotaResponse(SalesQuota quota);
}
