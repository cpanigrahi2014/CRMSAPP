package com.crm.email.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OAuthConnectResponse {
    private String authorizationUrl;
    private String provider;
    private String state;
}
