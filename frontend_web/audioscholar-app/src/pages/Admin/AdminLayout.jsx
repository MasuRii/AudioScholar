import React from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';

const AdminLayout = () => {
  const location = useLocation();
  
  const isActive = (path) => location.pathname === path;

  return (
    <div className="min-h-screen bg-gray-50">
       <header className="bg-[#1A365D] text-white shadow-sm">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <div className="flex items-center space-x-8">
            <Link to="/admin" className="text-xl font-bold">Admin Portal</Link>
            <nav className="hidden md:flex space-x-1">
              <Link 
                to="/admin" 
                className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive('/admin') ? 'bg-[#2D8A8A] text-white' : 'text-gray-300 hover:text-white hover:bg-[#2a4c7d]'
                }`}
              >
                Dashboard
              </Link>
              <Link 
                to="/admin/users" 
                className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive('/admin/users') ? 'bg-[#2D8A8A] text-white' : 'text-gray-300 hover:text-white hover:bg-[#2a4c7d]'
                }`}
              >
                User Management
              </Link>
              <Link 
                to="/admin/analytics" 
                className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive('/admin/analytics') ? 'bg-[#2D8A8A] text-white' : 'text-gray-300 hover:text-white hover:bg-[#2a4c7d]'
                }`}
              >
                Analytics
              </Link>
            </nav>
          </div>
          <Link to="/dashboard" className="text-sm text-gray-300 hover:text-white">
            Exit to App
          </Link>
        </div>
      </header>

      <main className="container mx-auto px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
};

export default AdminLayout;
