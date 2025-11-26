import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../Home/HomePage';
import CardPaymentForm from './CardPaymentForm';
import EWalletPaymentForm from './EWalletPaymentForm';
import { FaCcVisa, FaCcMastercard } from 'react-icons/fa';

const PaymentMethodPage = () => {
    const [paymentMethod, setPaymentMethod] = useState('card'); // Default to 'card'
    const navigate = useNavigate();

    const handlePaymentSubmit = (paymentDetails) => {
        console.log('Payment Method:', paymentMethod);
        console.log('Payment Details:', paymentDetails);
        // Store payment details temporarily (e.g., localStorage or state management)
        // Be cautious about storing sensitive data like full card numbers in localStorage
        localStorage.setItem('paymentMethod', paymentMethod);
        localStorage.setItem('paymentDetails', JSON.stringify(paymentDetails)); // Store details (potentially masked later)

        // Mock successful payment and navigate to checkout/summary
        navigate('/checkout');
    };

    return (
        <div className="min-h-screen flex flex-col bg-gray-100 dark:bg-gray-900 transition-colors duration-200">
            <title>AudioScholar - Payment Method</title>
            <Header />

            <main className="flex-grow flex items-center justify-center py-12">
                <div className="container mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="max-w-lg mx-auto bg-white dark:bg-gray-800 p-8 rounded-lg shadow-xl transition-colors duration-200">
                        <h1 className="text-2xl font-bold text-gray-800 dark:text-white mb-6 text-center">Select Payment Method</h1>

                        <div className="mb-6">
                            <div className="flex border border-gray-200 dark:border-gray-600 rounded-md overflow-hidden">
                                <button
                                    onClick={() => setPaymentMethod('card')}
                                    className={`flex-1 py-3 px-4 text-sm font-medium focus:outline-none transition-colors duration-200 flex items-center justify-center space-x-2 ${
                                        paymentMethod === 'card' 
                                        ? 'bg-[#2D8A8A] text-white' 
                                        : 'bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-600'
                                    }`}
                                >
                                    <span>Credit/Debit Card</span>
                                    <div className="flex space-x-1 items-center">
                                        <FaCcVisa className="h-5 w-5" />
                                        <FaCcMastercard className="h-5 w-5" />
                                    </div>
                                </button>
                                <button
                                    onClick={() => setPaymentMethod('ewallet')}
                                    className={`flex-1 py-3 px-4 text-sm font-medium focus:outline-none transition-colors duration-200 flex items-center justify-center space-x-2 ${
                                        paymentMethod === 'ewallet' 
                                        ? 'bg-[#2D8A8A] text-white' 
                                        : 'bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-600'
                                    }`}
                                >
                                    <span></span>
                                    <img src="/gcash.jpg" alt="GCash" className="h-5 w-auto rounded" />
                                    <img src="/Maya_logo.svg.png" alt="PayMaya" className="h-5 w-auto" />
                                </button>
                            </div>
                        </div>

                        {/* Render the appropriate form based on selection */}
                        {paymentMethod === 'card' && <CardPaymentForm onSubmit={handlePaymentSubmit} />}
                        {paymentMethod === 'ewallet' && <EWalletPaymentForm onSubmit={handlePaymentSubmit} />}
                        
                    </div>
                </div>
            </main>
        </div>
    );
};

export default PaymentMethodPage; 