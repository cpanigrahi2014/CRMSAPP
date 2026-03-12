package com.crm.activity.mapper;

import com.crm.activity.dto.CreateActivityRequest;
import com.crm.activity.dto.ActivityResponse;
import com.crm.activity.entity.Activity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ActivityMapper {

    Activity toEntity(CreateActivityRequest request);

    ActivityResponse toResponse(Activity activity);

    void updateEntity(CreateActivityRequest request, @MappingTarget Activity activity);
}
