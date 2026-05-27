package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderItem;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.StockAdjustRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockAdjustmentType;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final ProductService    productService;

    /**
     * Deduct stock for every item in the order.
     * Products with stockStatus = MADE_TO_ORDER are skipped (no physical stock tracked).
     * If any product has insufficient stock the transaction rolls back.
     */
    @Transactional
    public void deductForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProductId() == null) continue;

            Product product = productRepository.findByIdAndDeletedFalse(item.getProductId())
                    .orElse(null);
            if (product == null) continue;

            if (product.getStockStatus() == StockStatus.MADE_TO_ORDER) {
                log.debug("Skipping stock deduction for MADE_TO_ORDER product {}", product.getId());
                continue;
            }

            int units = item.getTotalUnits() != null ? item.getTotalUnits() : item.getQuantity();
            if (units <= 0) continue;

            if (product.getStockCount() < units) {
                throw new IllegalStateException(
                        "Insufficient stock for product '" + product.getName() +
                        "'. Available: " + product.getStockCount() + ", required: " + units);
            }

            int updated = productRepository.deductStock(product.getId(), units);
            if (updated == 0) {
                throw new IllegalStateException(
                        "Stock deduction failed for product '" + product.getName() +
                        "' — concurrent update detected. Please retry.");
            }
            log.info("Stock deducted: product={} units={} orderRef={}",
                    product.getId(), units, order.getReference());
        }
    }

    /**
     * Restore stock for every item in the order (cancel / refund).
     * MADE_TO_ORDER products are skipped.
     */
    @Transactional
    public void restoreForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProductId() == null) continue;

            Product product = productRepository.findByIdAndDeletedFalse(item.getProductId())
                    .orElse(null);
            if (product == null) continue;

            if (product.getStockStatus() == StockStatus.MADE_TO_ORDER) continue;

            int units = item.getTotalUnits() != null ? item.getTotalUnits() : item.getQuantity();
            if (units <= 0) continue;

            productRepository.restoreStock(product.getId(), units);
            log.info("Stock restored: product={} units={} orderRef={}",
                    product.getId(), units, order.getReference());
        }
    }

    /**
     * Manual stock adjustment by admin staff.
     * RESTOCK / RETURN / CORRECTION with positive delta adds stock.
     * DAMAGE_WRITE_OFF / MANUAL_DEDUCTION / CORRECTION with negative delta removes stock.
     */
    @Transactional
    public ProductDto adjustStock(UUID productId, StockAdjustRequest request) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        int delta = request.getDelta();

        if (request.getType() == StockAdjustmentType.RESTOCK ||
            request.getType() == StockAdjustmentType.RETURN) {
            delta = Math.abs(delta);
        } else if (request.getType() == StockAdjustmentType.DAMAGE_WRITE_OFF ||
                   request.getType() == StockAdjustmentType.MANUAL_DEDUCTION) {
            delta = -Math.abs(delta);
        }
        // CORRECTION uses signed delta as-is

        int newCount = Math.max(0, product.getStockCount() + delta);
        productRepository.setStockCount(productId, newCount);

        log.info("Manual stock adjustment: product={} type={} delta={} newCount={} reason={}",
                productId, request.getType(), delta, newCount, request.getReason());

        return productService.getById(productId);
    }

    /**
     * Direct stock count set — used by admin for full corrections.
     */
    @Transactional
    public ProductDto setStock(UUID productId, int newCount, String reason) {
        productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (newCount < 0) throw new IllegalArgumentException("Stock count cannot be negative");

        productRepository.setStockCount(productId, newCount);
        log.info("Stock set directly: product={} newCount={} reason={}", productId, newCount, reason);
        return productService.getById(productId);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getLowStockProducts() {
        return productRepository.findLowStockProducts()
                .stream()
                .map(p -> productService.getById(p.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getOutOfStockProducts() {
        return productRepository.findByStockStatusAndDeletedFalse(StockStatus.OUT_OF_STOCK)
                .stream()
                .map(p -> productService.getById(p.getId()))
                .toList();
    }
}