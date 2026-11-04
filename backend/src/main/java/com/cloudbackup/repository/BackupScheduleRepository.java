package com.cloudbackup.repository;

import com.cloudbackup.entity.BackupSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackupScheduleRepository extends JpaRepository<BackupSchedule, UUID> {

    List<BackupSchedule> findByUserId(UUID userId);

    List<BackupSchedule> findByIsActiveTrue();

    Optional<BackupSchedule> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndIsActiveTrue(UUID userId);
}
