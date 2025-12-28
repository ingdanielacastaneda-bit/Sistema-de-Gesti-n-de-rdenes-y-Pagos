package com.ordersystem.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {
    
    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String name;
    
    @NotBlank(message = "El email del cliente es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    private String email;
}


