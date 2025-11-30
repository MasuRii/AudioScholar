const API_BASE_URL = 'http://localhost:8080/api/notes';

const getAuthHeaders = () => {
  const token = localStorage.getItem('AuthToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
};

export const noteService = {
  createNote: async (recordingId, content, tags = []) => {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ recordingId, content, tags }),
    });
    if (!response.ok) throw new Error('Failed to create note');
    return await response.json();
  },

  getNotes: async (recordingId) => {
    const response = await fetch(`${API_BASE_URL}?recordingId=${recordingId}`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch notes');
    return await response.json();
  },

  getNote: async (noteId) => {
    const response = await fetch(`${API_BASE_URL}/${noteId}`, {
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch note');
    return await response.json();
  },

  updateNote: async (noteId, content, tags = []) => {
    const response = await fetch(`${API_BASE_URL}/${noteId}`, {
      method: 'PATCH',
      headers: getAuthHeaders(),
      body: JSON.stringify({ content, tags }),
    });
    if (!response.ok) throw new Error('Failed to update note');
    return await response.json();
  },

  deleteNote: async (noteId) => {
    const response = await fetch(`${API_BASE_URL}/${noteId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to delete note');
  },
};
