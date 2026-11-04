/**
 * CloudBackup Enterprise Client Application
 * Handles authentication, real-time dashboard analytics, object versioning,
 * SHA-256 integrity verification, scheduling, and disaster recovery orchestration.
 */

const API_BASE = 'http://localhost:8080/api';

// State Management
let currentUser = null;
let currentToken = null;
let currentView = 'dashboard';
let activityChartInstance = null;
let cachedFiles = [];
let cachedVersions = [];
let searchDebounceTimer = null;

// Initialize on Document Ready
document.addEventListener('DOMContentLoaded', () => {
  initAuth();
  if (window.lucide) {
    lucide.createIcons();
  }
});

// Toast Notification Engine
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  const bgColors = {
    success: 'bg-emerald-950 border-emerald-500/50 text-emerald-200',
    error: 'bg-rose-950 border-rose-500/50 text-rose-200',
    info: 'bg-slate-900 border-slate-700 text-slate-200',
    warning: 'bg-amber-950 border-amber-500/50 text-amber-200'
  };

  const icons = {
    success: 'check-circle-2',
    error: 'alert-circle',
    info: 'info',
    warning: 'alert-triangle'
  };

  toast.className = `flex items-center space-x-2.5 px-4 py-3 rounded-xl border shadow-xl text-xs font-medium transform transition-all duration-300 pointer-events-auto opacity-0 translate-y-2 ${bgColors[type] || bgColors.info}`;
  toast.innerHTML = `
    <i data-lucide="${icons[type] || 'info'}" class="w-4 h-4 shrink-0"></i>
    <span>${message}</span>
  `;

  container.appendChild(toast);
  if (window.lucide) lucide.createIcons();

  requestAnimationFrame(() => {
    toast.classList.remove('opacity-0', 'translate-y-2');
  });

  setTimeout(() => {
    toast.classList.add('opacity-0', '-translate-y-1');
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Authentication Handlers
function initAuth() {
  const token = localStorage.getItem('backup_auth_token');
  const userJson = localStorage.getItem('backup_auth_user');

  if (token && userJson) {
    currentToken = token;
    try {
      currentUser = JSON.parse(userJson);
      updateUserInterface();
      hideAuthModal();
      loadActiveView();
      loadNotifications();
    } catch (e) {
      handleLogout();
    }
  } else {
    showAuthModal();
  }
}

function showAuthModal() {
  const modal = document.getElementById('auth-modal');
  if (modal) modal.classList.remove('hidden');
}

function hideAuthModal() {
  const modal = document.getElementById('auth-modal');
  if (modal) modal.classList.add('hidden');
}

function switchAuthTab(tab) {
  const loginForm = document.getElementById('form-login');
  const regForm = document.getElementById('form-register');
  const tabLogin = document.getElementById('tab-login');
  const tabReg = document.getElementById('tab-register');

  if (tab === 'login') {
    loginForm.classList.remove('hidden');
    regForm.classList.add('hidden');
    tabLogin.className = 'flex-1 pb-3 text-sm font-semibold border-b-2 border-blue-500 text-blue-400 text-center transition';
    tabReg.className = 'flex-1 pb-3 text-sm font-medium border-b-2 border-transparent text-slate-400 hover:text-slate-200 text-center transition';
  } else {
    loginForm.classList.add('hidden');
    regForm.classList.remove('hidden');
    tabReg.className = 'flex-1 pb-3 text-sm font-semibold border-b-2 border-blue-500 text-blue-400 text-center transition';
    tabLogin.className = 'flex-1 pb-3 text-sm font-medium border-b-2 border-transparent text-slate-400 hover:text-slate-200 text-center transition';
  }
}

function fillDemoUser() {
  document.getElementById('login-email').value = 'admin@cloud.io';
  document.getElementById('login-password').value = 'Password123!';
}

async function handleLogin(e) {
  e.preventDefault();
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;
  const btn = document.getElementById('btn-login-submit');

  try {
    btn.disabled = true;
    btn.innerHTML = `<span>Authenticating...</span>`;

    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Authentication failed');
    }

    currentToken = data.data.token;
    currentUser = data.data.user;
    localStorage.setItem('backup_auth_token', currentToken);
    localStorage.setItem('backup_auth_user', JSON.stringify(currentUser));

    showToast(`Welcome back, ${currentUser.fullName}`, 'success');
    hideAuthModal();
    updateUserInterface();
    loadActiveView();
    loadNotifications();

  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<span>Authenticate & Access Console</span><i data-lucide="arrow-right" class="w-4 h-4"></i>`;
    if (window.lucide) lucide.createIcons();
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const fullName = document.getElementById('reg-name').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const password = document.getElementById('reg-password').value;
  const btn = document.getElementById('btn-reg-submit');

  try {
    btn.disabled = true;
    btn.innerHTML = `<span>Creating Account...</span>`;

    const res = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName, email, password })
    });

    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Registration failed');
    }

    currentToken = data.data.token;
    currentUser = data.data.user;
    localStorage.setItem('backup_auth_token', currentToken);
    localStorage.setItem('backup_auth_user', JSON.stringify(currentUser));

    showToast('Account registered successfully! 10 GB quota allocated.', 'success');
    hideAuthModal();
    updateUserInterface();
    loadActiveView();
    loadNotifications();

  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<span>Create Enterprise Account</span><i data-lucide="user-plus" class="w-4 h-4"></i>`;
    if (window.lucide) lucide.createIcons();
  }
}

function handleLogout() {
  currentToken = null;
  currentUser = null;
  localStorage.removeItem('backup_auth_token');
  localStorage.removeItem('backup_auth_user');
  showAuthModal();
  showToast('You have been signed out.', 'info');
}

// User Interface & Quota Updates
function updateUserInterface() {
  if (!currentUser) return;

  const initials = currentUser.fullName
    .split(' ')
    .map(n => n[0])
    .join('')
    .substring(0, 2)
    .toUpperCase();

  const avatar = document.getElementById('user-avatar-initials');
  const nameEl = document.getElementById('header-user-name');
  const emailEl = document.getElementById('header-user-email');

  if (avatar) avatar.innerText = initials || 'U';
  if (nameEl) nameEl.innerText = currentUser.fullName;
  if (emailEl) emailEl.innerText = currentUser.email;

  updateStorageQuotaDisplay(currentUser.storageUsedBytes, currentUser.storageQuotaBytes);
}

function updateStorageQuotaDisplay(usedBytes = 0, quotaBytes = 10737418240) {
  const usedMB = (usedBytes / (1024 * 1024)).toFixed(1);
  const quotaGB = (quotaBytes / (1024 * 1024 * 1024)).toFixed(2);
  const percent = quotaBytes > 0 ? Math.min(100, Math.round((usedBytes / quotaBytes) * 1000) / 10) : 0;

  const textEl = document.getElementById('header-quota-text');
  const barEl = document.getElementById('header-quota-bar');

  if (textEl) textEl.innerText = `${usedMB} MB / ${quotaGB} GB`;
  if (barEl) barEl.style.width = `${Math.max(2, percent)}%`;
}

// Authenticated Fetch Helper
async function apiFetch(endpoint, options = {}) {
  if (!currentToken) {
    showAuthModal();
    throw new Error('Unauthenticated session');
  }

  const defaultHeaders = {
    'Authorization': `Bearer ${currentToken}`
  };

  if (!(options.body instanceof FormData)) {
    defaultHeaders['Content-Type'] = 'application/json';
  }

  const res = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers
    }
  });

  if (res.status === 401) {
    handleLogout();
    throw new Error('Session expired. Please log in again.');
  }

  const data = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(data?.message || `Request failed with status ${res.status}`);
  }

  return data;
}

