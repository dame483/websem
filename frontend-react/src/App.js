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
  
  // Filtres avancés
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState({
    language: '',
    country: '',
    director: '',
    producer: '',
    yearFrom: '',
    yearTo: '',
    distributor: ''
  });
  
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

  const handleAdvancedSearch = async (e) => {
    e.preventDefault();
    
    // Le titre est obligatoire (depuis la recherche de base)
    if (!query.trim()) {
      setError('Veuillez d\'abord entrer un titre de film');
      return;
    }

    setLoading(true);
    setError(null);
    setSearched(true);

    try {
      const response = await axios.post(
        `http://localhost:8080/api/movies/search-advanced`,
        filters,
        {
          params: { title: query }
        }
      );
      setMovies(response.data);
      if (response.data.length === 0) {
        setError('Aucun film trouvé avec ces critères');
      }
    } catch (err) {
      setError('Erreur lors de la recherche. Vérifiez que le backend est démarré.');
      console.error('Erreur:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (field, value) => {
    setFilters({
      ...filters,
      [field]: value
    });
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
                <button 
                  type="button" 
                  className="filter-toggle-button"
                  onClick={() => setShowFilters(!showFilters)}
                >
                  {showFilters ? '✕ Filtres' : '⚙ Filtres'}
                </button>
              </div>
            </form>

            {/* Formulaire des filtres avancés */}
            {showFilters && (
              <form onSubmit={handleAdvancedSearch} className="advanced-filter-form">
                <div className="filter-grid">
                  <div className="filter-group">
                    <label htmlFor="language">Langue</label>
                    <input
                      id="language"
                      type="text"
                      value={filters.language}
                      onChange={(e) => handleFilterChange('language', e.target.value)}
                      placeholder="Ex: English"
                      className="filter-input"
                    />
                  </div>

                  <div className="filter-group">
                    <label htmlFor="country">Pays</label>
                    <input
                      id="country"
                      type="text"
                      value={filters.country}
                      onChange={(e) => handleFilterChange('country', e.target.value)}
                      placeholder="Ex: United States"
                      className="filter-input"
                    />
                  </div>

                  <div className="filter-group">
                    <label htmlFor="director">Réalisateur</label>
                    <input
                      id="director"
                      type="text"
                      value={filters.director}
                      onChange={(e) => handleFilterChange('director', e.target.value)}
                      placeholder="Ex: James Cameron"
                      className="filter-input"
                    />
                  </div>

                  <div className="filter-group">
                    <label htmlFor="producer">Producteur</label>
                    <input
                      id="producer"
                      type="text"
                      value={filters.producer}
                      onChange={(e) => handleFilterChange('producer', e.target.value)}
                      placeholder="Ex: Jon Landau"
                      className="filter-input"
                    />
                  </div>

                  <div className="filter-group">
                    <label htmlFor="yearFrom">Année de</label>
                    <input
                      id="yearFrom"
                      type="number"
                      value={filters.yearFrom}
                      onChange={(e) => handleFilterChange('yearFrom', e.target.value)}
                      placeholder="Ex: 2000"
                      className="filter-input"
                    />
                  </div>

                  <div className="filter-group">
                    <label htmlFor="yearTo">Année à</label>
                    <input
                      id="yearTo"
                      type="number"
                      value={filters.yearTo}
                      onChange={(e) => handleFilterChange('yearTo', e.target.value)}
                      placeholder="Ex: 2023"
                      className="filter-input"
                    />
                  </div>

                  <div className="filter-group">
                    <label htmlFor="distributor">Distributeur</label>
                    <input
                      id="distributor"
                      type="text"
                      value={filters.distributor}
                      onChange={(e) => handleFilterChange('distributor', e.target.value)}
                      placeholder="Ex: 20th Century"
                      className="filter-input"
                    />
                  </div>
                </div>

                <div className="filter-buttons">
                  <button type="submit" className="filter-search-button" disabled={loading || !query.trim()}>
                    {loading ? 'Recherche en cours...' : 'Affiner la recherche'}
                  </button>
                  <button 
                    type="button"
                    className="filter-reset-button"
                    onClick={() => setFilters({
                      language: '',
                      country: '',
                      director: '',
                      producer: '',
                      yearFrom: '',
                      yearTo: '',
                      distributor: ''
                    })}
                  >
                    Réinitialiser
                  </button>
                </div>
              </form>
            )}

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
                    
                    <div className="movie-grid">
                      {movie.releaseDate && (
                        <div className="movie-info">
                          <span className="info-label">Année :</span>
                          <span className="info-value">{movie.releaseDate}</span>
                        </div>
                      )}
                      
                      {movie.director && (
                        <div className="movie-info">
                          <span className="info-label">Réalisateur :</span>
                          <span className="info-value">{movie.director}</span>
                        </div>
                      )}
                      
                      {movie.producer && (
                        <div className="movie-info">
                          <span className="info-label">Producteur :</span>
                          <span className="info-value">{movie.producer}</span>
                        </div>
                      )}
                      
                      {movie.country && (
                        <div className="movie-info">
                          <span className="info-label">Pays :</span>
                          <span className="info-value">{movie.country}</span>
                        </div>
                      )}
                      
                      {movie.language && (
                        <div className="movie-info">
                          <span className="info-label">Langue :</span>
                          <span className="info-value">{movie.language}</span>
                        </div>
                      )}
                      
                      {movie.editor && (
                        <div className="movie-info">
                          <span className="info-label">Monteur :</span>
                          <span className="info-value">{movie.editor}</span>
                        </div>
                      )}
                      
                      {movie.budget && (
                        <div className="movie-info">
                          <span className="info-label">Budget :</span>
                          <span className="info-value">{movie.budget}</span>
                        </div>
                      )}
                      
                      {movie.gross && (
                        <div className="movie-info">
                          <span className="info-label">Recettes :</span>
                          <span className="info-value">{movie.gross}</span>
                        </div>
                      )}
                      
                      {movie.studio && (
                        <div className="movie-info">
                          <span className="info-label">Studio :</span>
                          <span className="info-value">{movie.studio}</span>
                        </div>
                      )}
                      
                      {movie.musicComposer && (
                        <div className="movie-info">
                          <span className="info-label">Compositeur :</span>
                          <span className="info-value">{movie.musicComposer}</span>
                        </div>
                      )}
                      
                      {movie.runtime && (
                        <div className="movie-info">
                          <span className="info-label">Durée :</span>
                          <span className="info-value">{movie.runtime}</span>
                        </div>
                      )}
                      
                      {movie.distributor && (
                        <div className="movie-info">
                          <span className="info-label">Distributeur :</span>
                          <span className="info-value">{movie.distributor}</span>
                        </div>
                      )}
                    </div>
                    
                    {movie.description && (
                      <p className="movie-description">{movie.description}</p>
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
