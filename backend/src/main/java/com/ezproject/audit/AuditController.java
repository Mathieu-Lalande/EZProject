package com.ezproject.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ezproject.common.PageDto;

@RestController
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping("/api/organization/audit")
    public PageDto<AuditService.AuditDto> search(@RequestParam(required = false) String entityType,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return service.search(entityType, q, page, size);
    }
}
