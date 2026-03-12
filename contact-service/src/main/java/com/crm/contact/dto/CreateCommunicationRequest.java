package com.crm.contact.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommunicationRequest {

    @NotBlank(message = "Communication type is required")
    private String commType;          // EMAIL, CALL, MEETING, NOTE, SMS

    private String subject;
    private String body;

    @NotBlank(message = "Direction is required")
    private String direction;         // INBOUND, OUTBOUND

    private String status;            // COMPLETED, PENDING, MISSED, SCHEDULED
    private LocalDateTime communicationDate;
}
