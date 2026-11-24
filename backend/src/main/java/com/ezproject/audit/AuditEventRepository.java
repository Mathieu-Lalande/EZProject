package com.ezproject.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("select e from AuditEvent e where e.organization.id = :orgId "
            + "and (:entityType is null or e.entityType = :entityType) "
            + "and (:query is null or lower(e.summary) like :query or lower(e.actorName) like :query) "
            + "order by e.createdAt desc, e.id desc")
    Page<AuditEvent> search(@Param("orgId") Long organizationId, @Param("entityType") String entityType,
                            @Param("query") String query, Pageable pageable);
}
