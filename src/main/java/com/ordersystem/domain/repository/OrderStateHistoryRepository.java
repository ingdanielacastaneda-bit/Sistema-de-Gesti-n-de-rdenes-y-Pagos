package com.ordersystem.domain.repository;

import com.ordersystem.domain.model.OrderStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStateHistoryRepository extends JpaRepository<OrderStateHistory, Long> {
    
    List<OrderStateHistory> findByOrderIdOrderByTimestampAsc(Long orderId);
}


