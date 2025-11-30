import React, { useEffect, useState } from 'react';
import { noteService } from '../../services/noteService';
import NoteItem from './NoteItem';

const NoteList = ({ recordingId }) => {
  const [notes, setNotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isCreating, setIsCreating] = useState(false);
  const [newNoteContent, setNewNoteContent] = useState('');
  const [newNoteTags, setNewNoteTags] = useState('');

  useEffect(() => {
    const fetchNotes = async () => {
      try {
        setLoading(true);
        const data = await noteService.getNotes(recordingId);
        // Sort by most recent update/creation
        const sortedNotes = data.sort((a, b) => 
          new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt)
        );
        setNotes(sortedNotes);
        setError(null);
      } catch (err) {
        console.error("Error fetching notes:", err);
        setError('Failed to load notes.');
      } finally {
        setLoading(false);
      }
    };

    if (recordingId) {
      fetchNotes();
    }
  }, [recordingId]);

  const handleCreateNote = async () => {
    if (!newNoteContent.trim()) return;

    try {
      const tagsArray = newNoteTags.split(',').map(tag => tag.trim()).filter(tag => tag !== '');
      const newNote = await noteService.createNote(recordingId, newNoteContent, tagsArray);
      setNotes([newNote, ...notes]);
      setNewNoteContent('');
      setNewNoteTags('');
      setIsCreating(false);
    } catch (err) {
      console.error("Error creating note:", err);
      alert('Failed to create note.');
    }
  };

  const handleUpdateNote = async (noteId, content, tags) => {
    try {
      const updatedNote = await noteService.updateNote(noteId, content, tags);
      setNotes(notes.map(note => (note.noteId === noteId ? updatedNote : note)));
    } catch (err) {
      console.error("Error updating note:", err);
      alert('Failed to update note.');
    }
  };

  const handleDeleteNote = async (noteId) => {
    if (!window.confirm("Are you sure you want to delete this note?")) return;
    
    try {
      await noteService.deleteNote(noteId);
      setNotes(notes.filter(note => note.noteId !== noteId));
    } catch (err) {
      console.error("Error deleting note:", err);
      alert('Failed to delete note.');
    }
  };

  if (loading) return <div className="text-center py-4 text-gray-500">Loading notes...</div>;
  if (error) return <div className="text-center py-4 text-red-500">{error}</div>;

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-xl font-semibold text-gray-800">My Notes ({notes.length})</h3>
        {!isCreating && (
          <button
            onClick={() => setIsCreating(true)}
            className="bg-[#2D8A8A] text-white px-4 py-2 rounded-lg hover:bg-[#236b6b] transition shadow-sm"
          >
            + Add Note
          </button>
        )}
      </div>

      {isCreating && (
        <div className="bg-white p-6 rounded-lg shadow-md border border-blue-100 mb-8">
          <h4 className="text-sm font-bold text-gray-700 mb-3 uppercase tracking-wide">New Note</h4>
          <textarea
            className="w-full p-3 border border-gray-300 rounded-md mb-3 focus:outline-none focus:ring-2 focus:ring-[#2D8A8A] min-h-[120px]"
            rows="4"
            value={newNoteContent}
            onChange={(e) => setNewNoteContent(e.target.value)}
            placeholder="Type your thoughts here..."
          />
          <input
            type="text"
            className="w-full p-3 border border-gray-300 rounded-md mb-4 text-sm focus:outline-none focus:ring-2 focus:ring-[#2D8A8A]"
            value={newNoteTags}
            onChange={(e) => setNewNoteTags(e.target.value)}
            placeholder="Tags (comma separated, e.g., exam, important)"
          />
          <div className="flex justify-end space-x-3">
            <button
              onClick={() => setIsCreating(false)}
              className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-md transition"
            >
              Cancel
            </button>
            <button
              onClick={handleCreateNote}
              disabled={!newNoteContent.trim()}
              className={`px-4 py-2 text-white rounded-md transition ${
                !newNoteContent.trim() ? 'bg-gray-400 cursor-not-allowed' : 'bg-[#2D8A8A] hover:bg-[#236b6b]'
              }`}
            >
              Save Note
            </button>
          </div>
        </div>
      )}

      <div className="space-y-4">
        {notes.length === 0 ? (
          <div className="text-center py-10 bg-gray-50 rounded-lg border border-dashed border-gray-300">
            <p className="text-gray-500">No notes yet. Click "Add Note" to get started!</p>
          </div>
        ) : (
          notes.map(note => (
            <NoteItem
              key={note.noteId}
              note={note}
              onUpdate={handleUpdateNote}
              onDelete={handleDeleteNote}
            />
          ))
        )}
      </div>
    </div>
  );
};

export default NoteList;
