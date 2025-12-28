package com.ordersystem.domain.repository;

import com.ordersystem.domain.enums.OrderStatus;
import com.ordersystem.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByCustomerId(Long customerId);
    
    List<Order> findByStatus(OrderStatus status);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.items JOIN FETCH o.customer WHERE o.id = :id")
    Optional<Order> findByIdWithItemsAndCustomer(@Param("id") Long id);
    
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.stateHistory WHERE o.id = :id")
    Optional<Order> findByIdWithItemsAndCustomerAndHistory(@Param("id") Long id);
}

