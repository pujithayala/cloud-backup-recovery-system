package com.cloudbackup.repository;

import com.cloudbackup.entity.FileMetadata;
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
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    Page<FileMetadata> findByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    Page<FileMetadata> findByUserIdAndFilenameContainingIgnoreCaseAndIsDeletedFalse(UUID userId, String filename, Pageable pageable);

    Page<FileMetadata> findByUserIdAndOriginalPathAndIsDeletedFalse(UUID userId, String originalPath, Pageable pageable);

    Optional<FileMetadata> findByIdAndUserId(UUID id, UUID userId);

    Optional<FileMetadata> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    List<FileMetadata> findAllByIdInAndUserIdAndIsDeletedFalse(List<UUID> ids, UUID userId);

    long countByUserIdAndIsDeletedFalse(UUID userId);

    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileMetadata f WHERE f.user.id = :userId AND f.isDeleted = false")
    Long sumSizeBytesByUserId(@Param("userId") UUID userId);

    List<FileMetadata> findByUserIdAndIsDeletedFalse(UUID userId);
}

