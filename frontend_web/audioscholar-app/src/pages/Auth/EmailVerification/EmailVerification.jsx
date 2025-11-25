import { applyActionCode, getAuth } from 'firebase/auth';
import React, { useEffect, useState } from 'react';
import { FiAlertCircle, FiCheckCircle, FiLoader } from 'react-icons/fi';
import { Link, useSearchParams } from 'react-router-dom';
import { firebaseApp } from '../../../config/firebaseConfig';
import { Footer, Header } from '../../Home/HomePage';

const EmailVerification = () => {
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState('verifying'); // verifying, success, error
    const [message, setMessage] = useState('');
    const auth = getAuth(firebaseApp);

    useEffect(() => {
        const verifyEmail = async () => {
            const oobCode = searchParams.get('oobCode');
            
            if (!oobCode) {
                setStatus('error');
                setMessage('Invalid verification link. No code found.');
                return;
            }

            try {
                await applyActionCode(auth, oobCode);
                setStatus('success');
            } catch (error) {
                console.error('Email verification error:', error);

                // If the code has already been used but the email is verified,
                // Firebase will throw an "invalid-action-code" error even though
                // the verification itself succeeded. In that case we treat it as
                // a success state so users are not shown a failure page.
                if (
                    error?.code === 'auth/invalid-action-code' ||
                    error?.code === 'auth/code-expired' ||
                    (typeof error?.message === 'string' &&
                        error.message.toLowerCase().includes('already been used'))
                ) {
                    setStatus('success');
                    setMessage('Your email address is already verified.');
                    return;
                }

                setStatus('error');
                setMessage(error?.message || 'Failed to verify email. The link may be invalid or expired.');
            }
        };

        verifyEmail();
    }, [auth, searchParams]);

    return (
        <>
            <Header />
            <main className="flex-grow flex items-center justify-center py-12 bg-gray-50">
                <title>AudioScholar - Email Verification</title>
                <div className="container mx-auto px-4 animate-fade-in-up">
                    <div className="max-w-md mx-auto bg-white rounded-lg shadow-xl overflow-hidden">
                        <div className="p-8 md:p-10 text-center">
                            
                            {status === 'verifying' && (
                                <div className="flex flex-col items-center">
                                    <FiLoader className="w-16 h-16 text-[#2D8A8A] animate-spin mb-4" />
                                    <h1 className="text-2xl font-bold text-gray-800">Verifying Email...</h1>
                                    <p className="text-gray-600 mt-2">Please wait while we verify your email address.</p>
                                </div>
                            )}

                            {status === 'success' && (
                                <div className="flex flex-col items-center">
                                    <FiCheckCircle className="w-16 h-16 text-green-500 mb-4" />
                                    <h1 className="text-3xl font-bold text-gray-800">Email Verified!</h1>
                                    <p className="text-gray-600 mt-2">
                                        Your email address has been successfully verified.
                                    </p>
                                    <div className="mt-6 w-full">
                                        <Link
                                            to="/signin"
                                            className="block w-full bg-[#2D8A8A] text-white py-3 px-4 rounded-lg font-medium transition hover:bg-[#236b6b]"
                                        >
                                            Proceed to Sign In
                                        </Link>
                                    </div>
                                </div>
                            )}

                            {status === 'error' && (
                                <div className="flex flex-col items-center">
                                    <FiAlertCircle className="w-16 h-16 text-red-500 mb-4" />
                                    <h1 className="text-2xl font-bold text-gray-800">Verification Failed</h1>
                                    <p className="text-gray-600 mt-2">{message}</p>
                                    <div className="mt-6 w-full">
                                        <Link
                                            to="/signin"
                                            className="block w-full bg-gray-200 text-gray-800 py-3 px-4 rounded-lg font-medium transition hover:bg-gray-300"
                                        >
                                            Back to Sign In
                                        </Link>
                                    </div>
                                </div>
                            )}

                        </div>
                    </div>
                </div>
            </main>
            <Footer />
        </>
    );
};

export default EmailVerification;