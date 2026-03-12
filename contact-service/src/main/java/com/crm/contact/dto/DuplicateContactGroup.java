package com.crm.contact.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateContactGroup {
    private String matchField;                // "email", "phone", "name"
    private String matchValue;
    private List<ContactResponse> contacts;
}
