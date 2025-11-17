import React from 'react';
import { FiCheckCircle } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { Footer, Header } from '../../Home/HomePage';

const EmailVerification = () => {
    return (
        <>
            <Header />
            <main className="flex-grow flex items-center justify-center py-12 bg-gray-50">
                <title>AudioScholar - Email Verified</title>
                <div className="container mx-auto px-4 animate-fade-in-up">
                    <div className="max-w-md mx-auto bg-white rounded-lg shadow-xl overflow-hidden">
                        <div className="p-8 md:p-10 text-center">
                            <FiCheckCircle className="w-16 h-16 mx-auto text-green-500" />
                            <h1 className="text-3xl font-bold text-gray-800 mt-4">Email Verified!</h1>
                            <p className="text-gray-600 mt-2">
                                Your email address has been successfully verified.
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
                    </div>
                </div>
            </main>
            <Footer />
        </>
    );
};

export default EmailVerification;
