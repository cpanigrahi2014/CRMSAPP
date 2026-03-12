package com.crm.notification.mapper;

import com.crm.notification.dto.CreateNotificationRequest;
import com.crm.notification.dto.NotificationResponse;
import com.crm.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    Notification toEntity(CreateNotificationRequest request);

    NotificationResponse toResponse(Notification notification);

    void updateEntity(CreateNotificationRequest request, @MappingTarget Notification notification);
}
