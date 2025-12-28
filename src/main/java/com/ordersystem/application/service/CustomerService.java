package com.ordersystem.application.service;

import com.ordersystem.api.dto.request.CreateCustomerRequest;
import com.ordersystem.api.dto.response.CustomerResponse;
import com.ordersystem.domain.exception.BusinessRuleException;
import com.ordersystem.domain.exception.EntityNotFoundException;
import com.ordersystem.domain.model.Customer;
import com.ordersystem.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    @SuppressWarnings("null") // JPA save() siempre retorna un objeto no-null
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        // Validar que el email no exista
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException(
                "DUPLICATE_EMAIL",
                "Ya existe un cliente con el email: " + request.getEmail()
            );
        }

        Customer customer = Customer.builder()
            .name(request.getName())
            .email(request.getEmail())
            .build();

        Customer saved = customerRepository.save(customer);
        // JPA save siempre retorna un objeto no-null
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(Objects.requireNonNull(id, "El ID del cliente no puede ser null"))
            .orElseThrow(() -> new EntityNotFoundException("Cliente", id));
        
        return mapToResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("Cliente", email));
        
        return mapToResponse(customer);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
            .id(customer.getId())
            .name(customer.getName())
            .email(customer.getEmail())
            .createdAt(customer.getCreatedAt())
            .build();
    }
}

