import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const uploadDuration = new Trend('upload_duration');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 5 }, // Ramp up to 5 users
    { duration: '5m', target: 5 },  // Stay at 5 users for 5 minutes
    { duration: '30s', target: 0 }, // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests should be below 2s
    http_req_failed: ['rate<0.01'],    // Error rate should be less than 1%
    errors: ['rate<0.01'],             // Custom error rate should be less than 1%
  },
};

// Global state
let authTokens = {};
let userMetadata = {};

// Test data - realistic user data
const testUsers = [
  { email: 'user1@test.com', password: 'TestPass123!', name: 'Test User 1' },
  { email: 'user2@test.com', password: 'TestPass123!', name: 'Test User 2' },
  { email: 'user3@test.com', password: 'TestPass123!', name: 'Test User 3' },
  { email: 'user4@test.com', password: 'TestPass123!', name: 'Test User 4' },
  { email: 'user5@test.com', password: 'TestPass123!', name: 'Test User 5' },
];

// Helper function to generate random test audio data
function generateTestAudioFile() {
  const sizes = [1024, 5120, 10240, 25600, 51200]; // 1KB to 50KB
  const size = sizes[Math.floor(Math.random() * sizes.length)];
  return new Array(size).fill(0).map(() => Math.floor(Math.random() * 256));
}

// Base URL - modify this for your environment
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Main test function
export default function () {
  const userIndex = (__VU - 1) % testUsers.length; // Distribute users across test accounts
  const currentUser = testUsers[userIndex];
  
  // Ensure user is authenticated
  if (!authTokens[currentUser.email]) {
    authenticateUser(currentUser);
  }

  // Randomly select an operation to perform
  const operation = Math.random();
  
  if (operation < 0.3) {
    // 30% chance - Upload audio file
    uploadAudioFile(currentUser);
  } else if (operation < 0.6) {
    // 30% chance - Fetch metadata
    fetchMetadata(currentUser);
  } else if (operation < 0.8) {
    // 20% chance - Delete metadata (if any exists)
    deleteMetadata(currentUser);
  } else {
    // 20% chance - Logout and re-authenticate
    logoutUser(currentUser);
    authenticateUser(currentUser);
  }

  // Random think time between 1-5 seconds
  sleep(Math.random() * 4 + 1);
}

// Authentication functions
function authenticateUser(user) {
  console.log(`Authenticating user: ${user.email}`);
  
  const loginPayload = {
    email: user.email,
    password: user.password,
  };

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const response = http.post(`${BASE_URL}/api/auth/register`, JSON.stringify(loginPayload), params);
  
  const success = check(response, {
    'auth registration status is 200 or 201': (r) => r.status === 200 || r.status === 201,
  });

  errorRate.add(!success);
  
  if (success && response.status === 201) {
    // User registered successfully, now login
    const loginResponse = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(loginPayload), params);
    
    if (loginResponse.status === 200) {
      const loginData = JSON.parse(loginResponse.body);
      if (loginData.token) {
        authTokens[user.email] = loginData.token;
        console.log(`Authentication successful for: ${user.email}`);
      }
    }
  } else if (response.status === 409) {
    // User already exists, try to login
    const loginResponse = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(loginPayload), params);
    
    if (loginResponse.status === 200) {
      const loginData = JSON.parse(loginResponse.body);
      if (loginData.token) {
        authTokens[user.email] = loginData.token;
        console.log(`Login successful for: ${user.email}`);
      }
    }
  }
}

