import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { FiMail, FiCheckCircle } from 'react-icons/fi';
import { Footer, Header } from '../../Home/HomePage';

const EmailVerificationCodeInput = () => {
    const [code, setCode] = useState('');
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(false);
    const [loading, setLoading] = useState(false);

    const handleVerifyCode = (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(false);
        setLoading(true);

        if (code.length !== 6 || !/^\d+$/.test(code)) {
            setError('Please enter a valid 6-digit code.');
            setLoading(false);
            return;
        }

        // Simulate API call for code verification
        setTimeout(() => {
            setLoading(false);
            // For simulation, any 6-digit code is "successful"
            setSuccess(true);
            // In a real scenario, you would call Firebase/backend to verify the code
            // and then navigate to sign-in or dashboard
        }, 1500);
    };

    const handleResendCode = () => {
        setError(null);
        setSuccess(false);
        setLoading(true);
        // Simulate API call to resend code
        setTimeout(() => {
            setLoading(false);
            setError('A new code has been sent to your email.');
        }, 1500);
    };

    return (
        <>
            <Header />
            <main className="flex-grow flex items-center justify-center py-12 bg-gray-50">
                <title>AudioScholar - Verify Email</title>
                <div className="container mx-auto px-4 animate-fade-in-up">
                    <div className="max-w-md mx-auto bg-white rounded-lg shadow-xl overflow-hidden">
                        <div className="p-8 md:p-10">
                            <div className="text-center mb-6">
                                <FiMail className="w-12 h-12 mx-auto text-[#2D8A8A]" />
                                <h1 className="text-3xl font-bold text-gray-800 mt-4">Verify Your Email</h1>
                                <p className="text-gray-600 mt-2">
                                    A 6-digit verification code has been sent to your email address. Please enter it below.
                                </p>
                            </div>

                            {success ? (
                                <div className="text-center p-4 bg-green-50 rounded-lg">
                                    <FiCheckCircle className="w-8 h-8 mx-auto text-green-500" />
                                    <h2 className="text-xl font-semibold text-green-800 mt-3">Email Verified!</h2>
                                    <p className="text-green-700 mt-1">
                                        Your email address has been successfully verified. You can now proceed to sign in.
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
                                <form className="space-y-5" onSubmit={handleVerifyCode}>
                                    <div>
                                        <label htmlFor="code" className="block text-sm font-medium text-gray-700 mb-1">
                                            Verification Code
                                        </label>
                                        <input
                                            type="text"
                                            id="code"
                                            className="w-full px-4 py-2 border border-gray-300 rounded-lg text-center text-xl tracking-widest focus:ring-2 focus:ring-[#2D8A8A] focus:border-[#2D8A8A]"
                                            placeholder="------"
                                            maxLength="6"
                                            value={code}
                                            onChange={(e) => setCode(e.target.value)}
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
                                        {loading ? 'Verifying...' : 'Verify Code'}
                                    </button>

                                    <div className="text-center mt-4">
                                        <button
                                            type="button"
                                            onClick={handleResendCode}
                                            className="text-sm text-[#2D8A8A] hover:text-[#236b6b] font-medium"
                                            disabled={loading}
                                        >
                                            Resend Code
                                        </button>
                                    </div>
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

export default EmailVerificationCodeInput;