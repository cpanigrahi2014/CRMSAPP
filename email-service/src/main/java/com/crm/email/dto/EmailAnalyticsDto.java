package com.crm.email.dto;

import lombok.*;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailAnalyticsDto {
    private long totalSent;
    private long totalDelivered;
    private long totalOpened;
    private long totalClicked;
    private long totalBounced;
    private long totalFailed;
    private double openRate;       // opened / delivered * 100
    private double clickRate;      // clicked / delivered * 100
    private double bounceRate;     // bounced / sent * 100
    private double deliveryRate;   // delivered / sent * 100
    private Map<String, Long> sentByDay;         // date -> count
    private Map<String, Long> opensByDay;        // date -> count
    private Map<String, Long> clicksByDay;       // date -> count
    private Map<String, Long> sentByTemplate;    // template name -> count
}
