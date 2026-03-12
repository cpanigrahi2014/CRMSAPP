package com.crm.lead.mapper;

import com.crm.lead.dto.CreateLeadRequest;
import com.crm.lead.dto.LeadResponse;
import com.crm.lead.entity.Lead;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    Lead toEntity(CreateLeadRequest request);

    LeadResponse toResponse(Lead lead);

    void updateEntity(CreateLeadRequest request, @MappingTarget Lead lead);
}
