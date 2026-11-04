package com.cloudbackup.repository;

import com.cloudbackup.entity.Backup;
import com.cloudbackup.entity.BackupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackupRepository extends JpaRepository<Backup, UUID> {

    Page<Backup> findByUserId(UUID userId, Pageable pageable);

    Page<Backup> findByUserIdAndStatus(UUID userId, BackupStatus status, Pageable pageable);

    Optional<Backup> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, BackupStatus status);

    List<Backup> findTop10ByUserIdOrderByStartedAtDesc(UUID userId);

    @Query("SELECT b FROM Backup b WHERE b.user.id = :userId AND b.startedAt BETWEEN :start AND :end ORDER BY b.startedAt DESC")
    List<Backup> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
