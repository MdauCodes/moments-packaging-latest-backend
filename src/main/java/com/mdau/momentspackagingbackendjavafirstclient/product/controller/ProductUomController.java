package com.mdau.momentspackagingbackendjavafirstclient.product.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductUomDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductUom;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductUomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ProductUomController {

    private final ProductUomRepository uomRepository;

    /** Public — frontend needs this to display UOM options */
    @GetMapping("/api/v1/public/uoms")
    public ResponseEntity<List<ProductUomDto>> listPublic() {
        return ResponseEntity.ok(
                uomRepository.findByDeletedFalseOrderBySortOrderAsc()
                        .stream().map(ProductUomDto::new).collect(Collectors.toList()));
    }

    /** Admin — full list including soft-deleted for management */
    @IsStaffOrAdmin
    @GetMapping("/api/v1/admin/uoms")
    public ResponseEntity<List<ProductUomDto>> listAdmin() {
        return ResponseEntity.ok(
                uomRepository.findByDeletedFalseOrderBySortOrderAsc()
                        .stream().map(ProductUomDto::new).collect(Collectors.toList()));
    }

    /** Admin — create a custom UOM (e.g. DOZEN, ROLL) */
    @IsStaffOrAdmin
    @PostMapping("/api/v1/admin/uoms")
    public ResponseEntity<ProductUomDto> create(@RequestBody Map<String, Object> body) {
        String code = ((String) body.get("code")).toUpperCase().trim();
        String name = (String) body.get("name");
        String description = (String) body.getOrDefault("description", null);
        Integer sortOrder = body.get("sortOrder") != null
                ? Integer.parseInt(body.get("sortOrder").toString()) : 99;

        if (uomRepository.existsByCodeAndDeletedFalse(code)) {
            throw new IllegalArgumentException("A UOM with code '" + code + "' already exists.");
        }

        ProductUom uom = uomRepository.save(ProductUom.builder()
                .code(code).name(name).description(description)
                .isDefault(false).sortOrder(sortOrder).deleted(false)
                .build());

        URI location = URI.create("/api/v1/admin/uoms/" + uom.getId());
        return ResponseEntity.created(location).body(new ProductUomDto(uom));
    }

    /** Admin — soft-delete a custom UOM (default UOMs are protected) */
    @IsStaffOrAdmin
    @DeleteMapping("/api/v1/admin/uoms/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ProductUom uom = uomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UOM not found: " + id));

        if (Boolean.TRUE.equals(uom.getIsDefault())) {
            throw new IllegalArgumentException(
                    "Default UOMs (PIECE, PACKET, CARTON, BALE) cannot be deleted.");
        }

        uom.setDeleted(true);
        uomRepository.save(uom);
        return ResponseEntity.noContent().build();
    }
}