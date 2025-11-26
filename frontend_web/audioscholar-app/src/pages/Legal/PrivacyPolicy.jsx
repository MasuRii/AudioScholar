import React, { useEffect } from 'react';
import { Header, Footer } from '../Home/HomePage';

const PrivacyPolicy = () => {
  useEffect(() => {
    window.scrollTo(0, 0); // Scroll to top on component mount
  }, []);

  return (
    <div className="flex flex-col min-h-screen bg-gray-100 dark:bg-gray-900 transition-colors duration-200">
      <title>AudioScholar - Privacy Policy</title>
      <Header />
      <main className="flex-grow container mx-auto px-4 py-12">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-8 md:p-12 max-w-4xl mx-auto transition-colors duration-200">
          <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-6">Privacy Policy</h1>
          <div className="prose prose-gray dark:prose-invert max-w-none text-gray-700 dark:text-gray-300">
            <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">Last updated: {new Date().toLocaleDateString()}</p>
            
            <p className="mb-4">
              At AudioScholar, we take your privacy seriously. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you visit our website and use our application.
            </p>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">1. Information We Collect</h2>
            <p className="mb-4">
              We may collect information about you in a variety of ways. The information we may collect includes:
            </p>
            <ul className="list-disc list-inside mb-4 ml-4 space-y-2">
              <li><strong>Personal Data:</strong> Personally identifiable information, such as your name, email address, and telephone number, that you voluntarily give to us when you register with the application.</li>
              <li><strong>Derivative Data:</strong> Information our servers automatically collect when you access the application, such as your IP address, your browser type, your operating system, your access times, and the pages you have viewed directly before and after accessing the application.</li>
              <li><strong>Financial Data:</strong> Financial information, such as data related to your payment method (e.g., valid credit card number, card brand, expiration date) that we may collect when you purchase, order, return, exchange, or request information about our services from the application.</li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">2. Use of Your Information</h2>
            <p className="mb-4">
              Having accurate information about you permits us to provide you with a smooth, efficient, and customized experience. Specifically, we may use information collected about you via the application to:
            </p>
            <ul className="list-disc list-inside mb-4 ml-4 space-y-2">
              <li>Create and manage your account.</li>
              <li>Process your payments and refunds.</li>
              <li>Email you regarding your account or order.</li>
              <li>Generate a personal profile about you to make future visits to the application more personalized.</li>
              <li>Monitor and analyze usage and trends to improve your experience with the application.</li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">3. Disclosure of Your Information</h2>
            <p className="mb-4">
              We may share information we have collected about you in certain situations. Your information may be disclosed as follows:
            </p>
            <ul className="list-disc list-inside mb-4 ml-4 space-y-2">
              <li><strong>By Law or to Protect Rights:</strong> If we believe the release of information about you is necessary to respond to legal process, to investigate or remedy potential violations of our policies, or to protect the rights, property, and safety of others, we may share your information as permitted or required by any applicable law, rule, or regulation.</li>
              <li><strong>Third-Party Service Providers:</strong> We may share your information with third parties that perform services for us or on our behalf, including payment processing, data analysis, email delivery, hosting services, customer service, and marketing assistance.</li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">4. Security of Your Information</h2>
            <p className="mb-4">
              We use administrative, technical, and physical security measures to help protect your personal information. While we have taken reasonable steps to secure the personal information you provide to us, please be aware that despite our efforts, no security measures are perfect or impenetrable, and no method of data transmission can be guaranteed against any interception or other type of misuse.
            </p>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">5. Contact Us</h2>
            <p className="mb-4">
              If you have questions or comments about this Privacy Policy, please contact us at:
            </p>
            <p className="font-medium">support@audioscholar.com</p>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
};

export default PrivacyPolicy;