// Navigation View Routing
function navigateView(viewId) {
  currentView = viewId;
  const views = ['dashboard', 'files', 'backups', 'integrity', 'schedules', 'recovery'];

  views.forEach(v => {
    const el = document.getElementById(`view-${v}`);
    const navEl = document.getElementById(`nav-${v}`);
    if (el) {
      if (v === viewId) {
        el.classList.remove('hidden');
      } else {
        el.classList.add('hidden');
      }
    }
    if (navEl) {
      if (v === viewId) {
        navEl.className = 'nav-item w-full flex items-center space-x-3 px-3.5 py-2.5 rounded-xl text-sm font-medium text-blue-400 bg-blue-600/10 border border-blue-500/20 transition';
      } else {
        navEl.className = 'nav-item w-full flex items-center space-x-3 px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 border border-transparent transition';
      }
    }
  });

  loadActiveView();
  if (window.lucide) lucide.createIcons();
}

function loadActiveView() {
  switch (currentView) {
    case 'dashboard':
      loadDashboardData();
      break;
    case 'files':
      loadFilesData();
      break;
    case 'backups':
      loadBackupsData();
      break;
    case 'integrity':
      loadIntegrityData();
      break;
    case 'schedules':
      loadSchedulesData();
      break;
    case 'recovery':
      loadRecoveryHistory();
      break;
  }
}

