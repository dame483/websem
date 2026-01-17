import React, { useState } from 'react';
import axios from 'axios';
import './App.css';

function extractMovieName(uri) {
  // Extraire le nom du film depuis l'URI DBpedia
  // http://dbpedia.org/resource/Avatar -> Avatar
  if (typeof uri === 'string') {
    return uri.split('/').pop().replace(/_/g, ' ');
  }
  return uri;
}

function App() {
  const [query, setQuery] = useState('');
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searched, setSearched] = useState(false);
  
  // État pour l'agent conversationnel
  const [conversation, setConversation] = useState('');
  const [conversationHistory, setConversationHistory] = useState([]);
  const [conversationLoading, setConversationLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('search'); // 'search' ou 'conversation'

  const handleSearch = async (e) => {
    e.preventDefault();
    
    if (!query.trim()) {
      setError('Veuillez entrer un nom de film');
      return;
    }

    setLoading(true);
    setError(null);
    setSearched(true);

    try {
      const response = await axios.get(`http://localhost:8080/api/movies/search`, {
        params: { query: query }
      });
      setMovies(response.data);
      if (response.data.length === 0) {
        setError('Aucun film trouvé');
      }
    } catch (err) {
      setError('Erreur lors de la recherche. Vérifiez que le backend est démarré.');
      console.error('Erreur:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleConversation = async (e) => {
    e.preventDefault();
    
    if (!conversation.trim()) {
      return;
    }

    const userMessage = conversation;
    setConversation('');
    setConversationLoading(true);
    setError(null);

    try {
      const response = await axios.post(`http://localhost:8080/api/conversation/ask`, {
        question: userMessage
      });
      
      setConversationHistory([
        ...conversationHistory,
        {
          type: 'user',
          text: userMessage
        },
        {
          type: 'assistant',
          question: response.data.question,
          results: response.data.results,
          resultColumns: response.data.results && response.data.results.length > 0 
            ? Object.keys(response.data.results[0]) 
            : [],
          aiAnswer: response.data.aiAnswer,
          error: response.data.error
        }
      ]);
    } catch (err) {
      setError('Erreur lors du traitement de la question.');
      console.error('Erreur:', err);
    } finally {
      setConversationLoading(false);
    }
  };

  const extractMovieName = (uri) => {
    if (!uri) return '';
    const parts = uri.split('/');
    return decodeURIComponent(parts[parts.length - 1].replace(/_/g, ' '));
  };

  return (
    <div className="App">
      <div className="container">
        <header className="header">
          <h1>Films</h1>
        </header>

        {/* Tabs */}
        <div className="tabs">
          <button 
            className={`tab-button ${activeTab === 'search' ? 'active' : ''}`}
            onClick={() => setActiveTab('search')}
          >
            Recherche
          </button>
          <button 
            className={`tab-button ${activeTab === 'conversation' ? 'active' : ''}`}
            onClick={() => setActiveTab('conversation')}
          >
            Agent Conversationnel
          </button>
        </div>

        {/* Tab: Search */}
        {activeTab === 'search' && (
          <>
            <form onSubmit={handleSearch} className="search-form">
              <div className="search-container">
                <input
                  type="text"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Entrez le nom d'un film"
                  className="search-input"
                />
                <button type="submit" className="search-button" disabled={loading}>
                  {loading ? (
                    <span className="loader"></span>
                  ) : (
                    'Rechercher'
                  )}
                </button>
              </div>
            </form>

            {error && (
              <div className="error-message">
                {error}
              </div>
            )}

            {searched && !loading && !error && movies.length > 0 && (
              <div className="results-info">
                {movies.length} résultat{movies.length > 1 ? 's' : ''}
              </div>
            )}

            <div className="movies-list">
              {movies.map((movie, index) => (
                <div key={index} className="movie-card">
                  <div className="movie-content">
                    <h2 className="movie-title">{movie.title || extractMovieName(movie.uri)}</h2>
                    
                    {movie.releaseDate && (
                      <p className="movie-date">
                        {new Date(movie.releaseDate).getFullYear()}
                      </p>
                    )}
                    
                    {movie.director && (
                      <p className="movie-director">
                        Réalisé par {movie.director}
                      </p>
                    )}
                    
                    {movie.abstract_ && (
                      <p className="movie-abstract">{movie.abstract_}</p>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {loading && (
              <div className="loading-container">
                <div className="loading-spinner"></div>
                <p>Recherche en cours sur DBpedia...</p>
              </div>
            )}
          </>
        )}

        {/* Tab: Conversation */}
        {activeTab === 'conversation' && (
          <>
            <div className="conversation-container">
              <div className="conversation-history">
                {conversationHistory.length === 0 && (
                  <p className="conversation-placeholder">Posez une question sur les films...</p>
                )}
                {conversationHistory.map((message, index) => (
                  <div key={index} className={`message ${message.type}`}>
                    {message.type === 'user' ? (
                      <p className="user-message">{message.text}</p>
                    ) : (
                      <div className="assistant-message">
                        {message.error ? (
                          <p className="error-text">{message.error}</p>
                        ) : message.results && message.results.length > 0 ? (
                          <div className="conversation-results">
                            {message.results.map((result, i) => (
                              <div key={i} className="result-card">
                                {result.title && <p className="result-title">{result.title}</p>}
                                {result.film && <p className="result-title">{extractMovieName(result.film)}</p>}
                                {result.movie && <p className="result-title">{extractMovieName(result.movie)}</p>}
                                {result.label && <p className="result-title">{result.label}</p>}
                              </div>
                            ))}
                          </div>
                        ) : message.aiAnswer ? (
                          <p className="ai-answer">{message.aiAnswer}</p>
                        ) : null}
                      </div>
                    )}
                  </div>
                ))}
              </div>

              <form onSubmit={handleConversation} className="conversation-form">
                <div className="conversation-input-container">
                  <input
                    type="text"
                    value={conversation}
                    onChange={(e) => setConversation(e.target.value)}
                    placeholder="Posez votre question sur les films..."
                    className="conversation-input"
                  />
                  <button type="submit" className="send-button" disabled={conversationLoading}>
                    {conversationLoading ? (
                      <span className="loader"></span>
                    ) : (
                      'Envoyer'
                    )}
                  </button>
                </div>
              </form>

              {error && (
                <div className="error-message">
                  {error}
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default App;
