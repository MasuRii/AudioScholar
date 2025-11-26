import axios from 'axios';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { FiAlertTriangle, FiCheckCircle, FiClock, FiExternalLink, FiFile, FiLoader, FiTrash2, FiUploadCloud, FiSearch, FiRefreshCw } from 'react-icons/fi';
import { Link, useNavigate } from 'react-router-dom';
import { API_BASE_URL } from '../../services/authService';
import { Header } from '../Home/HomePage';

const TERMINAL_STATUSES = ['COMPLETE', 'COMPLETED', 'FAILED', 'PROCESSING_HALTED_UNSUITABLE_CONTENT', 'PROCESSING_HALTED_NO_SPEECH', 'SUMMARY_FAILED', 'COMPLETED_WITH_WARNINGS'];
const UPLOADING_STATUSES = ['UPLOAD_PENDING', 'UPLOAD_IN_PROGRESS', 'UPLOADING_TO_STORAGE', 'UPLOADED'];
const PROCESSING_STATUSES = [
  'PROCESSING_QUEUED',
  'TRANSCRIBING',
  'PDF_CONVERTING',
  'PDF_CONVERTING_API',
  'TRANSCRIPTION_COMPLETE',
  'PDF_CONVERSION_COMPLETE',
  'SUMMARIZATION_QUEUED',
  'SUMMARIZING',
  'SUMMARY_COMPLETE',
  'RECOMMENDATIONS_QUEUED',
  'GENERATING_RECOMMENDATIONS',
  'PROCESSING'
];
const UPLOAD_TIMEOUT_SECONDS = 10 * 60;

