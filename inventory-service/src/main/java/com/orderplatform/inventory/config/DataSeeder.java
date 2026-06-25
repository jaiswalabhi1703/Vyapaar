package com.orderplatform.inventory.config;

import com.orderplatform.inventory.service.InventoryService;
import com.orderplatform.inventory.repo.ProductRepository;
import com.orderplatform.inventory.web.dto.CreateProductRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/** Seeds a small catalog (prices in INR) so the storefront has something to show. */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String IMG = "https://images.unsplash.com/";

    @Bean
    ApplicationRunner seed(ProductRepository products, InventoryService inventory) {
        return args -> {
            if (products.count() > 0) {
                return;
            }
            inventory.createProduct(new CreateProductRequest("Mechanical Keyboard",
                    new BigDecimal("4999"), "Hot-swappable switches, RGB, compact 75% layout",
                    IMG + "photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80", 50));
            inventory.createProduct(new CreateProductRequest("27\" 4K Monitor",
                    new BigDecimal("24999"), "IPS panel, USB-C 90W, height-adjustable stand",
                    IMG + "photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=600&q=80", 20));
            inventory.createProduct(new CreateProductRequest("Noise-Cancelling Headphones",
                    new BigDecimal("14999"), "Over-ear ANC, 30h battery, multipoint Bluetooth",
                    IMG + "photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80", 100));
            inventory.createProduct(new CreateProductRequest("Wireless Mouse",
                    new BigDecimal("1799"), "Silent clicks, 8000 DPI, USB-C rechargeable",
                    IMG + "photo-1527814050087-3793815479db?auto=format&fit=crop&w=600&q=80", 75));
            inventory.createProduct(new CreateProductRequest("Smart Watch",
                    new BigDecimal("9999"), "AMOLED, SpO2 + heart rate, 7-day battery",
                    IMG + "photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80", 40));
            inventory.createProduct(new CreateProductRequest("Bluetooth Speaker",
                    new BigDecimal("3499"), "360° sound, IPX7 waterproof, 24h playback",
                    IMG + "photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=600&q=80", 60));
            inventory.createProduct(new CreateProductRequest("Limited Edition Mousepad",
                    new BigDecimal("799"), "Scarce stock — great for testing concurrency",
                    IMG + "photo-1616071358593-2c0e6c4a0f9f?auto=format&fit=crop&w=600&q=80", 1));
            log.info("Seeded {} products (INR)", products.count());
        };
    }
}
