import React, { useState } from 'react';

const NoteItem = ({ note, onUpdate, onDelete }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editedContent, setEditedContent] = useState(note.content);
  const [editedTags, setEditedTags] = useState(note.tags ? note.tags.join(', ') : '');
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    try {
      setSaving(true);
      const tagsArray = editedTags.split(',').map(tag => tag.trim()).filter(tag => tag !== '');
      await onUpdate(note.noteId, editedContent, tagsArray);
      setIsEditing(false);
    } catch (error) {
      console.error("Failed to update note", error);
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setEditedContent(note.content);
    setEditedTags(note.tags ? note.tags.join(', ') : '');
    setIsEditing(false);
  };

  if (isEditing) {
    return (
      <div className="bg-white p-4 rounded-lg shadow border border-teal-200 mb-4">
        <textarea
          className="w-full p-2 border border-gray-300 rounded-md mb-2 focus:outline-none focus:ring-2 focus:ring-[#2D8A8A]"
          rows="4"
          value={editedContent}
          onChange={(e) => setEditedContent(e.target.value)}
          placeholder="Write your note here..."
        />
        <input
          type="text"
          className="w-full p-2 border border-gray-300 rounded-md mb-4 text-sm focus:outline-none focus:ring-2 focus:ring-[#2D8A8A]"
          value={editedTags}
          onChange={(e) => setEditedTags(e.target.value)}
          placeholder="Tags (comma separated)"
        />
        <div className="flex justify-end space-x-2">
          <button
            onClick={handleCancel}
            className="px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 rounded"
            disabled={saving}
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="px-3 py-1 text-sm text-white bg-[#2D8A8A] hover:bg-[#236b6b] rounded"
            disabled={saving}
          >
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 mb-4 hover:shadow-md transition-shadow">
      <div className="whitespace-pre-wrap text-gray-700 mb-3">{note.content}</div>
      
      {note.tags && note.tags.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-3">
          {note.tags.map((tag, index) => (
            <span key={index} className="bg-teal-50 text-teal-700 text-xs px-2 py-1 rounded-full">
              #{tag}
            </span>
          ))}
        </div>
      )}

      <div className="flex justify-between items-center text-xs text-gray-500 mt-2 border-t pt-2">
        <span>{new Date(note.updatedAt || note.createdAt).toLocaleString()}</span>
        <div className="space-x-2">
          <button
            onClick={() => setIsEditing(true)}
            className="text-[#2D8A8A] hover:text-[#236b6b] font-medium"
          >
            Edit
          </button>
          <button
            onClick={() => onDelete(note.noteId)}
            className="text-red-600 hover:text-red-800 font-medium"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  );
};

export default NoteItem;