function logoutUser(user) {
  if (!authTokens[user.email]) return;
  
  const params = {
    headers: {
      'Authorization': `Bearer ${authTokens[user.email]}`,
    },
  };

  const response = http.post(`${BASE_URL}/api/auth/logout`, {}, params);
  
  const success = check(response, {
    'logout status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  
  if (success) {
    delete authTokens[user.email];
    console.log(`Logout successful for: ${user.email}`);
  }
}

// Audio operations
function uploadAudioFile(user) {
  if (!authTokens[user.email]) return;
  
  console.log(`Uploading audio file for user: ${user.email}`);
  
  // Prepare multipart form data
  const audioData = generateTestAudioFile();
  const boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW';
  
  let formData = `--${boundary}\r\n`;
  formData += `Content-Disposition: form-data; name="audioFile"; filename="test_audio_${Date.now()}.mp3"\r\n`;
  formData += `Content-Type: audio/mpeg\r\n\r\n`;
  formData += audioData.join(',') + '\r\n';
  
  // Add optional title and description
  formData += `--${boundary}\r\n`;
  formData += `Content-Disposition: form-data; name="title"\r\n\r\n`;
  formData += `Load Test Audio Upload ${Date.now()}\r\n`;
  
  formData += `--${boundary}\r\n`;
  formData += `Content-Disposition: form-data; name="description"\r\n\r\n`;
  formData += `This is a test audio file uploaded during load testing\r\n`;
  formData += `--${boundary}--\r\n`;

  const params = {
    headers: {
      'Authorization': `Bearer ${authTokens[user.email]}`,
      'Content-Type': `multipart/form-data; boundary=${boundary}`,
    },
  };

  const startTime = Date.now();
  const response = http.post(`${BASE_URL}/api/audio/upload`, formData, params);
  const endTime = Date.now();
  
  uploadDuration.add(endTime - startTime);
  
  const success = check(response, {
    'upload status is 202 or 200': (r) => r.status === 202 || r.status === 200,
    'upload response contains metadata': (r) => r.json('id') !== undefined,
  });

  errorRate.add(!success);
  
  if (success && response.json('id')) {
    // Store metadata ID for potential deletion
    if (!userMetadata[user.email]) {
      userMetadata[user.email] = [];
    }
    userMetadata[user.email].push(response.json('id'));
    console.log(`Upload successful for: ${user.email}, metadata ID: ${response.json('id')}`);
  } else {
    console.log(`Upload failed for: ${user.email}, status: ${response.status}`);
  }
}

function fetchMetadata(user) {
  if (!authTokens[user.email]) return;
  
  console.log(`Fetching metadata for user: ${user.email}`);
  
  const params = {
    headers: {
      'Authorization': `Bearer ${authTokens[user.email]}`,
    },
  };

  const pageSize = Math.floor(Math.random() * 20) + 1; // Random page size 1-20
  const response = http.get(`${BASE_URL}/api/audio/metadata?pageSize=${pageSize}`, params);
  
  const success = check(response, {
    'metadata fetch status is 200': (r) => r.status === 200,
    'metadata response is array': (r) => Array.isArray(r.json()),
  });

  errorRate.add(!success);
  
  if (success) {
    const metadata = response.json();
    console.log(`Metadata fetch successful for: ${user.email}, found ${metadata.length} items`);
  }
}

function deleteMetadata(user) {
  if (!authTokens[user.email] || !userMetadata[user.email] || userMetadata[user.email].length === 0) {
    return;
  }
  
  const metadataId = userMetadata[user.email].pop(); // Get and remove last metadata ID
  console.log(`Deleting metadata for user: ${user.email}, ID: ${metadataId}`);
  
  const params = {
    headers: {
      'Authorization': `Bearer ${authTokens[user.email]}`,
    },
  };

  const response = http.del(`${BASE_URL}/api/audio/metadata/${metadataId}`, {}, params);
  
  const success = check(response, {
    'delete status is 204 or 200': (r) => r.status === 204 || r.status === 200,
  });

  errorRate.add(!success);
  
  if (success) {
    console.log(`Delete successful for: ${user.email}, ID: ${metadataId}`);
  }
}

// Setup function - runs once at the start
export function setup() {
  console.log('Starting AudioScholar Load Test Setup');
  console.log(`Target URL: ${BASE_URL}`);
  console.log(`Test duration: 5 minutes with 5 concurrent users`);
  
  // You can add setup logic here if needed
  return { setupComplete: true };
}

// Teardown function - runs once at the end
export function teardown(data) {
  console.log('Load test completed');
  console.log(`Setup data: ${JSON.stringify(data)}`);
  
  // Cleanup if needed
  console.log(`Total authenticated users: ${Object.keys(authTokens).length}`);
}