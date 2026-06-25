package com.orderplatform.inventory.web;

import com.orderplatform.inventory.service.InventoryService;
import com.orderplatform.inventory.web.dto.CreateProductRequest;
import com.orderplatform.inventory.web.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final InventoryService service;

    public ProductController(InventoryService service) {
        this.service = service;
    }

    /** The gateway forwards the authenticated role in X-User-Role; only ADMIN sees stock numbers. */
    @GetMapping
    public List<ProductResponse> list(@RequestHeader(value = "X-User-Role", required = false) String role) {
        return service.listProducts(isAdmin(role));
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id,
                               @RequestHeader(value = "X-User-Role", required = false) String role) {
        return service.getProduct(id, isAdmin(role));
    }

    private static boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest req) {
        return service.createProduct(req);
    }
}
