package com.divinecorner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotBlank
    private String shippingAddress;
    @NotBlank
    private String shippingCity;
    @NotBlank
    private String shippingState;
    @NotBlank
    private String shippingZipCode;
    @NotBlank
    private String shippingCountry;
    @NotBlank
    private String customerPhone;
    @NotBlank @Email
    private String customerEmail;
    private String paymentMethod;
}
