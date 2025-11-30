import React, { useEffect, useState } from 'react';
import { adminService } from '../../../services/adminService';

const AdminUserList = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null); // 'uid-action'

  const fetchUsers = async () => {
    try {
      setLoading(true);
      // Fetching all users for now. Pagination can be added if list grows large.
      const data = await adminService.getUsers(100);
      setUsers(data);
    } catch (err) {
      console.error("Failed to fetch users:", err);
      setError("Failed to load users.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleToggleStatus = async (uid, currentStatus) => {
    if (!window.confirm(`Are you sure you want to ${currentStatus ? 'enable' : 'disable'} this user?`)) return;

    try {
      setActionLoading(`${uid}-status`);
      await adminService.updateUserStatus(uid, !currentStatus);
      setUsers(users.map(u => u.uid === uid ? { ...u, disabled: !currentStatus } : u));
    } catch (err) {
      alert("Failed to update user status.");
    } finally {
      setActionLoading(null);
    }
  };

  const handleToggleAdmin = async (user) => {
    const isAdmin = user.roles.includes('ROLE_ADMIN');
    const action = isAdmin ? 'remove' : 'add';
    if (!window.confirm(`Are you sure you want to ${action} ADMIN privileges for ${user.displayName}?`)) return;

    try {
      setActionLoading(`${user.uid}-role`);
      let newRoles = [...user.roles];
      if (isAdmin) {
        newRoles = newRoles.filter(r => r !== 'ROLE_ADMIN');
      } else {
        newRoles.push('ROLE_ADMIN');
      }
      
      await adminService.updateUserRoles(user.uid, newRoles);
      setUsers(users.map(u => u.uid === user.uid ? { ...u, roles: newRoles } : u));
    } catch (err) {
      alert("Failed to update user roles.");
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) return <div className="text-center py-10">Loading users...</div>;
  if (error) return <div className="text-center py-10 text-red-600">{error}</div>;

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">User Management</h1>

      <div className="bg-white rounded-lg shadow overflow-hidden border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Roles</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {users.map((user) => (
              <tr key={user.uid} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="flex-shrink-0 h-10 w-10">
                      {user.photoUrl ? (
                        <img className="h-10 w-10 rounded-full object-cover" src={user.photoUrl} alt="" />
                      ) : (
                        <div className="h-10 w-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 text-lg font-bold">
                          {user.displayName?.charAt(0) || user.email?.charAt(0)}
                        </div>
                      )}
                    </div>
                    <div className="ml-4">
                      <div className="text-sm font-medium text-gray-900">{user.displayName || 'No Name'}</div>
                      <div className="text-sm text-gray-500">{user.email}</div>
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                    user.disabled ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'
                  }`}>
                    {user.disabled ? 'Disabled' : 'Active'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  <div className="flex flex-wrap gap-1">
                    {user.roles.map(role => (
                      <span key={role} className={`px-2 py-0.5 rounded text-xs ${
                        role === 'ROLE_ADMIN' ? 'bg-purple-100 text-purple-800 border border-purple-200' : 'bg-gray-100 text-gray-800 border border-gray-200'
                      }`}>
                        {role.replace('ROLE_', '')}
                      </span>
                    ))}
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button
                    onClick={() => handleToggleAdmin(user)}
                    disabled={!!actionLoading}
                    className="text-indigo-600 hover:text-indigo-900 mr-4 disabled:opacity-50"
                  >
                    {user.roles.includes('ROLE_ADMIN') ? 'Remove Admin' : 'Make Admin'}
                  </button>
                  <button
                    onClick={() => handleToggleStatus(user.uid, user.disabled)}
                    disabled={!!actionLoading}
                    className={`${user.disabled ? 'text-green-600 hover:text-green-900' : 'text-red-600 hover:text-red-900'} disabled:opacity-50`}
                  >
                    {user.disabled ? 'Enable' : 'Disable'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {users.length === 0 && (
          <div className="text-center py-8 text-gray-500">No users found.</div>
        )}
      </div>
    </div>
  );
};

export default AdminUserList;