const RecordingList = () => {
    const [recordings, setRecordings] = useState([]);
    const [filteredRecordings, setFilteredRecordings] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();
    const pollIntervalRef = useRef(null);
    const isMountedRef = useRef(true);
    const deletedIdsRef = useRef(new Set());

    // ... (existing fetchRecordings, startPolling, useEffect, handleDelete, handleDeleteAllRecordings, formatDate, getStatusBadge code remains same until return)

    const fetchRecordings = useCallback(async () => {
        console.log("Fetching recordings...");
        setLoading(true); // Ensure loading state is set when retrying
        setError(null);
        const token = localStorage.getItem('AuthToken');
        if (!token) {
            setError("User not authenticated. Please log in.");
            setLoading(false);
            navigate('/signin');
            return null;
        }

        try {
            const url = `${API_BASE_URL}api/audio/metadata`;
            const response = await axios.get(url, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            // Filter out locally deleted items
            const validRecordings = response.data.filter(rec => !deletedIdsRef.current.has(rec.id));

            const sortedRecordings = validRecordings.sort((a, b) =>
                (b.uploadTimestamp?.seconds ?? 0) - (a.uploadTimestamp?.seconds ?? 0)
            );

            console.log("Fetched recordings:", sortedRecordings);
            setRecordings(sortedRecordings);
            setFilteredRecordings(sortedRecordings);
            return sortedRecordings;

        } catch (err) {
            console.error('Error fetching recordings:', err);
            if (err.response && (err.response.status === 401 || err.response.status === 403)) {
                setError("Session expired or not authorized. Please log in again.");
                localStorage.removeItem('AuthToken');
                navigate('/signin');
            } else {
                setError('Failed to fetch recordings. Please check your connection and try again.');
            }
            return null;
        } finally {
            setLoading(false);
        }
    }, [navigate]);

    const startPolling = useCallback((initialData) => {
        if (pollIntervalRef.current) {
            clearInterval(pollIntervalRef.current);
            pollIntervalRef.current = null;
            console.log("Cleared existing poll interval");
        }

        const needsPolling = initialData?.some(rec => {
            const statusUpper = rec.status?.toUpperCase();
            const isTerminal = TERMINAL_STATUSES.includes(statusUpper);
            if (isTerminal) return false;

            const isUploading = UPLOADING_STATUSES.includes(statusUpper);
            if (isUploading) {
                const elapsedSeconds = rec.uploadTimestamp?.seconds
                    ? (Date.now() / 1000) - rec.uploadTimestamp.seconds
                    : 0;
                if (elapsedSeconds > UPLOAD_TIMEOUT_SECONDS) {
                    console.log(`[Polling Check - ${rec.id}] Upload status ${statusUpper} timed out (${Math.round(elapsedSeconds)}s > ${UPLOAD_TIMEOUT_SECONDS}s). No polling needed for this item.`);
                    return false;
                }
            }
            return true;
        });

        if (!needsPolling) {
            console.log("No recordings require further status checks (all terminal or timed-out uploads). Polling not started.");
            return;
        }

        console.log("Starting polling for recordings not in a terminal or timed-out upload state...");
        pollIntervalRef.current = setInterval(async () => {
            if (!isMountedRef.current) {
                console.log("Component unmounted, stopping poll interval.");
                clearInterval(pollIntervalRef.current);
                pollIntervalRef.current = null;
                return;
            }
            console.log("Polling for updates...");
            // Silent fetch for polling (don't set global loading)
            const token = localStorage.getItem('AuthToken');
            if (!token) return;

            try {
                const response = await axios.get(`${API_BASE_URL}api/audio/metadata`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                const validRecordings = response.data.filter(rec => !deletedIdsRef.current.has(rec.id));
                const sortedRecordings = validRecordings.sort((a, b) =>
                    (b.uploadTimestamp?.seconds ?? 0) - (a.uploadTimestamp?.seconds ?? 0)
                );
                
                if (isMountedRef.current) {
                    setRecordings(sortedRecordings);
                    // Update polling status
                     const stillNeedsPolling = sortedRecordings.some(rec => {
                        const statusUpper = rec.status?.toUpperCase();
                        const isTerminal = TERMINAL_STATUSES.includes(statusUpper);
                        if (isTerminal) return false;

                        const isUploading = UPLOADING_STATUSES.includes(statusUpper);
                        if (isUploading) {
                            const elapsedSeconds = rec.uploadTimestamp?.seconds
                                ? (Date.now() / 1000) - rec.uploadTimestamp.seconds
                                : 0;
                            if (elapsedSeconds > UPLOAD_TIMEOUT_SECONDS) return false;
                        }
                        return true;
                    });

                    if (!stillNeedsPolling) {
                        clearInterval(pollIntervalRef.current);
                        pollIntervalRef.current = null;
                    }
                }
            } catch (err) {
                console.error("Error during poll:", err);
            }
        }, 15000);

    }, []);

    useEffect(() => {
        isMountedRef.current = true;
        
        const cachedRecordings = localStorage.getItem('recording_list');
        if (cachedRecordings) {
            const parsed = JSON.parse(cachedRecordings);
            setRecordings(parsed);
            setFilteredRecordings(parsed); 
        }

        fetchRecordings().then(initialData => {
            if (initialData && isMountedRef.current) {
                localStorage.setItem('recording_list', JSON.stringify(initialData));
                startPolling(initialData);
            }
        });

        return () => {
            console.log("RecordingList component unmounting, clearing interval.");
            isMountedRef.current = false;
            if (pollIntervalRef.current) {
                clearInterval(pollIntervalRef.current);
                pollIntervalRef.current = null;
            }
        };
    }, [fetchRecordings, startPolling]);

    // Update filtered recordings when search query or recordings change
    useEffect(() => {
        const lowerQuery = searchQuery.toLowerCase();
        const filtered = recordings.filter(rec => 
            (rec.title && rec.title.toLowerCase().includes(lowerQuery)) ||
            (rec.description && rec.description.toLowerCase().includes(lowerQuery))
        );
        setFilteredRecordings(filtered);
    }, [searchQuery, recordings]);


    const handleDelete = async (idToDelete) => {
        if (!window.confirm('Are you sure you want to delete this recording and its summary? This action cannot be undone.')) {
            return;
        }

        // Optimistic update
        deletedIdsRef.current.add(idToDelete);
        const updatedRecordings = recordings.filter(rec => rec.id !== idToDelete);
        setRecordings(updatedRecordings);

        const token = localStorage.getItem('AuthToken');
        if (!token) {
            // Revert optimistic update if no token
            deletedIdsRef.current.delete(idToDelete);
            fetchRecordings().then(data => data && setRecordings(data));
            setError("Authentication required.");
            navigate('/signin');
            return;
        }

        try {
            const url = `${API_BASE_URL}api/audio/metadata/${idToDelete}`;
            await axios.delete(url, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            console.log(`Recording ${idToDelete} deleted successfully (Backend).`);
        } catch (err) {
            console.error('Error deleting recording:', err);
            // Revert optimistic update on failure
            deletedIdsRef.current.delete(idToDelete);
            fetchRecordings().then(data => data && setRecordings(data));

            if (err.response && (err.response.status === 401 || err.response.status === 403)) {
                setError("Session expired or not authorized. Please log in again.");
                localStorage.removeItem('AuthToken');
                navigate('/signin');
            } else {
                setError('Failed to delete recording. Please try again.');
            }
        }
    };

    const handleDeleteAllRecordings = async () => {
        if (!window.confirm('Are you sure you want to delete ALL your recordings and their summaries? This action cannot be undone.')) {
            return;
        }

        const token = localStorage.getItem('AuthToken');
        if (!token) {
            setError("Authentication required to delete all recordings.");
            navigate('/signin');
            return;
        }

        const recordingsToDelete = [...recordings];
        if (recordingsToDelete.length === 0) {
            setError("No recordings to delete.");
            return;
        }

        // Optimistic Update: Clear UI immediately
        const idsToDelete = recordingsToDelete.map(r => r.id);
        idsToDelete.forEach(id => deletedIdsRef.current.add(id));
        setRecordings([]);
        
        // Stop polling immediately since list is empty
        if (pollIntervalRef.current) {
            clearInterval(pollIntervalRef.current);
            pollIntervalRef.current = null;
        }

        // Background Processing (Fire and Forget)
        const deletePromises = recordingsToDelete.map(recording => {
            const url = `${API_BASE_URL}api/audio/metadata/${recording.id}`;
            return axios.delete(url, { headers: { 'Authorization': `Bearer ${token}` } })
                .then(response => ({ id: recording.id, status: 'fulfilled', response }))
                .catch(error => ({ id: recording.id, status: 'rejected', reason: error }));
        });

        // We do NOT await this to block the UI. We let it run in background.
        Promise.allSettled(deletePromises).then(results => {
            let successfulDeletions = 0;
            let failedDeletions = 0;

            results.forEach(result => {
                if (result.status === 'fulfilled' && (result.value.response.status === 200 || result.value.response.status === 204)) {
                    successfulDeletions++;
                } else {
                    failedDeletions++;
                    const recordingId = result.status === 'fulfilled' ? result.value.id : (result.reason && result.reason.id);
                    console.error(`Failed to delete recording ${recordingId || 'unknown'} in background:`, result.status === 'rejected' ? result.reason : result.value.response);
                }
            });
            console.log(`Background deletion complete. Success: ${successfulDeletions}, Failed: ${failedDeletions}`);
        }).catch(err => {
             console.error('Error in background batch deletion:', err);
        });
    };

    const formatDate = (timestamp) => {
        if (!timestamp?.seconds) return 'N/A';
        return new Date(timestamp.seconds * 1000).toLocaleDateString();
    };

    const getStatusBadge = (recording) => {
        const { status, failureReason, uploadTimestamp } = recording;
        const originalStatus = status;
        const statusUpper = status?.toUpperCase() ?? 'UNKNOWN';
        let bgColor = 'bg-gray-100';
        let textColor = 'text-gray-800';
        let Icon = FiClock;
        let displayStatus = 'Unknown';
        let isSpinning = false;
        let titleText = '';

        const isUploadingOrPending = UPLOADING_STATUSES.includes(statusUpper);
        const isProcessing = PROCESSING_STATUSES.includes(statusUpper);
        const elapsedSeconds = uploadTimestamp?.seconds
            ? (Date.now() / 1000) - uploadTimestamp.seconds
            : 0;
        const isTimedOutUploadDisplay = isUploadingOrPending && elapsedSeconds > UPLOAD_TIMEOUT_SECONDS;

        if (isTimedOutUploadDisplay) {
            bgColor = 'bg-gray-100';
            textColor = 'text-gray-700';
            Icon = FiClock;
            displayStatus = 'Processing Upload';
            isSpinning = false;
            titleText = `Upload received ${Math.round(elapsedSeconds / 60)} mins ago, processing initiated. Status: ${originalStatus}`;
        } else {
            if (TERMINAL_STATUSES.includes(statusUpper)) {
                if (statusUpper === 'COMPLETE' || statusUpper === 'COMPLETED') {
                    bgColor = 'bg-green-100';
                    textColor = 'text-green-800';
                    Icon = FiCheckCircle;
                    displayStatus = 'Completed';
                } else if (statusUpper === 'COMPLETED_WITH_WARNINGS') {
                    bgColor = 'bg-yellow-100';
                    textColor = 'text-yellow-800';
                    Icon = FiAlertTriangle;
                    displayStatus = 'Completed w/ Warn';
                } else {
                    bgColor = 'bg-red-100';
                    textColor = 'text-red-800';
                    Icon = FiAlertTriangle;
                    displayStatus = 'Failed';
                }
            } else if (isProcessing) {
                bgColor = 'bg-yellow-100';
                textColor = 'text-yellow-800';
                Icon = FiLoader;
                displayStatus = 'Processing';
                isSpinning = true;
            } else if (isUploadingOrPending) {
                bgColor = 'bg-blue-100';
                textColor = 'text-blue-800';
                Icon = FiUploadCloud;
                displayStatus = 'Uploading';
                isSpinning = true;
            } else {
                displayStatus = 'Unknown';
                if (statusUpper !== 'UNKNOWN') {
                    console.warn('[Badge] Unknown recording status received:', originalStatus);
                }
            }
            
            titleText = (displayStatus === 'Failed' && failureReason)
                ? `${displayStatus}: ${failureReason}`
                : displayStatus;
            if (displayStatus !== originalStatus && originalStatus) {
                titleText += ` (Backend: ${originalStatus})`;
            }
        }

        return (
            <span
                title={titleText}
                className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${bgColor} ${textColor}`}
            >
                <Icon className={`mr-1 h-3 w-3 ${isSpinning ? 'animate-spin' : ''}`} />
                {displayStatus}
            </span>
        );
    };

    const groupRecordings = (recs) => {
        const groups = {
            'Today': [],
            'This Week': [],
            'This Month': [],
            'Older': []
        };

        const now = new Date();
        const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const oneWeekAgo = new Date(todayStart);
        oneWeekAgo.setDate(todayStart.getDate() - 7);
        const oneMonthAgo = new Date(todayStart);
        oneMonthAgo.setMonth(todayStart.getMonth() - 1);

        recs.forEach(rec => {
            if (!rec.uploadTimestamp?.seconds) {
                groups['Older'].push(rec);
                return;
            }
            const date = new Date(rec.uploadTimestamp.seconds * 1000);
            
            if (date >= todayStart) {
                groups['Today'].push(rec);
            } else if (date >= oneWeekAgo) {
                groups['This Week'].push(rec);
            } else if (date >= oneMonthAgo) {
                groups['This Month'].push(rec);
            } else {
                groups['Older'].push(rec);
            }
        });

        return groups;
    };

    const groupedRecordings = groupRecordings(filteredRecordings);

    const handleRetry = () => {
        fetchRecordings().then(initialData => {
             if (initialData) {
                startPolling(initialData);
             }
        });
    };

    return (
        <>
            <Header />
            <main className="flex-grow py-12 bg-gray-50 dark:bg-gray-900">
                <title>AudioScholar - My Recordings</title>
                <div className="container mx-auto px-4">
                    <div className="flex flex-col md:flex-row justify-between items-center mb-8 gap-4">
                        <h1 className="text-3xl font-bold text-gray-800 dark:text-white">My Recordings</h1>
                        <div className="flex items-center gap-4 w-full md:w-auto">
                             <div className="relative w-full md:w-64">
                                <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <FiSearch className="text-gray-400" />
                                </span>
                                <input
                                    type="text"
                                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500 dark:bg-gray-800 dark:border-gray-700 dark:text-white"
                                    placeholder="Search recordings..."
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                />
                            </div>

                            {!loading && recordings.length > 0 && (
                                <button
                                    onClick={handleDeleteAllRecordings}
                                    className="bg-red-600 hover:bg-red-700 text-white font-medium py-2 px-4 rounded-md transition flex items-center whitespace-nowrap"
                                    title="Delete All Recordings"
                                >
                                    <FiTrash2 className="mr-2 h-4 w-4" />
                                    Delete All
                                </button>
                            )}
                        </div>
                    </div>

                    {loading && (
                        <div className="text-center py-10">
                            <FiLoader className="animate-spin h-8 w-8 mx-auto text-teal-600" />
                            <p className="mt-2 text-gray-600 dark:text-gray-300">Loading recordings...</p>
                        </div>
                    )}

                    {error && !loading && (
                         <div className="flex flex-col items-center justify-center py-12 bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
                            <div className="bg-red-100 p-4 rounded-full mb-4">
                                <FiAlertTriangle className="w-8 h-8 text-red-600" />
                            </div>
                            <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-2">Failed to fetch recordings</h3>
                            <p className="text-gray-500 dark:text-gray-400 mb-6 text-center max-w-md">{error}</p>
                            <button
                                onClick={handleRetry}
                                className="flex items-center gap-2 px-6 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-md font-medium transition-colors"
                            >
                                <FiRefreshCw className="w-4 h-4" /> Retry
                            </button>
                        </div>
                    )}

                    {!loading && recordings.length === 0 && !error && (
                        <div className="text-center py-10 bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                            <FiFile className="mx-auto h-12 w-12 text-gray-400 mb-4" />
                            <p className="text-gray-600 dark:text-gray-300">You haven't uploaded any recordings yet.</p>
                            <Link to="/upload" className="mt-4 inline-block bg-[#2D8A8A] hover:bg-[#236b6b] text-white font-medium py-2 px-5 rounded-md transition">
                                Upload Your First Recording
                            </Link>
                        </div>
                    )}

                    {!loading && filteredRecordings.length === 0 && recordings.length > 0 && (
                         <div className="text-center py-10">
                            <p className="text-gray-600 dark:text-gray-300">No recordings found matching your search.</p>
                        </div>
                    )}

                    {!loading && filteredRecordings.length > 0 && (
                        <div className="space-y-8">
                            {Object.entries(groupedRecordings).map(([group, groupRecordings]) => (
                                groupRecordings.length > 0 && (
                                    <div key={group}>
                                        <h2 className="text-xl font-semibold text-gray-700 dark:text-gray-200 mb-4 ml-1">{group}</h2>
                                        <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
                                            <ul className="divide-y divide-gray-200 dark:divide-gray-700">
                                                {groupRecordings.map((recording) => (
                                                    <li key={recording.id} className="px-6 py-4 hover:bg-gray-50 dark:hover:bg-gray-700 transition duration-150 ease-in-out">
                                                        <div className="flex items-center justify-between flex-wrap gap-4">
                                                            <div className="flex-1 min-w-0">
                                                                <Link to={`/recordings/${recording.id}`} className="text-lg font-semibold text-teal-700 hover:text-teal-900 dark:text-teal-400 dark:hover:text-teal-300 truncate block" title={recording.title}>
                                                                    {recording.title || 'Untitled Recording'}
                                                                </Link>
                                                                <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">Uploaded: {formatDate(recording.uploadTimestamp)}</p>
                                                            </div>
                                                            <div className="flex items-center space-x-4 flex-shrink-0">
                                                                {getStatusBadge(recording)}
                                                                <Link to={`/recordings/${recording.id}`} className="text-sm text-indigo-600 hover:text-indigo-800 dark:text-indigo-400 dark:hover:text-indigo-300 font-medium inline-flex items-center" title="View Details">
                                                                    View Details <FiExternalLink className="ml-1 h-3 w-3" />
                                                                </Link>
                                                                <button
                                                                    onClick={() => handleDelete(recording.id)}
                                                                    className="text-red-500 hover:text-red-700 p-1 rounded-md hover:bg-red-100 dark:hover:bg-red-900/30 transition"
                                                                    title="Delete Recording"
                                                                >
                                                                    <FiTrash2 className="h-4 w-4" />
                                                                </button>
                                                            </div>
                                                        </div>
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>
                                    </div>
                                )
                            ))}
                        </div>
                    )}
                </div>
            </main>
        </>
    );
};

export default RecordingList;