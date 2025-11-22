/**
 * Initializes the application's logging behavior based on the environment.
 * 
 * Per requirements:
 * - If the domain is localhost (dev), full debugging logs are enabled.
 * - If the domain is NOT localhost (production/other), debugging logs (log, info, debug) 
 *   are suppressed to make it user-friendly and secure.
 *   Critical errors and warnings are preserved.
 */
export const initLogger = () => {
  const hostname = window.location.hostname;
  const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1';

  if (!isLocalhost) {
    // Override console methods to suppress output
    console.log = () => {};
    console.debug = () => {};
    console.info = () => {};
    
    // Note: console.warn and console.error are preserved for critical runtime errors.
  }
};
