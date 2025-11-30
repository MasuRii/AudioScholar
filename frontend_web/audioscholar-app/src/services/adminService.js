const API_BASE_URL = 'http://localhost:8080/api/admin';

const getAuthHeaders = () => {
  const token = localStorage.getItem('AuthToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
};

export const adminService = {
  // User Management
  getUsers: async (limit = 20, startAfter = null) => {
    const params = new URLSearchParams({ limit });
    if (startAfter) params.append('startAfter', startAfter);
    
    const response = await fetch(`${API_BASE_URL}/users?${params.toString()}`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch users');
    return await response.json();
  },

  updateUserStatus: async (uid, disabled) => {
    const response = await fetch(`${API_BASE_URL}/users/${uid}/status`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ disabled }),
    });
    if (!response.ok) throw new Error('Failed to update user status');
  },

  updateUserRoles: async (uid, roles) => {
    const response = await fetch(`${API_BASE_URL}/users/${uid}/roles`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ roles }),
    });
    if (!response.ok) throw new Error('Failed to update user roles');
  },

  // Analytics
  getOverview: async () => {
    const response = await fetch(`${API_BASE_URL}/analytics/overview`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch overview stats');
    return await response.json();
  },

  getActivityStats: async () => {
    const response = await fetch(`${API_BASE_URL}/analytics/activity`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch activity stats');
    return await response.json();
  },

  getUserDistribution: async () => {
    const response = await fetch(`${API_BASE_URL}/analytics/users`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch user distribution');
    return await response.json();
  },

  getContentEngagement: async () => {
    const response = await fetch(`${API_BASE_URL}/analytics/content`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch content engagement');
    return await response.json();
  },
};
