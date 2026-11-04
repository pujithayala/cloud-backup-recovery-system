export interface UserSummary {
  id: string;
  email: string;
  fullName: string;
  role: 'ROLE_USER' | 'ROLE_ADMIN';
  storageQuotaBytes: number;
  storageUsedBytes: number;
  storageUsagePercentage: number;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: UserSummary;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface FileMetadata {
  id: string;
  filename: string;
  originalPath: string;
  contentType: string;
  sizeBytes: number;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface FileUploadResponse {
  uploadedCount: number;
  totalSizeBytes: number;
  files: FileMetadata[];
}

export interface BackupVersion {
  id: string;
  backupId: string;
  backupName: string;
  fileId: string;
  filename: string;
  versionNumber: number;
  storageKey: string;
  checksumSha256: string;
  sizeBytes: number;
  status: 'ACTIVE' | 'RESTORED' | 'CORRUPTED';
  createdAt: string;
}

export interface BackupSummary {
  id: string;
  backupName: string;
  description?: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  totalFiles: number;
  totalSizeBytes: number;
  startedAt: string;
  completedAt?: string;
}

export interface BackupDetail extends BackupSummary {
  scheduleId?: string;
  versions: BackupVersion[];
}

export interface BackupSchedule {
  id: string;
  scheduleName: string;
  cronExpression: string;
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM';
  isActive: boolean;
  targetDirectory: string;
  lastRunAt?: string;
  nextRunAt?: string;
  createdAt: string;
}

export interface RecoveryLog {
  id: string;
  backupId?: string;
  backupName?: string;
  versionId: string;
  fileId?: string;
  filename?: string;
  versionNumber?: number;
  destinationPath: string;
  status: 'INITIATED' | 'SUCCESS' | 'FAILED' | 'INTEGRITY_FAILED';
  checksumVerified: boolean;
  errorMessage?: string;
  recoveredAt: string;
}

export interface IntegrityCheckResult {
  versionId: string;
  filename: string;
  storedChecksum: string;
  computedChecksum: string;
  match: boolean;
  versionStatus: 'ACTIVE' | 'RESTORED' | 'CORRUPTED';
}

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  notificationType: 'BACKUP_SUCCESS' | 'BACKUP_FAILURE' | 'RECOVERY_SUCCESS' | 'RECOVERY_FAILURE' | 'SCHEDULE_TRIGGERED' | 'SYSTEM_ALERT';
  isRead: boolean;
  metadataJson?: string;
  createdAt: string;
}

export interface DashboardStats {
  totalFilesBackedUp: number;
  totalStorageConsumedBytes: number;
  storageQuotaBytes: number;
  storageUsedPercentage: number;
  successfulBackups: number;
  failedBackups: number;
  totalRecoveryOperations: number;
  activeSchedules: number;
  unreadNotifications: number;
}

export interface ActivityPoint {
  date: string;
  backupCount: number;
  bytesBackedUp: number;
  recoveryCount: number;
}

