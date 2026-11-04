package com.cloudbackup.repository;

import com.cloudbackup.entity.RecoveryLog;
import com.cloudbackup.entity.RecoveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecoveryLogRepository extends JpaRepository<RecoveryLog, UUID> {

    Page<RecoveryLog> findByUserIdOrderByRecoveredAtDesc(UUID userId, Pageable pageable);

    Optional<RecoveryLog> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, RecoveryStatus status);

    List<RecoveryLog> findByUserIdAndRecoveredAtBetween(UUID userId, Instant start, Instant end);
}
