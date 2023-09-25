package com.nordic.presentation.rest.api;


import com.nordic.domain.dto.EntityDTO;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RequestMapping("/api")
public interface TPRController {

    @CreateRepute
    @PostMapping("/mks-headers")
    @PreAuthorize("hasRole('ROLE_USER')")
    ResponseEntity<EntityDTO> createMKSHeader(@Valid @RequestBody EntityDTO mksHeaderDto);

    @UpdateRepute
    @PutMapping("/mks-headers")
    @PreAuthorize("hasRole('ROLE_USER')")
    ResponseEntity<EntityDTO> updateMKSHeader(@Valid @RequestBody EntityDTO mksHeaderDto);

    @InquiryRepute
    @GetMapping("/mks-headers")
    @PreAuthorize("hasRole('ROLE_USER')")
    ResponseEntity<EntityDTO> inquiryMKSHeaders(@RequestParam(value = "id") Long id);

    @QueryRepute
    @PostMapping("/mks-headers/query")
    @PreAuthorize("hasRole('ROLE_USER')")
    ResponseEntity<Page<EntityDTO>> queryMKSHeaders(
            @RequestBody Map<String, Object> map,
            @RequestParam(value = "page") Integer page,
            @RequestParam(value = "size") Integer size,
            @RequestParam(value = "sort", required = false) String params);

    @DeleteRepute
    @DeleteMapping("/mks-headers")
    @PreAuthorize("hasRole('ROLE_USER')")
    ResponseEntity<?> deleteMKSHeader(@RequestParam(value = "id") Long id);
}
