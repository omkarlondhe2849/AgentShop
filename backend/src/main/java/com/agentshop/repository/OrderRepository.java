package com.agentshop.repository;

import com.agentshop.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findBySessionId(String sessionId);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Order> findByRazorpayPaymentLinkId(String paymentLinkId);
    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByStatusAndCreatedAtAfter(Order.OrderStatus status, java.time.LocalDateTime date);

    List<Order> findByCreatedAtAfter(java.time.LocalDateTime date);

    List<Order> findTop10ByStatusOrderByCreatedAtDesc(Order.OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PAID'")
    long countPaidOrders();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID'")
    BigDecimal totalRevenue();

    @Query("SELECT COUNT(DISTINCT o.sessionId) FROM Order o")
    long countUniqueSessions();

    @Query("SELECT o.sessionId, SUM(o.totalAmount) as total FROM Order o WHERE o.status = 'PAID' GROUP BY o.sessionId ORDER BY total DESC")
    List<Object[]> findTopSessionsByRevenue();
}
