import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../Home/HomePage';
import axios from 'axios';
import { API_BASE_URL } from '../../services/authService';

const CheckoutPage = () => {
    const [tier, setTier] = useState(null);
    const [paymentDetails, setPaymentDetails] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [showSuccessModal, setShowSuccessModal] = useState(false);
    const [successMessage, setSuccessMessage] = useState('');
    const [secondaryMessage, setSecondaryMessage] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        // Retrieve selected tier and payment details from localStorage
        const selectedTier = localStorage.getItem('selectedTier');
        const details = localStorage.getItem('paymentDetails');

        if (!selectedTier || !details) {
            // If data is missing, redirect back to subscription selection
            console.error('Missing subscription or payment details. Redirecting...');
            navigate('/subscribe'); 
            return;
        }

        setTier(selectedTier);
        try {
            setPaymentDetails(JSON.parse(details));
        } catch (error) {
            console.error('Error parsing payment details:', error);
            navigate('/payment'); // Redirect if details are corrupted
        }

    }, [navigate]);

    const handleConfirm = () => {
        setIsLoading(true);

        console.log('Confirming subscription...', { tier, paymentDetails });

        // --- Mock Backend Call ---
        // Simulate API call delay
        setTimeout(async () => {
            console.log('Subscription Confirmed (Mocked)!');

            const token = localStorage.getItem('AuthToken');
            const userId = localStorage.getItem('userId');

            if (!token || !userId) {
                console.error('AuthToken or UserId not found. Cannot update role.');
                setSuccessMessage('We encountered an issue updating your account');
                setSecondaryMessage('Critical user information is missing to update your role. Please re-login or contact support.');
                setShowSuccessModal(true);
                setIsLoading(false);
                localStorage.removeItem('selectedTier');
                localStorage.removeItem('paymentDetails');
                return;
            }

            try {
                const roleUpdateUrl = `${API_BASE_URL}api/users/${userId}/role`;
                const roleUpdatePayload = { role: 'ROLE_PREMIUM' };
                
                console.log(`Attempting to update role for user ${userId} to ROLE_PREMIUM at ${roleUpdateUrl}`);

                const response = await axios.put(roleUpdateUrl, roleUpdatePayload, {
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (response.status === 200 || response.status === 204) {
                    console.log('Successfully updated user role to ROLE_PREMIUM');
                    localStorage.setItem('userSubscriptionTier', tier);
                    setSuccessMessage('Thank you for your purchase!');
                    setSecondaryMessage('Your Premium membership is now active and your account has been upgraded.');
                    setShowSuccessModal(true);
                } else {
                    console.error('Role update API call was not successful, status:', response.status);
                    localStorage.setItem('userSubscriptionTier', tier);
                    setSuccessMessage('Subscription completed, but with an issue');
                    setSecondaryMessage('Your payment went through, but we could not update your account privileges automatically. Please contact support.');
                    setShowSuccessModal(true);
                }

            } catch (err) {
                console.error('Error updating user role:', err);
                localStorage.setItem('userSubscriptionTier', tier);
                let errorMessage = 'Subscription payment processed, but failed to update your account privileges.';
                if (err.response) {
                    errorMessage += ` (Server responded with ${err.response.status}).`;
                    if (err.response.status === 403) {
                        errorMessage += ' It seems there was a permission issue.';
                    }
                }
                setSuccessMessage('We encountered an issue updating your account');
                setSecondaryMessage(errorMessage + ' Please contact support if this persists.');
                setShowSuccessModal(true);
            }
            
            localStorage.removeItem('selectedTier');
            localStorage.removeItem('paymentDetails');
            setIsLoading(false);

        }, 1500); // Simulate 1.5 seconds delay
    };

    const handleCloseSuccessModal = () => {
        setShowSuccessModal(false);
        navigate('/profile');
    };

    const getPaymentDisplay = () => {
        if (!paymentDetails) return 'N/A';

        if (paymentDetails.type === 'card') {
            return `Card ending in ${paymentDetails.last4}, expires ${paymentDetails.expiry}`;
        } else if (paymentDetails.type === 'ewallet') {
            // Use masked number if available, otherwise show full (though masking is better)
            return `E-Wallet: ${paymentDetails.maskedNumber || paymentDetails.number}`;
        }
        return 'Unknown Payment Method';
    };

    const getPrice = () => {
        // In a real app, fetch this based on the tier
        // Updated price for Premium
        return tier === 'Premium' ? '₱150 / month' : 'Free';
    }

    if (!tier || !paymentDetails) {
        // Show loading or placeholder while retrieving data
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-100">
                 <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-500"></div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex flex-col bg-gray-100">
            <title>AudioScholar - Checkout</title>
            <Header />

            <main className="flex-grow flex items-center justify-center py-12">
                <div className="container mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="max-w-lg mx-auto bg-white p-8 rounded-lg shadow-xl relative">
                        <h1 className="text-2xl font-bold text-gray-800 mb-6 text-center">Confirm Your Subscription</h1>

                        <div className="space-y-4 mb-8">
                            <div className="flex justify-between items-center border-b pb-2">
                                <span className="text-gray-600 font-medium">Plan:</span>
                                <span className="text-gray-800 font-semibold text-lg">{tier}</span>
                            </div>
                            <div className="flex justify-between items-center border-b pb-2">
                                <span className="text-gray-600 font-medium">Price:</span>
                                <span className="text-gray-800 font-semibold text-lg">{getPrice()}</span>
                            </div>
                            <div className="flex justify-between items-center border-b pb-2">
                                <span className="text-gray-600 font-medium">Payment Method:</span>
                                <span className="text-gray-800 text-sm text-right">{getPaymentDisplay()}</span>
                            </div>
                        </div>

                        <button 
                            onClick={handleConfirm}
                            disabled={isLoading}
                            className={`w-full py-3 px-4 rounded-md shadow-sm text-sm font-medium text-white transition-colors duration-200 transform hover:-translate-y-0.5 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#2D8A8A] ${
                                isLoading 
                                ? 'bg-gray-400 cursor-not-allowed' 
                                : 'bg-[#2D8A8A] hover:bg-[#236b6b]'
                            }`}
                        >
                            {isLoading ? (
                                <span className="flex items-center justify-center">
                                    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    Processing...
                                </span>
                            ) : (
                                'Confirm and Pay'
                            )}
                        </button>

                        <button 
                            onClick={() => navigate('/payment')} // Go back to payment method selection
                            disabled={isLoading}
                            className="w-full mt-3 py-2 px-4 rounded-md text-sm font-medium text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-300 transition-colors duration-200"
                        >
                            Change Payment Method
                        </button>
                    </div>
                    {showSuccessModal && (
                        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
                            <div className="bg-white rounded-lg shadow-2xl max-w-md w-full mx-4 p-6">
                                <div className="flex flex-col items-center text-center">
                                    <div className="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center mb-4">
                                        <svg className="w-7 h-7 text-green-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                                        </svg>
                                    </div>
                                    <h2 className="text-xl font-bold text-gray-900 mb-2">{successMessage || 'Purchase complete'}</h2>
                                    <p className="text-sm text-gray-600 mb-4">
                                        {secondaryMessage || 'Your Premium membership details have been updated.'}
                                    </p>
                                    <button
                                        onClick={handleCloseSuccessModal}
                                        className="w-full mt-2 py-2.5 px-4 rounded-md text-sm font-medium text-white bg-[#2D8A8A] hover:bg-[#236b6b] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#2D8A8A] transition-colors"
                                    >
                                        Go to Profile
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
};

export default CheckoutPage;