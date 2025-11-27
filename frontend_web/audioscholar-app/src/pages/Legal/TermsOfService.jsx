import React, { useEffect } from 'react';
import { Header, Footer } from '../Home/HomePage';

const TermsOfService = () => {
  useEffect(() => {
    window.scrollTo(0, 0); // Scroll to top on component mount
  }, []);

  return (
    <div className="flex flex-col min-h-screen bg-gray-100 dark:bg-gray-900 transition-colors duration-200">
      <title>AudioScholar - Terms of Service</title>
      <Header />
      <main className="flex-grow container mx-auto px-4 py-12">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-8 md:p-12 max-w-4xl mx-auto transition-colors duration-200">
          <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-6">Terms of Service</h1>
          <div className="prose prose-gray dark:prose-invert max-w-none text-gray-700 dark:text-gray-300">
            <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">Last updated: {new Date().toLocaleDateString()}</p>
            
            <p className="mb-4">
              Welcome to AudioScholar! These Terms of Service ("Terms") govern your use of the AudioScholar website and application. By accessing or using our service, you agree to be bound by these Terms.
            </p>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">1. Acceptance of Terms</h2>
            <p className="mb-4">
              By creating an account or using our services, you agree to comply with these Terms and our Privacy Policy. If you do not agree with any part of these terms, you are prohibited from using our services.
            </p>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">2. User Accounts</h2>
            <p className="mb-4">
              To access certain features, you must create an account. You are responsible for maintaining the confidentiality of your account credentials and for all activities that occur under your account. You agree to notify us immediately of any unauthorized use of your account.
            </p>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">3. Acceptable Use</h2>
            <p className="mb-4">
              You agree not to use the service for any unlawful purpose or in any way that interruptions, damages, or impairs the service. Prohibited activities include, but are not limited to:
            </p>
            <ul className="list-disc list-inside mb-4 ml-4 space-y-2">
              <li>Uploading content that infringes on intellectual property rights.</li>
              <li>Attempting to gain unauthorized access to other user accounts or our systems.</li>
              <li>Using the service to transmit malware or viruses.</li>
              <li>Harassing, abusing, or harming another person.</li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">4. Intellectual Property</h2>
            <p className="mb-4">
              The service and its original content (excluding content provided by users), features, and functionality are and will remain the exclusive property of AudioScholar and its licensors.
            </p>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">5. Termination</h2>
            <p className="mb-4">
              We may terminate or suspend your account immediately, without prior notice or liability, for any reason whatsoever, including without limitation if you breach the Terms.
            </p>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">6. Changes to Terms</h2>
            <p className="mb-4">
              We reserve the right, at our sole discretion, to modify or replace these Terms at any time. What constitutes a material change will be determined at our sole discretion.
            </p>

            <h2 className="text-xl font-semibold text-gray-800 dark:text-white mt-8 mb-4">7. Contact Us</h2>
            <p className="mb-4">
              If you have any questions about these Terms, please contact us at:
            </p>
            <p className="font-medium">support@audioscholar.com</p>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
};

export default TermsOfService;