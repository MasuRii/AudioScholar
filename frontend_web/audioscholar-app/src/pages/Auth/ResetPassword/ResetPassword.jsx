import { getAuth, confirmPasswordReset } from 'firebase/auth';
import React, { useState } from 'react';
import { FiLock, FiCheckCircle } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { firebaseApp } from '../../../config/firebaseConfig';
import { Footer, Header } from '../../Home/HomePage';

const ResetPassword = ({ oobCode }) => {
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(false);
    const auth = getAuth(firebaseApp);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(false);

        if (!newPassword || !confirmPassword) {
            setError('Please fill in both password fields.');
            return;
        }

        if (newPassword.length < 8) {
            setError('New password must be at least 8 characters long.');
            return;
        }

        if (newPassword !== confirmPassword) {
            setError('New password and confirm password do not match.');
            return;
        }

        setLoading(true);
        try {
            await confirmPasswordReset(auth, oobCode, newPassword);
            setSuccess(true);
            setNewPassword('');
            setConfirmPassword('');
        } catch (err) {
            console.error('Password reset error:', err);
            setError(err.message || 'Failed to reset password. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <Header />
            <main className="flex-grow flex items-center justify-center py-12 bg-gray-50">
                <title>AudioScholar - Reset Password</title>
                <div className="container mx-auto px-4 animate-fade-in-up">
                    <div className="max-w-md mx-auto bg-white rounded-lg shadow-xl overflow-hidden">
                        <div className="p-8 md:p-10">
                            <div className="text-center mb-6">
                                <FiLock className="w-12 h-12 mx-auto text-[#2D8A8A]" />
                                <h1 className="text-3xl font-bold text-gray-800 mt-4">Set New Password</h1>
                                <p className="text-gray-600 mt-2">
                                    Enter your new password below.
                                </p>
                            </div>

                            {success ? (
                                <div className="text-center p-4 bg-green-50 rounded-lg">
                                    <FiCheckCircle className="w-8 h-8 mx-auto text-green-500" />
                                    <h2 className="text-xl font-semibold text-green-800 mt-3">Password Reset Successful!</h2>
                                    <p className="text-green-700 mt-1">
                                        Your password has been successfully reset. You can now sign in with your new password.
                                    </p>
                                    <div className="mt-6">
                                        <Link
                                            to="/signin"
                                            className="w-full bg-[#2D8A8A] text-white py-3 px-4 rounded-lg font-medium transition hover:bg-[#236b6b]"
                                        >
                                            Proceed to Sign In
                                        </Link>
                                    </div>
                                </div>
                            ) : (
                                <form className="space-y-5" onSubmit={handleSubmit}>
                                    <div>
                                        <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700 mb-1">
                                            New Password
                                        </label>
                                        <input
                                            type="password"
                                            id="newPassword"
                                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#2D8A8A] focus:border-[#2D8A8A]"
                                            placeholder="Enter new password"
                                            value={newPassword}
                                            onChange={(e) => setNewPassword(e.target.value)}
                                            required
                                            disabled={loading}
                                        />
                                    </div>

                                    <div>
                                        <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">
                                            Confirm New Password
                                        </label>
                                        <input
                                            type="password"
                                            id="confirmPassword"
                                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#2D8A8A] focus:border-[#2D8A8A]"
                                            placeholder="Confirm new password"
                                            value={confirmPassword}
                                            onChange={(e) => setConfirmPassword(e.target.value)}
                                            required
                                            disabled={loading}
                                        />
                                    </div>

                                    {error && (
                                        <p className="text-red-500 text-sm text-center">{error}</p>
                                    )}

                                    <button
                                        type="submit"
                                        className={`w-full bg-[#2D8A8A] text-white py-3 px-4 rounded-lg font-medium transition ${loading ? 'opacity-50 cursor-not-allowed' : 'hover:bg-[#236b6b]'}`}
                                        disabled={loading}
                                    >
                                        {loading ? 'Resetting Password...' : 'Reset Password'}
                                    </button>
                                </form>
                            )}
                        </div>
                    </div>
                </div>
            </main>
            <Footer />
        </>
    );
};

export default ResetPassword;