import React, { useEffect, useState } from 'react';
import { adminService } from '../../../services/adminService';

const SimpleBarChart = ({ data, title, colorClass }) => {
  if (!data || Object.keys(data).length === 0) return <div className="text-gray-500 italic">No data available</div>;

  // Sort keys (dates)
  const sortedKeys = Object.keys(data).sort();
  const maxVal = Math.max(...Object.values(data));

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
      <h3 className="text-lg font-bold text-gray-800 mb-4">{title}</h3>
      <div className="space-y-2">
        {sortedKeys.map(key => {
          const val = data[key];
          const percentage = maxVal > 0 ? (val / maxVal) * 100 : 0;
          return (
            <div key={key} className="flex items-center text-sm">
              <span className="w-24 text-gray-500 truncate">{key}</span>
              <div className="flex-1 mx-3 h-4 bg-gray-100 rounded overflow-hidden">
                <div 
                  className={`h-full rounded ${colorClass}`} 
                  style={{ width: `${percentage}%` }}
                ></div>
              </div>
              <span className="w-8 text-right font-medium text-gray-700">{val}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
};

const AdminAnalytics = () => {
  const [activity, setActivity] = useState(null);
  const [distribution, setDistribution] = useState(null);
  const [engagement, setEngagement] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [activityData, distData, engData] = await Promise.all([
          adminService.getActivityStats(),
          adminService.getUserDistribution(),
          adminService.getContentEngagement()
        ]);
        
        setActivity(activityData);
        setDistribution(distData);
        setEngagement(engData);
      } catch (err) {
        console.error("Failed to fetch analytics:", err);
        setError("Failed to load analytics data.");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) return <div className="text-center py-10">Loading analytics...</div>;
  if (error) return <div className="text-center py-10 text-red-600">{error}</div>;

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-bold text-gray-800">Platform Analytics</h1>

      {/* Activity Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <SimpleBarChart 
          title="New Users (Last 30 Days)" 
          data={activity?.newUsersLast30Days} 
          colorClass="bg-blue-500" 
        />
        <SimpleBarChart 
          title="New Recordings (Last 30 Days)" 
          data={activity?.newRecordingsLast30Days} 
          colorClass="bg-teal-500" 
        />
      </div>

      {/* Distribution Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
          <h3 className="text-lg font-bold text-gray-800 mb-4">User Roles</h3>
          <ul className="divide-y divide-gray-100">
            {distribution?.usersByRole && Object.entries(distribution.usersByRole).map(([role, count]) => (
              <li key={role} className="py-2 flex justify-between items-center">
                <span className="text-gray-600">{role.replace('ROLE_', '')}</span>
                <span className="font-bold text-gray-800 bg-gray-100 px-2 py-1 rounded-full text-xs">{count}</span>
              </li>
            ))}
          </ul>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
          <h3 className="text-lg font-bold text-gray-800 mb-4">Login Providers</h3>
          <ul className="divide-y divide-gray-100">
            {distribution?.usersByProvider && Object.entries(distribution.usersByProvider).map(([provider, count]) => (
              <li key={provider} className="py-2 flex justify-between items-center">
                <span className="text-gray-600 capitalize">{provider}</span>
                <span className="font-bold text-gray-800 bg-gray-100 px-2 py-1 rounded-full text-xs">{count}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* Engagement Section */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
        <h3 className="text-lg font-bold text-gray-800 mb-4">Top Content (By Favorites)</h3>
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm text-left text-gray-500">
            <thead className="text-xs text-gray-700 uppercase bg-gray-50">
              <tr>
                <th className="px-6 py-3">Title</th>
                <th className="px-6 py-3 text-right">Favorites</th>
              </tr>
            </thead>
            <tbody>
              {engagement && engagement.length > 0 ? (
                engagement.map((item) => (
                  <tr key={item.recordingId} className="bg-white border-b hover:bg-gray-50">
                    <td className="px-6 py-4 font-medium text-gray-900 whitespace-nowrap">
                      {item.title || 'Untitled Recording'}
                    </td>
                    <td className="px-6 py-4 text-right">
                      {item.favoriteCount}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="2" className="px-6 py-4 text-center">No engagement data available.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default AdminAnalytics;
