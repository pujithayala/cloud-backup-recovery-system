import axios, { AxiosInstance } from 'axios';
import {
  ApiResponse,
  AuthResponse,
  BackupDetail,
  BackupSchedule,
  BackupSummary,
  BackupVersion,
  DashboardStats,
  ActivityPoint,
  FileMetadata,
  FileUploadResponse,
  IntegrityCheckResult,
  NotificationItem,
  RecoveryLog,
  UserSummary
} from '../types';

const API_BASE_URL = (import.meta as any).env?.VITE_API_URL || 'http://localhost:8080/api';

const client: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to attach JWT token
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('backup_auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor for 401 Unauthorized handling
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('backup_auth_token');
      localStorage.removeItem('backup_auth_user');
      if (window.location.pathname !== '/login') {
        // Optional redirect or trigger event
      }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  login: async (email: string, password: string): Promise<AuthResponse> => {
    const res = await client.post<ApiResponse<AuthResponse>>('/auth/login', { email, password });
    return res.data.data;
  },
  register: async (email: string, password: string, fullName: string): Promise<AuthResponse> => {
    const res = await client.post<ApiResponse<AuthResponse>>('/auth/register', { email, password, fullName });
    return res.data.data;
  },
  getCurrentUser: async (): Promise<UserSummary> => {
    const res = await client.get<ApiResponse<UserSummary>>('/auth/me');
    return res.data.data;
  },
};

export const filesApi = {
  uploadFiles: async (files: File[], folderPath: string = '/'): Promise<FileUploadResponse> => {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    formData.append('folderPath', folderPath);

    const res = await client.post<ApiResponse<FileUploadResponse>>('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },
  listFiles: async (search?: string, folder?: string, page: number = 0, size: number = 50) => {
    const params: any = { page, size };
    if (search) params.search = search;
    if (folder) params.folder = folder;

    const res = await client.get<ApiResponse<{ content: FileMetadata[]; totalElements: number; totalPages: number }>>(
      '/files',
      { params }
    );
    return res.data.data;
  },
  deleteFile: async (id: string): Promise<void> => {
    await client.delete(`/files/${id}`);
  },
  restoreFile: async (id: string): Promise<FileMetadata> => {
    const res = await client.post<ApiResponse<FileMetadata>>(`/files/${id}/restore`);
    return res.data.data;
  },
};

export const backupsApi = {
  createBackup: async (backupName: string, fileIds: string[], description?: string): Promise<BackupDetail> => {
    const res = await client.post<ApiResponse<BackupDetail>>('/backups', {
      backupName,
      fileIds,
      description,
    });
    return res.data.data;
  },
  listBackups: async (status?: string, page: number = 0, size: number = 20) => {
    const params: any = { page, size };
    if (status) params.status = status;
    const res = await client.get<ApiResponse<{ content: BackupSummary[]; totalElements: number; totalPages: number }>>(
      '/backups',
      { params }
    );
    return res.data.data;
  },
  getBackupById: async (id: string): Promise<BackupDetail> => {
    const res = await client.get<ApiResponse<BackupDetail>>(`/backups/${id}`);
    return res.data.data;
  },
  deleteBackup: async (id: string): Promise<void> => {
    await client.delete(`/backups/${id}`);
  },
};

export const versionsApi = {
  getFileVersions: async (fileId: string): Promise<BackupVersion[]> => {
    const res = await client.get<ApiResponse<BackupVersion[]>>(`/versions/${fileId}`);
    return res.data.data;
  },
  downloadVersionUrl: (versionId: string): string => {
    return `${API_BASE_URL}/versions/${versionId}/download`;
  },
};

export const recoveryApi = {
  verifyIntegrity: async (versionId: string): Promise<IntegrityCheckResult> => {
    const res = await client.post<ApiResponse<IntegrityCheckResult>>(`/recovery/verify-integrity/${versionId}`);
    return res.data.data;
  },
  restoreVersion: async (versionId: string, destinationPath: string = '/recovered'): Promise<RecoveryLog> => {
    const res = await client.post<ApiResponse<RecoveryLog>>(
      `/recovery/restore-version/${versionId}?destinationPath=${encodeURIComponent(destinationPath)}`
    );
    return res.data.data;
  },
  batchRestore: async (versionIds: string[], destinationPath: string = '/recovered'): Promise<RecoveryLog[]> => {
    const res = await client.post<ApiResponse<RecoveryLog[]>>('/recovery/batch', {
      versionIds,
      destinationPath,
    });
    return res.data.data;
  },
  getRecoveryHistory: async (page: number = 0, size: number = 50) => {
    const res = await client.get<ApiResponse<{ content: RecoveryLog[]; totalElements: number }>>(
      '/recovery/history',
      { params: { page, size } }
    );
    return res.data.data;
  },
};

export const schedulesApi = {
  createSchedule: async (
    scheduleName: string,
    frequency: string,
    cronExpression?: string,
    targetDirectory: string = '/'
  ): Promise<BackupSchedule> => {
    const res = await client.post<ApiResponse<BackupSchedule>>('/schedules', {
      scheduleName,
      frequency,
      cronExpression,
      targetDirectory,
    });
    return res.data.data;
  },
  listSchedules: async (): Promise<BackupSchedule[]> => {
    const res = await client.get<ApiResponse<BackupSchedule[]>>('/schedules');
    return res.data.data;
  },
  toggleSchedule: async (id: string): Promise<BackupSchedule> => {
    const res = await client.patch<ApiResponse<BackupSchedule>>(`/schedules/${id}/toggle`);
    return res.data.data;
  },
  triggerNow: async (id: string): Promise<BackupDetail> => {
    const res = await client.post<ApiResponse<BackupDetail>>(`/schedules/${id}/trigger-now`);
    return res.data.data;
  },
  deleteSchedule: async (id: string): Promise<void> => {
    await client.delete(`/schedules/${id}`);
  },
};

export const notificationsApi = {
  getAll: async (): Promise<NotificationItem[]> => {
    const res = await client.get<ApiResponse<NotificationItem[]>>('/notifications');
    return res.data.data;
  },
  getUnreadCount: async (): Promise<number> => {
    const res = await client.get<ApiResponse<{ unreadCount: number }>>('/notifications/unread-count');
    return res.data.data.unreadCount;
  },
  markAsRead: async (id: string): Promise<void> => {
    await client.patch(`/notifications/${id}/read`);
  },
  markAllAsRead: async (): Promise<void> => {
    await client.post('/notifications/read-all');
  },
};

export const analyticsApi = {
  getDashboardStats: async (): Promise<DashboardStats> => {
    const res = await client.get<ApiResponse<DashboardStats>>('/analytics/dashboard');
    return res.data.data;
  },
  getActivityTrends: async (days: number = 7): Promise<ActivityPoint[]> => {
    const res = await client.get<ApiResponse<ActivityPoint[]>>('/analytics/activity', {
      params: { days },
    });
    return res.data.data;
  },
};

