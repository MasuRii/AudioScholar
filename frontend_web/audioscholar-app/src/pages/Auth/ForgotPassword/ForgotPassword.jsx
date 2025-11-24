import { getAuth, sendPasswordResetEmail } from 'firebase/auth';
import React, { useState } from 'react';
import { FiCheckCircle, FiKey, FiMail } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { firebaseApp } from '../../../config/firebaseConfig';
import { Footer, Header } from '../../Home/HomePage';


const ForgotPassword = () => {
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(false);
    const auth = getAuth(firebaseApp);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(false);

        if (!email) {
            setError('Please enter your email address.');
            return;
        }

        setLoading(true);
        try {
            const actionCodeSettings = {
                url: `${window.location.origin}/reset-password`,
                handleCodeInApp: true,
            };
            await sendPasswordResetEmail(auth, email, actionCodeSettings);
            setSuccess(true);
        } catch (err) {
            console.error('Firebase password reset error:', err);
            let errorMessage = 'Failed to send password reset email.';
            if (err.code) {
                switch (err.code) {
                    case 'auth/user-not-found':
                        errorMessage = 'No user found with this email address.';
                        break;
                    case 'auth/invalid-email':
                        errorMessage = 'Please enter a valid email address.';
                        break;
                    default:
                        errorMessage = 'An unexpected error occurred. Please try again.';
                }
            }
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <Header />
            <main className="flex-grow flex items-center justify-center py-12 bg-gray-50">
                <title>AudioScholar - Forgot Password</title>
                <div className="container mx-auto px-4 animate-fade-in-up">
                    <div className="max-w-md mx-auto bg-white rounded-lg shadow-xl overflow-hidden">
                        <div className="p-8 md:p-10">
                            <div className="text-center mb-6">
                                <FiKey className="w-12 h-12 mx-auto text-[#2D8A8A]" />
                                <h1 className="text-3xl font-bold text-gray-800 mt-4">Forgot Your Password?</h1>
                                <p className="text-gray-600 mt-2">
                                    No problem. Enter your email below and we'll send you a link to reset it.
                                </p>
                            </div>

                            {success ? (
                                <div className="text-center p-4 bg-green-50 rounded-lg">
                                    <FiCheckCircle className="w-8 h-8 mx-auto text-green-500" />
                                    <h2 className="text-xl font-semibold text-green-800 mt-3">Check Your Email</h2>
                                    <p className="text-green-700 mt-1">
                                        A password reset link has been sent to <span className="font-medium">{email}</span>. Please follow the instructions in the email to reset your password.
                                    </p>
                                </div>
                            ) : (
                                <form className="space-y-5" onSubmit={handleSubmit}>
                                    <div>
                                        <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                                            Email Address
                                        </label>
                                        <div className="relative">
                                            <FiMail className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                                            <input
                                                type="email"
                                                id="email"
                                                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#2D8A8A] focus:border-[#2D8A8A]"
                                                placeholder="you@example.com"
                                                value={email}
                                                onChange={(e) => setEmail(e.target.value)}
                                                required
                                                disabled={loading}
                                            />
                                        </div>
                                    </div>

                                    {error && (
                                        <p className="text-red-500 text-sm text-center">{error}</p>
                                    )}

                                    <button
                                        type="submit"
                                        className={`w-full bg-[#2D8A8A] text-white py-3 px-4 rounded-lg font-medium transition ${loading ? 'opacity-50 cursor-not-allowed' : 'hover:bg-[#236b6b]'}`}
                                        disabled={loading}
                                    >
                                        {loading ? 'Sending Reset Link...' : 'Send Password Reset Link'}
                                    </button>
                                </form>
                            )}

                            <div className="mt-6 text-center">
                                <p className="text-sm text-gray-600">
                                    Remember your password?{' '}
                                    <Link to="/signin" className="text-[#2D8A8A] hover:text-[#236b6b] font-medium">
                                        Sign In
                                    </Link>
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
            <Footer />
        </>
    );
};

export default ForgotPassword;
