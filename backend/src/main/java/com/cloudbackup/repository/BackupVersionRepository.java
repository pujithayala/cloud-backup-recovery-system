package com.cloudbackup.repository;

import com.cloudbackup.entity.BackupVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackupVersionRepository extends JpaRepository<BackupVersion, UUID> {

    List<BackupVersion> findByFileIdOrderByVersionNumberDesc(UUID fileId);

    Optional<BackupVersion> findTopByFileIdOrderByVersionNumberDesc(UUID fileId);

    List<BackupVersion> findByBackupId(UUID backupId);

    @Query("SELECT bv FROM BackupVersion bv WHERE bv.id = :id AND bv.backup.user.id = :userId")
    Optional<BackupVersion> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT bv FROM BackupVersion bv WHERE bv.file.id = :fileId AND bv.backup.user.id = :userId ORDER BY bv.versionNumber DESC")
    List<BackupVersion> findByFileIdAndUserId(@Param("fileId") UUID fileId, @Param("userId") UUID userId);
}
