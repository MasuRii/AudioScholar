import { getAuth, sendEmailVerification, signInWithEmailAndPassword } from 'firebase/auth';
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { firebaseApp } from '../../../config/firebaseConfig';
import { signUp } from '../../../services/authService';
import { Footer, Header } from '../../Home/HomePage';

const SignUp = () => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordTouched, setPasswordTouched] = useState(false);
  const [passwordRules, setPasswordRules] = useState({
    length: false,
    upper: false,
    lower: false,
    number: false,
    special: false,
  });
  const [loading, setLoading] = useState(false);
  const [formError, setFormError] = useState(null);
  const [backendError, setBackendError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const navigate = useNavigate();
  const auth = getAuth(firebaseApp);

  const updatePasswordRules = (value) => {
    const rules = {
      length: value.length >= 8,
      upper: /[A-Z]/.test(value),
      lower: /[a-z]/.test(value),
      number: /[0-9]/.test(value),
      special: /[^A-Za-z0-9]/.test(value),
    };
    setPasswordRules(rules);
    return rules;
  };

  const handlePasswordChange = (value) => {
    setPassword(value);
    if (!passwordTouched) {
      setPasswordTouched(true);
    }
    updatePasswordRules(value);
  };

  const isPasswordStrong = (rules) =>
    rules.length && rules.upper && rules.lower && rules.number && rules.special;

  const getPasswordStrength = () => {
    const score = Object.values(passwordRules).filter(Boolean).length;
    if (!passwordTouched || !password) {
      return { label: '', color: 'bg-gray-200', score: 0 };
    }

    if (score <= 2) return { label: 'Weak password', color: 'bg-red-500', score };
    if (score === 3 || score === 4)
      return { label: 'Medium strength password', color: 'bg-yellow-500', score };
    return { label: 'Strong password', color: 'bg-green-500', score };
  };

  const passwordStrength = getPasswordStrength();

  const handleSignUp = async (e) => {
    e.preventDefault();

    setFormError(null);
    setBackendError(null);
    setSuccessMessage(null);

    if (!firstName || !lastName || !email || !password) {
      setFormError('Please fill in all fields.');
      return;
    }

    if (!/\S+@\S+\.\S+/.test(email)) {
      setFormError('Please enter a valid email address.');
      return;
    }

    if (firstName.length < 3 || firstName.length > 50 || lastName.length < 3 || lastName.length > 50) {
      setFormError('First and last names must be between 3 and 50 characters.');
      return;
    }

    const latestRules = updatePasswordRules(password);
    if (!isPasswordStrong(latestRules)) {
      setFormError('Password must be at least 8 characters and include uppercase, lowercase, number, and symbol.');
      return;
    }

    setLoading(true);

    try {
      // Step 1: Call Backend to Create User (Firebase Auth + Firestore Profile)
      // We delegate creation to the backend to ensure the Firestore profile is created 
      // with the correct First and Last names immediately.
      await signUp({
        firstName,
        lastName,
        email,
        password,
        // firebaseUid is not needed here as the backend will create it
      });

      // Step 2: Sign In with the newly created credentials to get the Firebase session
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      const user = userCredential.user;

      // Step 3: Send verification email
      const actionCodeSettings = {
        url: `${window.location.origin}/email-verification`,
        handleCodeInApp: true,
      };
      await sendEmailVerification(user, actionCodeSettings);

      setSuccessMessage('Sign up successful! A verification email has been sent to your address. Please verify your email before signing in.');
      setFirstName('');
      setLastName('');
      setEmail('');
      setPassword('');
      navigate('/verify-email-notice', { state: { email: email } });

    } catch (err) {
      console.error('Sign up error:', err);

      if (err.code === 'auth/email-already-in-use' || (err.status === 409)) {
        setBackendError('This email address is already in use.');
      } else if (err.data && err.data.errors && Array.isArray(err.data.errors)) {
        // Display specific backend validation errors
        const validationErrors = err.data.errors.join(' ');
        setBackendError(`Validation failed: ${validationErrors}`);
      } else {
        setBackendError(err.message || 'An unexpected error occurred during sign up.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Header />
      <main className="flex-grow py-12">
        <title>AudioScholar - Sign Up</title>
        <div className="container mx-auto px-4">
          <form onSubmit={handleSignUp} className="max-w-md mx-auto bg-white p-8 rounded-lg shadow-md">
            <h1 className="text-3xl font-bold text-gray-800 mb-6">Create Account</h1>
            <p className="text-gray-600 mb-8">Start your journey to better learning</p>

            {successMessage ? (
              <div className="text-green-500 text-sm mt-2 text-center">{successMessage}</div>
            ) : (
              <div className="space-y-4">
                <div>
                  <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-1">First Name</label>
                  <input
                    type="text"
                    id="firstName"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                    placeholder="Enter your first name"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
                  <input
                    type="text"
                    id="lastName"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                    placeholder="Enter your last name"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                  <input
                    type="email"
                    id="email"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                    placeholder="Enter your email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                  <input
                    type="password"
                    id="password"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                    placeholder="Create a password"
                    value={password}
                    onChange={(e) => handlePasswordChange(e.target.value)}
                    required
                  />
                  <div className="mt-2">
                    {passwordTouched && password && (
                      <>
                        <div className="h-1 w-full bg-gray-200 rounded-full overflow-hidden">
                          <div
                            className={`h-full ${passwordStrength.color} transition-all`}
                            style={{ width: `${(passwordStrength.score / 5) * 100}%` }}
                          />
                        </div>
                        {passwordStrength.label && (
                          <p className="mt-1 text-xs font-medium text-gray-600">{passwordStrength.label}</p>
                        )}
                      </>
                    )}
                    <ul className="mt-2 space-y-1 text-xs">
                      <li className={passwordRules.length ? 'text-green-600' : 'text-gray-500'}>
                        At least 8 characters
                      </li>
                      <li className={passwordRules.upper ? 'text-green-600' : 'text-gray-500'}>
                        Contains an uppercase letter (A-Z)
                      </li>
                      <li className={passwordRules.lower ? 'text-green-600' : 'text-gray-500'}>
                        Contains a lowercase letter (a-z)
                      </li>
                      <li className={passwordRules.number ? 'text-green-600' : 'text-gray-500'}>
                        Contains a number (0-9)
                      </li>
                      <li className={passwordRules.special ? 'text-green-600' : 'text-gray-500'}>
                        Contains a symbol (e.g. !@#$%)
                      </li>
                    </ul>
                  </div>
                </div>

                {formError && (
                  <p className="text-red-500 text-sm mt-2 text-center">{formError}</p>
                )}

                {backendError && (
                  <p className="text-red-500 text-sm mt-2 text-center">{backendError}</p>
                )}


                <button
                  type="submit"
                  className={`w-full bg-teal-500 text-white py-3 px-4 rounded-lg font-medium transition ${loading ? 'opacity-50 cursor-not-allowed' : 'hover:bg-teal-600'}`}
                  disabled={loading}
                >
                  {loading ? 'Creating Account...' : 'Create Account'}
                </button>
              </div>
            )}

            <div className="mt-6 text-center">
              <p className="text-sm text-gray-600">
                Already have an account?{' '}
                <a href="/signin" className="text-teal-500 hover:text-teal-600 font-medium">Sign in</a>
              </p>
            </div>
          </form>
        </div>
      </main>
      <Footer />
    </>
  );
};

export default SignUp;