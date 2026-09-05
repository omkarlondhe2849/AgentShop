package com.agentshop.config;

import com.agentshop.model.AuditLog;
import com.agentshop.model.Order;
import com.agentshop.repository.AuditLogRepository;
import com.agentshop.repository.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedData(OrderRepository orderRepository, AuditLogRepository auditLogRepository) {
        return args -> {
            if (orderRepository.count() > 0) {
                return; // Already seeded
            }

            Random rand = new Random();
            String[] customers = {"John Doe", "Jane Smith", "Alice Johnson", "Bob Brown", "Charlie Davis"};
            String[] agents = {"default-session", "default-session", "agent-x1", "agent-y2"};
            String[] products = {
                    "1 x Sony WH-1000XM5, 1 x Apple AirPods Pro",
                    "1 x Dell XPS 15",
                    "2 x Samsung Galaxy S24 Ultra",
                    "1 x iPad Pro, 1 x Apple Pencil",
                    "1 x Bose QuietComfort 45",
                    "1 x Razer Blade 16, 1 x Razer DeathAdder V3",
                    "1 x LG C3 OLED TV"
            };
            
            BigDecimal[] amounts = {
                    new BigDecimal("35000.00"), new BigDecimal("120000.00"), 
                    new BigDecimal("210000.00"), new BigDecimal("95000.00"), 
                    new BigDecimal("24000.00"), new BigDecimal("185000.00"), 
                    new BigDecimal("150000.00")
            };

            for (int i = 0; i < 25; i++) {
                Order order = new Order();
                String session = agents[rand.nextInt(agents.length)];
                int pIdx = rand.nextInt(products.length);
                
                order.setSessionId(session);
                order.setCustomerName(customers[rand.nextInt(customers.length)]);
                order.setCustomerEmail(order.getCustomerName().split(" ")[0].toLowerCase() + "@example.com");
                order.setCustomerPhone("9876543210");
                order.setDeliveryAddress("123 Fake St, City");
                order.setItemsSummary(products[pIdx]);
                
                // Randomize discount 0 to 15%
                BigDecimal amount = amounts[pIdx];
                BigDecimal discount = amount.multiply(BigDecimal.valueOf(rand.nextInt(15))).divide(BigDecimal.valueOf(100));
                order.setDiscountAmount(discount);
                order.setTotalAmount(amount.subtract(discount));
                
                // Randomize date over the last 7 days
                LocalDateTime date = LocalDateTime.now().minusDays(rand.nextInt(7)).minusHours(rand.nextInt(24));
                
                order.setStatus(Order.OrderStatus.PAID);
                order.setRazorpayOrderId("order_mock_" + UUID.randomUUID().toString().substring(0, 8));
                order.setRazorpayPaymentLinkId("plink_mock_" + UUID.randomUUID().toString().substring(0, 8));
                order.setRazorpayPaymentId("pay_mock_" + UUID.randomUUID().toString().substring(0, 8));
                
                orderRepository.save(order); // Save first to generate ID
                
                // Set the created/paid at dates and re-save
                order.setCreatedAt(date);
                order.setPaidAt(date.plusMinutes(rand.nextInt(30)));
                orderRepository.save(order);
                
                // Generate a matching audit log for the checkout
                AuditLog log = new AuditLog();
                log.setSessionId(session);
                log.setActionType(AuditLog.ActionType.CHECKOUT_INITIATED);
                log.setDescription("Checkout initiated for " + order.getCustomerName());
                log.setAgentReasoning("User requested to checkout cart.");
                log.setStatus(AuditLog.ActionStatus.SUCCESS);
                log.setBoundaryCheckPassed(true);
                log.setBoundaryDetails("Discount " + discount + " passed max ceiling check.");
                log.setTimestamp(date.minusMinutes(2));
                auditLogRepository.save(log);
            }
            
            // Generate some random search audit logs
            for (int i = 0; i < 30; i++) {
                AuditLog log = new AuditLog();
                log.setSessionId(agents[rand.nextInt(agents.length)]);
                log.setActionType(AuditLog.ActionType.PRODUCT_SEARCH);
                log.setDescription("Searched for products.");
                log.setAgentReasoning("User is looking for items.");
                log.setStatus(AuditLog.ActionStatus.SUCCESS);
                log.setTimestamp(LocalDateTime.now().minusDays(rand.nextInt(7)).minusHours(rand.nextInt(24)));
                auditLogRepository.save(log);
            }
        };
    }
}
