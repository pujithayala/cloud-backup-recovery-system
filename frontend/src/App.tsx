import React, { useState, useEffect } from 'react';
import { UserSummary, DashboardStats, ActivityPoint } from './types';
import { authApi, analyticsApi } from './services/api';

export const App: React.FC = () => {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [currentTab, setCurrentTab] = useState<'dashboard' | 'files' | 'backups' | 'integrity' | 'schedules' | 'recovery'>('dashboard');
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('backup_auth_token');
    if (token) {
      authApi.getCurrentUser()
        .then(u => {
          setUser(u);
          return analyticsApi.getDashboardStats();
        })
        .then(s => setStats(s))
        .catch(() => {
          localStorage.removeItem('backup_auth_token');
          setUser(null);
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      <header className="h-16 border-b border-slate-800 bg-slate-900/80 px-6 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center font-bold text-white">CB</div>
          <span className="font-bold tracking-tight">CloudBackup Enterprise React Client</span>
        </div>
        <div className="flex items-center space-x-4 text-xs text-slate-400">
          <span>Engine: MinIO/S3 + SHA-256</span>
          {user && <span className="text-blue-400 font-medium">{user.email}</span>}
        </div>
      </header>
      <main className="flex-1 p-8">
        {stats ? (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
              <span className="text-xs text-slate-400 uppercase">Files Tracked</span>
              <div className="text-2xl font-bold font-mono mt-2">{stats.totalFilesBackedUp}</div>
            </div>
            <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
              <span className="text-xs text-slate-400 uppercase">Storage Consumed</span>
              <div className="text-2xl font-bold font-mono mt-2">{(stats.totalStorageConsumedBytes / (1024*1024)).toFixed(1)} MB</div>
            </div>
            <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
              <span className="text-xs text-slate-400 uppercase">Success Rate</span>
              <div className="text-2xl font-bold font-mono text-emerald-400 mt-2">
                {stats.successfulBackups + stats.failedBackups > 0
                  ? Math.round((stats.successfulBackups / (stats.successfulBackups + stats.failedBackups)) * 100)
                  : 100}%
              </div>
            </div>
            <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800">
              <span className="text-xs text-slate-400 uppercase">Active Automations</span>
              <div className="text-2xl font-bold font-mono text-amber-400 mt-2">{stats.activeSchedules}</div>
            </div>
          </div>
        ) : (
          <div className="text-center py-20 text-slate-500">
            <p>Connect with the backend console or open <code className="text-blue-400">index.html</code> to view full interactive application.</p>
          </div>
        )}
      </main>
    </div>
  );
};

export default App;