// VIEW 1: DASHBOARD & ANALYTICS
async function loadDashboardData() {
  try {
    const [statsRes, activityRes, backupsRes] = await Promise.all([
      apiFetch('/analytics/dashboard'),
      apiFetch('/analytics/activity?days=7'),
      apiFetch('/backups?page=0&size=5')
    ]);

    const stats = statsRes.data;
    const activity = activityRes.data;
    const backups = backupsRes.data.content;

    // Update KPI metrics
    document.getElementById('kpi-files-count').innerText = stats.totalFilesBackedUp.toLocaleString();
    const usedMB = (stats.totalStorageConsumedBytes / (1024 * 1024)).toFixed(1);
    const quotaGB = (stats.storageQuotaBytes / (1024 * 1024 * 1024)).toFixed(2);
    document.getElementById('kpi-storage-consumed').innerText = `${usedMB} MB`;
    document.getElementById('kpi-storage-percent').innerText = `(${stats.storageUsedPercentage}%)`;
    document.getElementById('kpi-storage-quota').innerText = `of ${quotaGB} GB available`;

    const totalRuns = stats.successfulBackups + stats.failedBackups;
    const rate = totalRuns > 0 ? Math.round((stats.successfulBackups / totalRuns) * 100) : 100;
    document.getElementById('kpi-success-rate').innerText = `${rate}%`;
    document.getElementById('kpi-backup-ratio').innerText = `${stats.successfulBackups} passed / ${stats.failedBackups} failed`;
    document.getElementById('kpi-active-schedules').innerText = stats.activeSchedules;

    updateStorageQuotaDisplay(stats.totalStorageConsumedBytes, stats.storageQuotaBytes);

    // Render Activity Chart
    renderActivityChart(activity);

    // Render Recent Backups
    renderRecentBackups(backups);

  } catch (err) {
    showToast(`Dashboard error: ${err.message}`, 'error');
  }
}

function renderActivityChart(dataPoints = []) {
  const ctx = document.getElementById('activityChart');
  if (!ctx) return;

  const labels = dataPoints.map(p => {
    const parts = p.date.split('-');
    return `${parts[1]}/${parts[2]}`;
  });

  const backupCounts = dataPoints.map(p => p.backupCount);
  const recoveryCounts = dataPoints.map(p => p.recoveryCount);

  if (activityChartInstance) {
    activityChartInstance.destroy();
  }

  activityChartInstance = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [
        {
          label: 'Backups Created',
          data: backupCounts,
          backgroundColor: 'rgba(59, 130, 246, 0.75)',
          borderColor: '#3b82f6',
          borderWidth: 1.5,
          borderRadius: 6,
        },
        {
          label: 'Recoveries Executed',
          data: recoveryCounts,
          backgroundColor: 'rgba(16, 185, 129, 0.75)',
          borderColor: '#10b981',
          borderWidth: 1.5,
          borderRadius: 6,
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          labels: { color: '#94a3b8', font: { size: 11, family: 'Inter' } }
        }
      },
      scales: {
        x: {
          grid: { color: 'rgba(51, 65, 85, 0.3)' },
          ticks: { color: '#94a3b8', font: { size: 10 } }
        },
        y: {
          beginAtZero: true,
          grid: { color: 'rgba(51, 65, 85, 0.3)' },
          ticks: { color: '#94a3b8', stepSize: 1, font: { size: 10 } }
        }
      }
    }
  });
}

function renderRecentBackups(backups = []) {
  const container = document.getElementById('dashboard-recent-backups');
  if (!container) return;

  if (backups.length === 0) {
    container.innerHTML = `<p class="text-slate-500 text-xs py-4 text-center">No backup runs recorded yet.</p>`;
    return;
  }

  container.innerHTML = backups.map(b => `
    <div class="p-3 rounded-xl bg-slate-900/80 border border-slate-800/80 flex items-center justify-between text-xs">
      <div>
        <div class="font-medium text-slate-200">${b.backupName}</div>
        <div class="text-[11px] text-slate-400 font-mono mt-0.5">${formatBytes(b.totalSizeBytes)} • ${b.totalFiles} files</div>
      </div>
      <span class="px-2 py-0.5 rounded text-[10px] font-semibold ${getStatusBadgeClass(b.status)}">
        ${b.status}
      </span>
    </div>
  `).join('');
}

