package com.agentshop.repository;

import com.agentshop.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Cacheable("products")
    List<Product> findByCategory(String category);

    @Cacheable("products")
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Product> searchProducts(@Param("query") String query);

    @Cacheable("products")
    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "p.price <= :maxPrice")
    List<Product> searchProductsWithMaxPrice(@Param("query") String query, @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.id != :productId ORDER BY p.rating DESC")
    List<Product> findRelatedProducts(@Param("category") String category, @Param("productId") Long productId);

    @Query("SELECT p FROM Product p WHERE p.category IN :categories AND p.id != :productId ORDER BY p.rating DESC")
    List<Product> findCrossSellProducts(@Param("categories") List<String> categories, @Param("productId") Long productId);

    List<Product> findByStockGreaterThan(int stock);

    @Query("SELECT DISTINCT p.category FROM Product p")
    List<String> findAllCategories();

    @Query("SELECT p FROM Product p WHERE p.category = :category ORDER BY p.price ASC LIMIT 1")
    Product findCheapestInCategory(@Param("category") String category);
}
