package net.codejava.catalog.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import net.codejava.account.security.CurrentUser;
import net.codejava.catalog.dto.CategoryDto;
import net.codejava.catalog.dto.CategoryRequest;
import net.codejava.catalog.dto.ProductDto;
import net.codejava.catalog.dto.ProductRequest;
import net.codejava.catalog.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** REST API for Module 3 - Inventory Management (staff only). */
@RestController
@RequestMapping("/api/admin/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final CurrentUser currentUser;

    public InventoryController(InventoryService inventoryService, CurrentUser currentUser) {
        this.inventoryService = inventoryService;
        this.currentUser = currentUser;
    }

    @GetMapping("/products")
    public List<ProductDto> list(HttpSession session) {
        currentUser.requireAdmin(session);
        return inventoryService.listAllProducts();
    }

    /** Product Addition. */
    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto add(@RequestBody ProductRequest request, HttpSession session) {
        currentUser.requireAdmin(session);
        return inventoryService.addProduct(request);
    }

    /** Product Info Edit. */
    @PutMapping("/products/{id}")
    public ProductDto edit(@PathVariable("id") Long id, @RequestBody ProductRequest request, HttpSession session) {
        currentUser.requireAdmin(session);
        return inventoryService.editProduct(id, request);
    }

    /** Product Removal. */
    @DeleteMapping("/products/{id}")
    public ProductDto remove(@PathVariable("id") Long id, HttpSession session) {
        currentUser.requireAdmin(session);
        return inventoryService.removeProduct(id);
    }

    @PostMapping("/products/{id}/restore")
    public ProductDto restore(@PathVariable("id") Long id, HttpSession session) {
        currentUser.requireAdmin(session);
        return inventoryService.restoreProduct(id);
    }

    /** Uploads a product photo (multipart form field "photo"). */
    @PostMapping("/products/{id}/photo")
    public ProductDto photo(@PathVariable("id") Long id, @RequestParam("photo") MultipartFile photo,
                            HttpSession session) {
        currentUser.requireAdmin(session);
        return inventoryService.uploadPhoto(id, photo);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto addCategory(@RequestBody CategoryRequest request, HttpSession session) {
        currentUser.requireAdmin(session);
        return inventoryService.addCategory(request);
    }
}
