import { applyActionCode, getAuth, onAuthStateChanged } from 'firebase/auth';
import React, { useEffect, useRef, useState } from 'react';
import { FiAlertCircle, FiCheckCircle, FiLoader } from 'react-icons/fi';
import { Link, useSearchParams } from 'react-router-dom';
import { firebaseApp } from '../../../config/firebaseConfig';
import { Footer, Header } from '../../Home/HomePage';

const EmailVerification = () => {
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState('verifying'); // verifying, success, error
    const [message, setMessage] = useState('');
    const auth = getAuth(firebaseApp);
    const verificationAttempted = useRef(false);

    useEffect(() => {
        let isMounted = true;

        const verifyEmail = async (currentUser) => {
            const oobCode = searchParams.get('oobCode');
            
            // If no code is present in the URL, we can't verify anything using applyActionCode.
            // However, we should check if the user is ALREADY verified in the background.
            if (!oobCode) {
                if (currentUser?.emailVerified) {
                     if (isMounted) {
                        setStatus('success');
                        setMessage('Your email address is verified.');
                     }
                     return;
                }
                
                if (isMounted) {
                    setStatus('error');
                    setMessage('Verification link is invalid or missing the required code.');
                }
                return;
            }

            // Prevent re-running applyActionCode if we've already started/finished it in this mount
            if (verificationAttempted.current) return;
            verificationAttempted.current = true;

            try {
                await applyActionCode(auth, oobCode);
                // Success!
                if (isMounted) {
                    setStatus('success');
                    setMessage('Your email address has been successfully verified.');
                }
            } catch (error) {
                console.error('Email verification error:', error);

                // If applyActionCode fails, it might be because the code was already used.
                // In this case, we MUST check the actual user status.
                // We reload the user to get the freshest token/claims.
                let isVerified = false;
                if (currentUser) {
                    try {
                        await currentUser.reload();
                        isVerified = currentUser.emailVerified;
                    } catch (reloadError) {
                        console.warn('Failed to reload user to check verification status:', reloadError);
                        // If we can't reload, we can't be sure, so we might fall back to error
                        // UNLESS the error code strongly suggests success (invalid-action-code often means used).
                    }
                }

                // If the user IS verified, override the error and show success.
                if (isVerified) {
                    if (isMounted) {
                        setStatus('success');
                        setMessage('Your email address is verified.');
                    }
                    return;
                }

                // If we aren't verified, but the error code suggests the link was used/expired
                // AND we couldn't verify the user status (e.g. not logged in), 
                // we might still want to show a "likely verified" or specific message, 
                // but for now, stick to the requested logic: false flags should be success.
                // "auth/invalid-action-code" usually means the code was used. 
                // If the user clicked it twice, the second time is "invalid".
                if (
                    error?.code === 'auth/invalid-action-code' || 
                    error?.code === 'auth/code-expired' ||
                    (typeof error?.message === 'string' &&
                        (error.message.toLowerCase().includes('already been used') || 
                         error.message.toLowerCase().includes('verified')))
                ) {
                    if (isMounted) {
                        setStatus('success');
                        setMessage('Your email address is likely already verified.');
                    }
                    return;
                }

                if (isMounted) {
                    setStatus('error');
                    setMessage(error?.message || 'Failed to verify email. The link may be invalid or expired.');
                }
            }
        };

        const unsubscribe = onAuthStateChanged(auth, (user) => {
            verifyEmail(user);
        });

        return () => {
            isMounted = false;
            unsubscribe();
        };
    }, [auth, searchParams]);

    return (
        <>
            <Header />
            <main className="flex-grow flex items-center justify-center py-12 bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
                <title>AudioScholar - Email Verification</title>
                <div className="container mx-auto px-4 animate-fade-in-up">
                    <div className="max-w-md mx-auto bg-white dark:bg-gray-800 rounded-lg shadow-xl overflow-hidden transition-colors duration-200">
                        <div className="p-8 md:p-10 text-center">
                            
                            {status === 'verifying' && (
                                <div className="flex flex-col items-center">
                                    <FiLoader className="w-16 h-16 text-[#2D8A8A] animate-spin mb-4" />
                                    <h1 className="text-2xl font-bold text-gray-800 dark:text-white">Verifying Email...</h1>
                                    <p className="text-gray-600 dark:text-gray-300 mt-2">Please wait while we verify your email address.</p>
                                </div>
                            )}

                            {status === 'success' && (
                                <div className="flex flex-col items-center">
                                    <FiCheckCircle className="w-16 h-16 text-green-500 mb-4" />
                                    <h1 className="text-3xl font-bold text-gray-800 dark:text-white">Email Verified!</h1>
                                    <p className="text-gray-600 dark:text-gray-300 mt-2">
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
                                    <h1 className="text-2xl font-bold text-gray-800 dark:text-white">Verification Failed</h1>
                                    <p className="text-gray-600 dark:text-gray-300 mt-2">{message}</p>
                                    <div className="mt-6 w-full">
                                        <Link
                                            to="/signin"
                                            className="block w-full bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-200 py-3 px-4 rounded-lg font-medium transition hover:bg-gray-300 dark:hover:bg-gray-600"
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