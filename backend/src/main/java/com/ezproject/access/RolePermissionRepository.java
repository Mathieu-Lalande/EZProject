package com.ezproject.access;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.Key> {

    @Query("select rp from RolePermission rp where rp.id.organizationId = :orgId")
    List<RolePermission> findByOrganization(@Param("orgId") Long organizationId);

    @Modifying
    @Query("delete from RolePermission rp where rp.id.organizationId = :orgId")
    void deleteByOrganization(@Param("orgId") Long organizationId);
}
