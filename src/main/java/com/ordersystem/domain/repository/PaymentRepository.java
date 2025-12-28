package com.ordersystem.domain.repository;

import com.ordersystem.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByOrderId(Long orderId);
    
    @Query("SELECT p FROM Payment p JOIN FETCH p.transactions WHERE p.id = :id")
    Optional<Payment> findByIdWithTransactions(@Param("id") Long id);
    
    @Query("SELECT p FROM Payment p JOIN FETCH p.transactions WHERE p.order.id = :orderId")
    List<Payment> findByOrderIdWithTransactions(@Param("orderId") Long orderId);
}


