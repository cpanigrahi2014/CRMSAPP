package com.crm.account.repository;

import com.crm.account.entity.AccountTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountTagRepository extends JpaRepository<AccountTag, UUID> {

    List<AccountTag> findByTenantIdAndDeletedFalse(String tenantId);

    Optional<AccountTag> findByNameAndTenantIdAndDeletedFalse(String name, String tenantId);

    Optional<AccountTag> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    @Query(value = "SELECT t.* FROM account_tags t " +
            "JOIN account_tag_mappings m ON t.id = m.tag_id " +
            "WHERE m.account_id = :accountId AND t.tenant_id = :tenantId AND t.deleted = false",
            nativeQuery = true)
    List<AccountTag> findTagsByAccountId(@Param("accountId") UUID accountId, @Param("tenantId") String tenantId);

    @Modifying
    @Query(value = "INSERT INTO account_tag_mappings (account_id, tag_id) VALUES (:accountId, :tagId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    void addTagToAccount(@Param("accountId") UUID accountId, @Param("tagId") UUID tagId);

    @Modifying
    @Query(value = "DELETE FROM account_tag_mappings WHERE account_id = :accountId AND tag_id = :tagId",
            nativeQuery = true)
    void removeTagFromAccount(@Param("accountId") UUID accountId, @Param("tagId") UUID tagId);
}
