import { getAuth, verifyPasswordResetCode } from 'firebase/auth';
import React, { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { firebaseApp } from '../../../config/firebaseConfig';
import ResetPassword from './ResetPassword';

const ResetPasswordRoute = () => {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [oobCode, setOobCode] = useState(null);
    const location = useLocation();
    const auth = getAuth(firebaseApp);

    useEffect(() => {
        const queryParams = new URLSearchParams(location.search);
        const code = queryParams.get('oobCode');

        if (code) {
            verifyPasswordResetCode(auth, code)
                .then(() => {
                    setOobCode(code);
                    setLoading(false);
                })
                .catch((err) => {
                    setError(err.message);
                    setLoading(false);
                });
        } else {
            setError('No password reset code found in the URL.');
            setLoading(false);
        }
    }, [auth, location.search]);

    if (loading) {
        return <div>Loading...</div>;
    }

    if (error) {
        return <Navigate to="/signin" state={{ error }} />;
    }

    return <ResetPassword oobCode={oobCode} />;
};

export default ResetPasswordRoute;