// VIEW 2: FILES & STORAGE
async function loadFilesData(search = '') {
  try {
    const endpoint = search ? `/files?search=${encodeURIComponent(search)}` : '/files';
    const res = await apiFetch(endpoint);
    cachedFiles = res.data.content;

    const tbody = document.getElementById('files-table-body');
    if (!tbody) return;

    if (cachedFiles.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="py-8 text-center text-slate-500">No files found. Click "Upload Files" to ingest source documents.</td></tr>`;
      return;
    }

    tbody.innerHTML = cachedFiles.map(f => `
      <tr class="hover:bg-slate-900/60 transition">
        <td class="py-3 px-4 font-medium text-slate-200 flex items-center space-x-2">
          <i data-lucide="file" class="w-4 h-4 text-blue-400 shrink-0"></i>
          <span>${f.filename}</span>
        </td>
        <td class="py-3 px-4 text-slate-400 font-mono text-[11px]">${f.originalPath}</td>
        <td class="py-3 px-4 text-slate-400">${f.contentType}</td>
        <td class="py-3 px-4 font-mono">${formatBytes(f.sizeBytes)}</td>
        <td class="py-3 px-4 text-slate-400 text-[11px]">${formatDate(f.createdAt)}</td>
        <td class="py-3 px-4 text-right space-x-2">
          <button onclick="createSingleFileBackup('${f.id}', '${f.filename}')" title="Create Snapshot" class="p-1 rounded hover:bg-slate-800 text-blue-400">
            <i data-lucide="camera" class="w-3.5 h-3.5"></i>
          </button>
          <button onclick="handleDeleteFile('${f.id}')" title="Delete File" class="p-1 rounded hover:bg-slate-800 text-rose-400">
            <i data-lucide="trash-2" class="w-3.5 h-3.5"></i>
          </button>
        </td>
      </tr>
    `).join('');

    if (window.lucide) lucide.createIcons();
  } catch (err) {
    showToast(`Failed to load files: ${err.message}`, 'error');
  }
}

function debounceFileSearch() {
  clearTimeout(searchDebounceTimer);
  searchDebounceTimer = setTimeout(() => {
    const query = document.getElementById('file-search-input').value.trim();
    loadFilesData(query);
  }, 300);
}

async function handleDeleteFile(fileId) {
  if (!confirm('Are you sure you want to delete this file? It can be restored later.')) return;
  try {
    await apiFetch(`/files/${fileId}`, { method: 'DELETE' });
    showToast('File deleted successfully', 'success');
    loadFilesData();
  } catch (err) {
    showToast(`Delete failed: ${err.message}`, 'error');
  }
}

function openUploadModal() {
  document.getElementById('modal-upload').classList.remove('hidden');
}

async function handleUploadSubmit(e) {
  e.preventDefault();
  const folder = document.getElementById('upload-folder-input').value.trim();
  const fileInput = document.getElementById('upload-files-input');
  const btn = document.getElementById('btn-upload-submit');

  if (fileInput.files.length === 0) {
    showToast('Please select at least one file.', 'warning');
    return;
  }

  const formData = new FormData();
  for (let i = 0; i < fileInput.files.length; i++) {
    formData.append('files', fileInput.files[i]);
  }
  formData.append('folderPath', folder);

  try {
    btn.disabled = true;
    btn.innerText = 'Uploading...';

    await apiFetch('/files/upload', {
      method: 'POST',
      body: formData
    });

    showToast('Files uploaded to cloud storage successfully!', 'success');
    closeModal('modal-upload');
    fileInput.value = '';
    loadFilesData();
  } catch (err) {
    showToast(`Upload failed: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerText = 'Upload Files';
  }
}

// VIEW 3: BACKUP SNAPSHOTS
async function loadBackupsData() {
  try {
    const res = await apiFetch('/backups?page=0&size=50');
    const backups = res.data.content;
    const tbody = document.getElementById('backups-table-body');
    if (!tbody) return;

    if (backups.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="py-8 text-center text-slate-500">No backup snapshots found. Click "Create New Backup" to initiate a snapshot.</td></tr>`;
      return;
    }

    tbody.innerHTML = backups.map(b => `
      <tr class="hover:bg-slate-900/60 transition">
        <td class="py-3 px-4 font-medium text-slate-200">
          <div>${b.backupName}</div>
          <div class="text-[11px] text-slate-500">${b.description || 'No description'}</div>
        </td>
        <td class="py-3 px-4">
          <span class="px-2 py-0.5 rounded text-[10px] font-semibold ${getStatusBadgeClass(b.status)}">
            ${b.status}
          </span>
        </td>
        <td class="py-3 px-4 font-mono">${b.totalFiles}</td>
        <td class="py-3 px-4 font-mono">${formatBytes(b.totalSizeBytes)}</td>
        <td class="py-3 px-4 text-slate-400 text-[11px]">${formatDate(b.startedAt)}</td>
        <td class="py-3 px-4 text-slate-400 text-[11px]">${b.completedAt ? formatDate(b.completedAt) : 'In progress...'}</td>
        <td class="py-3 px-4 text-right space-x-2">
          <button onclick="handleDeleteBackup('${b.id}')" title="Delete Snapshot & Blobs" class="p-1 rounded hover:bg-slate-800 text-rose-400">
            <i data-lucide="trash-2" class="w-3.5 h-3.5"></i>
          </button>
        </td>
      </tr>
    `).join('');

    if (window.lucide) lucide.createIcons();
  } catch (err) {
    showToast(`Failed to load backups: ${err.message}`, 'error');
  }
}

async function handleDeleteBackup(backupId) {
  if (!confirm('Are you sure you want to delete this backup and purge its cloud storage blobs?')) return;
  try {
    await apiFetch(`/backups/${backupId}`, { method: 'DELETE' });
    showToast('Backup purged successfully', 'success');
    loadBackupsData();
  } catch (err) {
    showToast(`Delete failed: ${err.message}`, 'error');
  }
}

async function openNewBackupModal() {
  const modal = document.getElementById('modal-new-backup');
  modal.classList.remove('hidden');

  const container = document.getElementById('backup-files-checklist');
  container.innerHTML = `<p class="text-slate-500 text-center py-3">Loading available files...</p>`;

  try {
    const res = await apiFetch('/files?size=100');
    const files = res.data.content;
    cachedFiles = files;

    if (files.length === 0) {
      container.innerHTML = `<p class="text-amber-400 text-center py-3">No files uploaded yet. Please upload files before taking a backup snapshot.</p>`;
      return;
    }

    container.innerHTML = files.map(f => `
      <label class="flex items-center space-x-2 p-1.5 rounded hover:bg-slate-900 cursor-pointer">
        <input type="checkbox" name="backup-file" value="${f.id}" checked class="rounded border-slate-700 bg-slate-800 text-blue-600 focus:ring-0">
        <span class="text-slate-300 font-medium">${f.filename}</span>
        <span class="text-[10px] text-slate-500 font-mono">(${formatBytes(f.sizeBytes)})</span>
      </label>
    `).join('');
  } catch (err) {
    container.innerHTML = `<p class="text-rose-400 text-center py-3">Error loading files: ${err.message}</p>`;
  }
}

async function handleCreateBackup(e) {
  e.preventDefault();
  const name = document.getElementById('backup-name-input').value.trim();
  const desc = document.getElementById('backup-desc-input').value.trim();
  const checkboxes = document.querySelectorAll('input[name="backup-file"]:checked');
  const btn = document.getElementById('btn-create-backup-submit');

  const fileIds = Array.from(checkboxes).map(cb => cb.value);
  if (fileIds.length === 0) {
    showToast('Please select at least one file to include in the backup snapshot.', 'warning');
    return;
  }

  try {
    btn.disabled = true;
    btn.innerText = 'Creating Snapshot & Hashing...';

    await apiFetch('/backups', {
      method: 'POST',
      body: JSON.stringify({
        backupName: name,
        description: desc,
        fileIds: fileIds
      })
    });

    showToast('Backup snapshot completed successfully with SHA-256 signatures!', 'success');
    closeModal('modal-new-backup');
    loadBackupsData();
    loadDashboardData();
  } catch (err) {
    showToast(`Backup failed: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerText = 'Take Snapshot';
  }
}

function createSingleFileBackup(fileId, filename) {
  openNewBackupModal().then(() => {
    document.getElementById('backup-name-input').value = `Snapshot: ${filename}`;
    const allCbs = document.querySelectorAll('input[name="backup-file"]');
    allCbs.forEach(cb => {
      cb.checked = (cb.value === fileId);
    });
  });
}

// VIEW 4: CRYPTOGRAPHIC INTEGRITY & POINT-IN-TIME VERSIONS
async function loadIntegrityData() {
  try {
    // Gather versions across all backups
    const backupsRes = await apiFetch('/backups?page=0&size=20');
    const backups = backupsRes.data.content;
    const versionList = [];

    for (const b of backups) {
      try {
        const detailRes = await apiFetch(`/backups/${b.id}`);
        if (detailRes.data && detailRes.data.versions) {
          versionList.push(...detailRes.data.versions);
        }
      } catch (e) {
        console.warn('Could not fetch backup details', e);
      }
    }

    cachedVersions = versionList;
    const tbody = document.getElementById('versions-table-body');
    if (!tbody) return;

    if (versionList.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="py-8 text-center text-slate-500">No backup versions found yet. Run a backup snapshot to create cryptographic versions.</td></tr>`;
      return;
    }

    tbody.innerHTML = versionList.map(v => `
      <tr class="hover:bg-slate-900/60 transition">
        <td class="py-3 px-4 font-medium text-slate-200">
          <div>${v.filename || 'File Version'}</div>
          <div class="text-[10px] text-slate-500 font-mono">${v.storageKey}</div>
        </td>
        <td class="py-3 px-4 font-mono font-semibold text-blue-400">v${v.versionNumber}</td>
        <td class="py-3 px-4">
          <span class="hash-text bg-slate-900 px-2 py-1 rounded text-slate-300 border border-slate-800" title="${v.checksumSha256}">
            ${v.checksumSha256.substring(0, 16)}...${v.checksumSha256.substring(48)}
          </span>
        </td>
        <td class="py-3 px-4 font-mono">${formatBytes(v.sizeBytes)}</td>
        <td class="py-3 px-4">
          <span class="px-2 py-0.5 rounded text-[10px] font-semibold ${v.status === 'CORRUPTED' ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20' : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'}">
            ${v.status === 'CORRUPTED' ? 'TAMPERED / CORRUPTED' : 'INTEGRITY VERIFIED'}
          </span>
        </td>
        <td class="py-3 px-4 text-right space-x-2">
          <button onclick="runIntegrityCheck('${v.id}')" title="Verify SHA-256 Checksum" class="px-2.5 py-1 rounded-lg bg-blue-600/10 hover:bg-blue-600/20 text-blue-400 text-xs font-medium border border-blue-500/20 transition">
            Verify Hash
          </button>
          <button onclick="openRestoreModal('${v.id}', '${v.filename}', ${v.versionNumber})" title="Restore Version" class="px-2.5 py-1 rounded-lg bg-emerald-600/10 hover:bg-emerald-600/20 text-emerald-400 text-xs font-medium border border-emerald-500/20 transition">
            Restore
          </button>
          <a href="${API_BASE}/versions/${v.id}/download" download title="Download Snapshot" class="p-1 rounded inline-block hover:bg-slate-800 text-slate-300 align-middle">
            <i data-lucide="download" class="w-3.5 h-3.5"></i>
          </a>
        </td>
      </tr>
    `).join('');

    if (window.lucide) lucide.createIcons();
  } catch (err) {
    showToast(`Integrity catalog error: ${err.message}`, 'error');
  }
}

async function runIntegrityCheck(versionId) {
  try {
    showToast('Executing cryptographic SHA-256 check on cloud storage blob...', 'info');
    const res = await apiFetch(`/recovery/verify-integrity/${versionId}`, { method: 'POST' });
    const result = res.data;

    const modal = document.getElementById('modal-integrity-result');
    const content = document.getElementById('integrity-result-content');

    const statusBadge = result.match
      ? `<span class="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">PASS - ZERO CORRUPTION</span>`
      : `<span class="px-2.5 py-1 rounded-full text-xs font-bold bg-rose-500/10 text-rose-400 border border-rose-500/30">FAIL - DATA TAMPER DETECTED</span>`;

    content.innerHTML = `
      <div class="flex items-center justify-between pb-2 border-b border-slate-800">
        <div>
          <span class="text-slate-400">Target File:</span>
          <span class="font-bold text-slate-100 ml-1">${result.filename}</span>
        </div>
        ${statusBadge}
      </div>
      <div>
        <label class="block text-slate-400 mb-1">Expected / Stored SHA-256 Digest:</label>
        <div class="p-2.5 rounded-lg bg-slate-950 border border-slate-800 font-mono text-slate-200 text-xs break-all">${result.storedChecksum}</div>
      </div>
      <div>
        <label class="block text-slate-400 mb-1">Live Recalculated Cloud Blob Digest:</label>
        <div class="p-2.5 rounded-lg bg-slate-950 border border-slate-800 font-mono text-slate-200 text-xs break-all ${result.match ? 'text-emerald-400' : 'text-rose-400'}">${result.computedChecksum}</div>
      </div>
      <div class="p-3 rounded-lg ${result.match ? 'bg-emerald-950/30 border border-emerald-500/20 text-emerald-300' : 'bg-rose-950/30 border border-rose-500/20 text-rose-300'}">
        ${result.match
          ? 'Byte-for-byte cryptographic integrity is fully intact. File is safe for disaster restoration.'
          : 'Warning: Hash mismatch detected! Cloud blob has been tampered with or corrupted on disk.'}
      </div>
    `;

    modal.classList.remove('hidden');
    loadIntegrityData();
  } catch (err) {
    showToast(`Verification failed: ${err.message}`, 'error');
  }
}

function openRestoreModal(versionId, filename, versionNumber) {
  document.getElementById('restore-version-id').value = versionId;
  document.getElementById('restore-version-label').innerText = `${filename} (Version #${versionNumber})`;
  document.getElementById('modal-restore').classList.remove('hidden');
}

async function handleExecuteRestore(e) {
  e.preventDefault();
  const versionId = document.getElementById('restore-version-id').value;
  const destinationPath = document.getElementById('restore-destination-input').value.trim();
  const btn = document.getElementById('btn-restore-submit');

  try {
    btn.disabled = true;
    btn.innerText = 'Verifying & Restoring...';

    const res = await apiFetch(`/recovery/restore-version/${versionId}?destinationPath=${encodeURIComponent(destinationPath)}`, {
      method: 'POST'
    });

    showToast(`Snapshot restored successfully to ${destinationPath}`, 'success');
    closeModal('modal-restore');
    loadIntegrityData();
    loadDashboardData();
  } catch (err) {
    showToast(`Restoration aborted: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.innerText = 'Authorize & Restore';
  }
}

// VIEW 5: SCHEDULES & AUTOMATION
async function loadSchedulesData() {
  try {
    const res = await apiFetch('/schedules');
    const schedules = res.data;
    const tbody = document.getElementById('schedules-table-body');
    if (!tbody) return;

    if (schedules.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="py-8 text-center text-slate-500">No automation schedules defined. Click "Create Schedule" to setup automated cloud backups.</td></tr>`;
      return;
    }

    tbody.innerHTML = schedules.map(s => `
      <tr class="hover:bg-slate-900/60 transition">
        <td class="py-3 px-4 font-medium text-slate-200">${s.scheduleName}</td>
        <td class="py-3 px-4">
          <span class="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-800 text-blue-400 border border-slate-700">
            ${s.frequency}
          </span>
          <span class="block text-[10px] text-slate-500 font-mono mt-0.5">${s.cronExpression}</span>
        </td>
        <td class="py-3 px-4 font-mono text-[11px] text-slate-400">${s.targetDirectory}</td>
        <td class="py-3 px-4">
          <button onclick="handleToggleSchedule('${s.id}')" class="px-2.5 py-0.5 rounded-full text-[10px] font-bold ${s.isActive ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' : 'bg-slate-800 text-slate-400 border border-slate-700'}">
            ${s.isActive ? 'ACTIVE' : 'PAUSED'}
          </button>
        </td>
        <td class="py-3 px-4 text-slate-400 text-[11px]">${s.lastRunAt ? formatDate(s.lastRunAt) : 'Never'}</td>
        <td class="py-3 px-4 text-slate-300 text-[11px] font-mono">${s.nextRunAt ? formatDate(s.nextRunAt) : 'N/A'}</td>
        <td class="py-3 px-4 text-right space-x-2">
          <button onclick="handleTriggerScheduleNow('${s.id}')" title="Trigger Instant Backup" class="px-2.5 py-1 rounded-lg bg-blue-600/10 hover:bg-blue-600/20 text-blue-400 text-xs font-medium border border-blue-500/20 transition">
            Run Now
          </button>
          <button onclick="handleDeleteSchedule('${s.id}')" title="Delete Schedule" class="p-1 rounded hover:bg-slate-800 text-rose-400">
            <i data-lucide="trash-2" class="w-3.5 h-3.5"></i>
          </button>
        </td>
      </tr>
    `).join('');

    if (window.lucide) lucide.createIcons();
  } catch (err) {
    showToast(`Failed to load schedules: ${err.message}`, 'error');
  }
}

function openNewScheduleModal() {
  document.getElementById('modal-schedule').classList.remove('hidden');
}

function toggleCustomCronInput() {
  const freq = document.getElementById('sched-freq').value;
  const box = document.getElementById('sched-custom-cron-box');
  if (freq === 'CUSTOM') {
    box.classList.remove('hidden');
  } else {
    box.classList.add('hidden');
  }
}

async function handleCreateSchedule(e) {
  e.preventDefault();
  const name = document.getElementById('sched-name').value.trim();
  const freq = document.getElementById('sched-freq').value;
  const target = document.getElementById('sched-target').value.trim();
  const customCron = document.getElementById('sched-cron').value.trim();

  try {
    await apiFetch('/schedules', {
      method: 'POST',
      body: JSON.stringify({
        scheduleName: name,
        frequency: freq,
        cronExpression: freq === 'CUSTOM' ? customCron : undefined,
        targetDirectory: target
      })
    });

    showToast('Automation schedule created successfully!', 'success');
    closeModal('modal-schedule');
    loadSchedulesData();
    loadDashboardData();
  } catch (err) {
    showToast(`Failed to create schedule: ${err.message}`, 'error');
  }
}

async function handleToggleSchedule(id) {
  try {
    await apiFetch(`/schedules/${id}/toggle`, { method: 'PATCH' });
    showToast('Schedule state updated', 'info');
    loadSchedulesData();
    loadDashboardData();
  } catch (err) {
    showToast(`Toggle failed: ${err.message}`, 'error');
  }
}

async function handleTriggerScheduleNow(id) {
  try {
    showToast('Triggering scheduled backup job...', 'info');
    await apiFetch(`/schedules/${id}/trigger-now`, { method: 'POST' });
    showToast('Scheduled backup job executed successfully!', 'success');
    loadSchedulesData();
    loadDashboardData();
  } catch (err) {
    showToast(`Execution failed: ${err.message}`, 'error');
  }
}

async function handleDeleteSchedule(id) {
  if (!confirm('Are you sure you want to delete this schedule?')) return;
  try {
    await apiFetch(`/schedules/${id}`, { method: 'DELETE' });
    showToast('Schedule deleted', 'success');
    loadSchedulesData();
    loadDashboardData();
  } catch (err) {
    showToast(`Delete failed: ${err.message}`, 'error');
  }
}

// VIEW 6: RECOVERY AUDIT LOGS
async function loadRecoveryHistory() {
  try {
    const res = await apiFetch('/recovery/history?page=0&size=50');
    const logs = res.data.content;
    const tbody = document.getElementById('recovery-table-body');
    if (!tbody) return;

    if (logs.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="py-8 text-center text-slate-500">No disaster recovery events recorded yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = logs.map(l => `
      <tr class="hover:bg-slate-900/60 transition">
        <td class="py-3 px-4 text-slate-300 font-mono text-[11px]">${formatDate(l.recoveredAt)}</td>
        <td class="py-3 px-4 font-medium text-slate-200">
          <div>${l.filename || 'Restored File'}</div>
          <div class="text-[10px] text-slate-500 font-mono">v${l.versionNumber || 1} • ${l.backupName || 'Backup'}</div>
        </td>
        <td class="py-3 px-4 font-mono text-[11px] text-slate-400">${l.destinationPath}</td>
        <td class="py-3 px-4">
          <span class="px-2 py-0.5 rounded text-[10px] font-semibold ${l.checksumVerified ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'}">
            ${l.checksumVerified ? 'PASSED (SHA-256)' : 'FAILED'}
          </span>
        </td>
        <td class="py-3 px-4">
          <span class="px-2 py-0.5 rounded text-[10px] font-semibold ${getStatusBadgeClass(l.status)}">
            ${l.status}
          </span>
        </td>
        <td class="py-3 px-4 text-slate-400 text-[11px]">
          ${l.errorMessage ? `<span class="text-rose-400">${l.errorMessage}</span>` : '<span class="text-emerald-400">Restore confirmed</span>'}
        </td>
      </tr>
    `).join('');

    if (window.lucide) lucide.createIcons();
  } catch (err) {
    showToast(`Failed to load recovery audit logs: ${err.message}`, 'error');
  }
}

// NOTIFICATIONS SYSTEM
async function loadNotifications() {
  try {
    const [listRes, countRes] = await Promise.all([
      apiFetch('/notifications'),
      apiFetch('/notifications/unread-count')
    ]);

    const notifications = listRes.data;
    const unreadCount = countRes.data.unreadCount;

    const badge = document.getElementById('unread-badge');
    if (badge) {
      if (unreadCount > 0) {
        badge.innerText = unreadCount > 99 ? '99+' : unreadCount;
        badge.classList.remove('hidden');
      } else {
        badge.classList.add('hidden');
      }
    }

    const listEl = document.getElementById('notification-list');
    if (!listEl) return;

    if (notifications.length === 0) {
      listEl.innerHTML = `<p class="text-slate-500 text-center py-4">No notifications yet.</p>`;
      return;
    }

    listEl.innerHTML = notifications.slice(0, 10).map(n => `
      <div class="py-2.5 px-1 hover:bg-slate-800/40 cursor-pointer transition ${!n.isRead ? 'bg-blue-950/20' : ''}" onclick="markNotificationRead('${n.id}')">
        <div class="flex items-center justify-between">
          <span class="font-semibold text-slate-200 text-xs">${n.title}</span>
          <span class="text-[10px] text-slate-500">${formatDate(n.createdAt)}</span>
        </div>
        <p class="text-slate-400 text-[11px] mt-0.5">${n.message}</p>
      </div>
    `).join('');

  } catch (e) {
    console.debug('Notification fetch error', e);
  }
}

function toggleNotificationDropdown() {
  const menu = document.getElementById('notification-dropdown');
  if (menu) menu.classList.toggle('hidden');
}

async function markNotificationRead(id) {
  try {
    await apiFetch(`/notifications/${id}/read`, { method: 'PATCH' });
    loadNotifications();
  } catch (e) {}
}

async function markAllNotificationsRead() {
  try {
    await apiFetch('/notifications/read-all', { method: 'POST' });
    loadNotifications();
    showToast('All notifications marked as read', 'info');
  } catch (e) {}
}

// Utility Helpers
function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.add('hidden');
}

function formatBytes(bytes = 0) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(isoStr) {
  if (!isoStr) return '';
  const d = new Date(isoStr);
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function getStatusBadgeClass(status) {
  switch (status) {
    case 'COMPLETED':
    case 'SUCCESS':
      return 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20';
    case 'IN_PROGRESS':
    case 'INITIATED':
      return 'bg-blue-500/10 text-blue-400 border border-blue-500/20';
    case 'FAILED':
    case 'INTEGRITY_FAILED':
    case 'CORRUPTED':
      return 'bg-rose-500/10 text-rose-400 border border-rose-500/20';
    case 'PENDING':
    default:
      return 'bg-slate-800 text-slate-400 border border-slate-700';
  }
}

