package com.divinecorner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder @NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String name;
    private String description;
    private String imageUrl;
    private String icon;
    private String skuPrefix;
    private Boolean active;
}
