import { getAuth, sendEmailVerification } from 'firebase/auth';
import React, { useEffect, useState } from 'react';
import { FiMail } from 'react-icons/fi';
import { useLocation, useNavigate } from 'react-router-dom';
import { firebaseApp } from '../../../config/firebaseConfig';
import { Footer, Header } from '../../Home/HomePage';

const EmailVerificationNotice = () => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [timer, setTimer] = useState(0);
    const [canResend, setCanResend] = useState(true);
    const [resendAttempts, setResendAttempts] = useState(0);
    const location = useLocation();
    const navigate = useNavigate();
    const auth = getAuth(firebaseApp);

    const handleResendVerification = async () => {
        if (!canResend) return;

        setLoading(true);
        setError(null);
        setSuccess(null);

        try {
            const user = auth.currentUser;
            if (user) {
                await sendEmailVerification(user);
                setSuccess('A new verification email has been sent.');
                setResendAttempts(resendAttempts + 1);
                setCanResend(false);
                if (resendAttempts < 1) {
                    setTimer(60);
                } else {
                    setTimer(600);
                }
            } else {
                setError('No user is signed in.');
            }
        } catch (err) {
            setError(err.message || 'An unexpected error occurred.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        let interval;
        if (!canResend && timer > 0) {
            interval = setInterval(() => {
                setTimer((prevTimer) => prevTimer - 1);
            }, 1000);
        } else if (timer === 0) {
            setCanResend(true);
            clearInterval(interval);
        }
        return () => clearInterval(interval);
    }, [canResend, timer]);

    // Polling for email verification status
    useEffect(() => {
        const checkVerification = setInterval(async () => {
            const user = auth.currentUser;
            if (user) {
                await user.reload(); // Reload user to get the latest emailVerified status
                if (user.emailVerified) {
                    clearInterval(checkVerification);
                    navigate('/signin'); // Redirect to sign-in page
                }
            }
        }, 3000); // Check every 3 seconds

        return () => clearInterval(checkVerification); // Cleanup on unmount
    }, [auth, navigate]);


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
                                    We sent an email with a verification link to <strong>{location.state?.email}</strong>. Please check your inbox and click the link to verify your email address.
                                </p>
                            </div>

                            {error && <p className="text-red-500 text-sm text-center mb-4">{error}</p>}
                            {success && <p className="text-green-500 text-sm text-center mb-4">{success}</p>}

                            <div className="mt-6 text-center">
                                {resendAttempts < 2 ? (
                                    <>
                                        <p className="text-gray-600">
                                            Didn't receive the email?{' '}
                                            <button
                                                onClick={handleResendVerification}
                                                className={`text-sm text-[#2D8A8A] hover:text-[#236b6b] font-medium ${!canResend || loading ? 'opacity-50 cursor-not-allowed' : ''}`}
                                                disabled={!canResend || loading}
                                            >
                                                {loading ? 'Sending...' : 'Resend Verification Link'}
                                            </button>
                                        </p>
                                        {!canResend && timer > 0 && (
                                            <p className="text-gray-500 text-sm mt-2">
                                                You can resend the link in {Math.floor(timer / 60)}:{('0' + (timer % 60)).slice(-2)} minutes.
                                            </p>
                                        )}
                                    </>
                                ) : (
                                    <p className="text-red-500 text-sm text-center">
                                        You have requested too many verification emails. Please wait {Math.floor(timer / 60)}:{('0' + (timer % 60)).slice(-2)} minutes before trying again.
                                    </p>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </main>
            <Footer />
        </>
    );
};

export default EmailVerificationNotice;