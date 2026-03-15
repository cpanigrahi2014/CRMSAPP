package com.crm.contact.repository;

import com.crm.contact.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {

    Page<Contact> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    List<Contact> findByTenantIdAndDeletedFalse(String tenantId);

    Optional<Contact> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<Contact> findByAccountIdAndTenantIdAndDeletedFalse(UUID accountId, String tenantId, Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false " +
            "AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.department) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Contact> searchContacts(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);

    // Feature 5: Segmentation
    Page<Contact> findBySegmentAndTenantIdAndDeletedFalse(String segment, String tenantId, Pageable pageable);

    Page<Contact> findByLifecycleStageAndTenantIdAndDeletedFalse(String stage, String tenantId, Pageable pageable);

    // Feature 9: Duplicate detection
    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false " +
            "AND c.email = :email AND c.id <> :excludeId")
    List<Contact> findDuplicatesByEmail(@Param("tenantId") String tenantId, @Param("email") String email, @Param("excludeId") UUID excludeId);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false " +
            "AND c.email = :email")
    List<Contact> findByEmailAndTenant(@Param("tenantId") String tenantId, @Param("email") String email);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false " +
            "AND c.phone = :phone AND c.phone IS NOT NULL")
    List<Contact> findByPhoneAndTenant(@Param("tenantId") String tenantId, @Param("phone") String phone);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false " +
            "AND LOWER(c.firstName) = LOWER(:firstName) AND LOWER(c.lastName) = LOWER(:lastName)")
    List<Contact> findByNameAndTenant(@Param("tenantId") String tenantId, @Param("firstName") String firstName, @Param("lastName") String lastName);

    // Feature 10: Analytics
    long countByTenantIdAndDeletedFalse(String tenantId);

    long countByTenantIdAndDeletedFalseAndEmailIsNotNull(String tenantId);

    long countByTenantIdAndDeletedFalseAndPhoneIsNotNull(String tenantId);

    long countByTenantIdAndDeletedFalseAndAccountIdIsNotNull(String tenantId);

    long countByTenantIdAndDeletedFalseAndEmailOptInTrue(String tenantId);

    long countByTenantIdAndDeletedFalseAndSmsOptInTrue(String tenantId);

    long countByTenantIdAndDeletedFalseAndDoNotCallTrue(String tenantId);

    @Query("SELECT c.segment, COUNT(c) FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.segment IS NOT NULL GROUP BY c.segment")
    List<Object[]> countBySegment(@Param("tenantId") String tenantId);

    @Query("SELECT c.lifecycleStage, COUNT(c) FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.lifecycleStage IS NOT NULL GROUP BY c.lifecycleStage")
    List<Object[]> countByLifecycleStage(@Param("tenantId") String tenantId);

    @Query("SELECT c.leadSource, COUNT(c) FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.leadSource IS NOT NULL GROUP BY c.leadSource")
    List<Object[]> countByLeadSource(@Param("tenantId") String tenantId);

    @Query("SELECT c.department, COUNT(c) FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.department IS NOT NULL GROUP BY c.department")
    List<Object[]> countByDepartment(@Param("tenantId") String tenantId);
}
