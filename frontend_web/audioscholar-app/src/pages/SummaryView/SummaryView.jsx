import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
//text
const Summaryview = () => {
  const { recordingId } = useParams();

  const [summaryData, setSummaryData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('summary');

  useEffect(() => {
    const fetchSummary = async () => {
      try {
        setLoading(true);
        const cachedSummary = localStorage.getItem(`summary_${recordingId}`);

        if (cachedSummary) {
          setSummaryData(JSON.parse(cachedSummary));
          setLoading(false);
        } else {
          const token = localStorage.getItem('AuthToken');
          const response = await fetch(`http://localhost:8080/api/recordings/${recordingId}`, {
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${token}`,
            },
          });

          if (!response.ok) {
            throw new Error('Failed to fetch summary');
          }

          const data = await response.json();
          localStorage.setItem(`summary_${recordingId}`, JSON.stringify(data));
          setSummaryData(data);
        }
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    };

    fetchSummary();
  }, [recordingId]);

  if (loading) {
    return <div className="text-center mt-8">Loading summary...</div>;
  }

  if (error) {
    return <div className="text-center mt-8 text-red-600">Error loading summary: {error.message}</div>;
  }

  if (!summaryData) {
    return <div className="text-center mt-8">No summary data found for ID: {recordingId}.</div>;
  }


  const {
    title,
    course,
    instructor,
    date,
    status,
    keyPoints,
    keyVocabulary,
    detailedSummary,
    practiceQuestions,
    transcript,
    myNotes
  } = summaryData;

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      <header className="bg-[#1A365D] text-white py-4 shadow-sm">
        <div className="container mx-auto px-4 flex justify-between items-center">
          <span className="text-2xl font-bold">AudioScholar</span>
          <Link to="/recordings" className="text-gray-300 hover:text-indigo-400 transition-colors">
            Back to Recordings
          </Link>
        </div>
      </header>

      <main className="flex-grow container mx-auto px-4 py-8">
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="mb-6 border-b pb-4">
            <h1 className="text-2xl font-bold text-gray-800 mb-2">{title}</h1>
            <div className="text-gray-600 text-sm flex items-center space-x-4">
              <span>{course} - {instructor}</span>
              <span>{date}</span>
              {status && (
                <span className={`px-2 py-0.5 text-xs font-semibold rounded-full ${status === 'Completed'
                    ? 'bg-green-100 text-green-800'
                    : 'bg-yellow-100 text-yellow-800' // Adjust colors based on your design
                  }`}>
                  {status}
                </span>
              )}
            </div>
          </div>

          <div className="flex justify-end space-x-4 mb-6">
            <button className="bg-[#2D8A8A] text-white py-2 px-4 rounded-lg font-medium hover:bg-[#236b6b] transition">
              Download Summary
            </button>
            <button className="bg-gray-300 text-gray-800 py-2 px-4 rounded-lg font-medium hover:bg-gray-400 transition">
              Share
            </button>
          </div>

          <div className="border-b border-gray-200 mb-6">
            <nav className="-mb-px flex space-x-8">
              <button
                className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${activeTab === 'summary'
                    ? 'border-blue-600 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                onClick={() => setActiveTab('summary')}
              >
                Summary
              </button>
              <button
                className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${activeTab === 'transcript'
                    ? 'border-blue-600 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                onClick={() => setActiveTab('transcript')}
              >
                Transcript
              </button>
              <button
                className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${activeTab === 'myNotes'
                    ? 'border-blue-600 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                onClick={() => setActiveTab('myNotes')}
              >
                My Notes
              </button>
            </nav>
          </div>

          <div>
            {activeTab === 'summary' && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="md:col-span-2 space-y-6">
                  <section className="bg-gray-50 p-4 rounded-lg">
                    <h3 className="text-md font-semibold text-gray-800 mb-3">Key Points</h3>
                    <ul className="list-disc list-inside text-gray-700 text-sm space-y-1">
                      {keyPoints.map((point, index) => (
                        <li key={index}>{point}</li>
                      ))}
                    </ul>
                  </section>

                  {detailedSummary.map((section, index) => (
                    <section key={index} className="space-y-2">
                      <h3 className="text-md font-semibold text-gray-800">{section.heading}</h3>
                      <p className="text-gray-700 text-sm">{section.content}</p>
                    </section>
                  ))}
                </div>

                <div className="md:col-span-1 space-y-6">
                  <section className="bg-gray-50 p-4 rounded-lg">
                    <h3 className="text-md font-semibold text-gray-800 mb-3">Key Vocabulary</h3>
                    <ul className="text-gray-700 text-sm space-y-2">
                      {keyVocabulary.map((vocab, index) => (
                        <li key={index}>
                          <strong className="block">{vocab.term}:</strong> {vocab.definition}
                        </li>
                      ))}
                    </ul>
                  </section>

                  <section className="bg-blue-50 p-4 rounded-lg">
                    <h3 className="text-md font-semibold text-gray-800 mb-3">Practice Questions</h3>
                    <ul className="list-disc list-inside text-gray-700 text-sm space-y-1">
                      {practiceQuestions.map((question, index) => (
                        <li key={index}>{question}</li>
                      ))}
                    </ul>
                  </section>
                </div>
              </div>
            )}

            {activeTab === 'transcript' && (
              <div className="bg-gray-50 p-4 rounded-lg text-gray-700">
                <h3 className="text-md font-semibold mb-3">Transcript</h3>
                <p>{transcript}</p>
              </div>
            )}

            {activeTab === 'myNotes' && (
              <div className="bg-gray-50 p-4 rounded-lg text-gray-700">
                <h3 className="text-md font-semibold mb-3">My Notes</h3>
                <p>{myNotes}</p>
              </div>
            )}
          </div>
        </div>
      </main>

      <footer className="bg-[#1A365D] text-gray-300 py-8">
        <div className="container mx-auto px-4 grid grid-cols-1 md:grid-cols-4 gap-8">
          <div>
            <h4 className="text-white text-lg font-semibold mb-4">AudioScholar</h4>
            <p className="text-sm">
              Transform your learning experience with AI-powered lecture notes and summaries.
            </p>
          </div>
          <div>
            <h4 className="text-white text-lg font-semibold mb-4">Product</h4>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-indigo-400">Features</a></li>
              <li><a href="#" className="hover:text-indigo-400">Pricing</a></li>
              <li><a href="#" className="hover:text-indigo-400">Use Cases</a></li>
              <li><a href="#" className="hover:text-indigo-400">Support</a></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white text-lg font-semibold mb-4">Company</h4>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-indigo-400">About</a></li>
              <li><a href="#" className="hover:text-indigo-400">Blog</a></li>
              <li><a href="#" className="hover:text-indigo-400">Careers</a></li>
              <li><a href="#" className="hover:text-indigo-400">Contact</a></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white text-lg font-semibold mb-4">Legal</h4>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-indigo-400">Terms</a></li>
              <li><a href="#" className="hover:text-indigo-400">Privacy</a></li>
              <li><a href="#" className="hover:text-indigo-400">Security</a></li>
              <li><a href="#" className="hover:text-indigo-400">Cookies</a></li>
            </ul>
          </div>
        </div>
        <div className="container mx-auto px-4 mt-8 text-center text-gray-500 text-sm">
          &copy; {new Date().getFullYear()} AudioScholar. All rights reserved.
        </div>
      </footer>
    </div>
  );
};

export default Summaryview;