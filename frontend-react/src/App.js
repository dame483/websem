import * as d3 from 'd3';
import React, { useState, useRef, useEffect } from 'react';
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

function GraphVisualization({ selectedMovie, recentMovies, svgRef }) {
  useEffect(() => {
    if (!selectedMovie || recentMovies.length === 0) return;

    const svg = d3.select(svgRef.current);
    svg.selectAll('*').remove();

    const width = 800;
    const height = 600;

    svg.attr('width', width).attr('height', height);

    const nodes = [
      { 
        id: 'main', 
        name: selectedMovie.director,
        isMain: true 
      },
      ...recentMovies.map((m, i) => ({
        id: `movie-${i}`,
        name: m.title || extractMovieName(m.uri),
        year: m.releaseDate,
        isMain: false
      }))
    ];

    const links = recentMovies.map((_, i) => ({
      source: 'main',
      target: `movie-${i}`
    }));

    const simulation = d3.forceSimulation(nodes)
      .force('link', d3.forceLink(links).id(d => d.id).distance(150))
      .force('charge', d3.forceManyBody().strength(-300))
      .force('center', d3.forceCenter(width / 2, height / 2))
      .force('collision', d3.forceCollide().radius(60));

    const g = svg.append('g');

    const zoom = d3.zoom()
      .scaleExtent([0.5, 3])
      .on('zoom', (event) => {
        g.attr('transform', event.transform);
      });

    svg.call(zoom);

    const link = g.append('g')
      .selectAll('line')
      .data(links)
      .join('line')
      .attr('stroke', '#999')
      .attr('stroke-opacity', 0.6)
      .attr('stroke-width', 2);

    const node = g.append('g')
      .selectAll('g')
      .data(nodes)
      .join('g')
      .call(d3.drag()
        .on('start', dragstarted)
        .on('drag', dragged)
        .on('end', dragended));

    node.append('circle')
      .attr('r', d => d.isMain ? 30 : 20)
      .attr('fill', d => d.isMain ? '#ff6b6b' : '#4ecdc4')
      .attr('stroke', '#fff')
      .attr('stroke-width', 2);

    node.append('text')
      .text(d => {
        const name = d.name.length > 20 ? d.name.substring(0, 20) + '...' : d.name;
        return name;
      })
      .attr('text-anchor', 'middle')
      .attr('dy', d => d.isMain ? -40 : -30)
      .attr('font-size', d => d.isMain ? '14px' : '12px')
      .attr('font-weight', d => d.isMain ? 'bold' : 'normal')
      .attr('fill', '#333');

    node.append('text')
      .text(d => d.year ? `(${d.year})` : '')
      .attr('text-anchor', 'middle')
      .attr('dy', d => d.isMain ? 45 : 35)
      .attr('font-size', '10px')
      .attr('fill', '#666');

    simulation.on('tick', () => {
      link
        .attr('x1', d => d.source.x)
        .attr('y1', d => d.source.y)
        .attr('x2', d => d.target.x)
        .attr('y2', d => d.target.y);

      node.attr('transform', d => `translate(${d.x},${d.y})`);
    });

    function dragstarted(event) {
      if (!event.active) simulation.alphaTarget(0.3).restart();
      event.subject.fx = event.subject.x;
      event.subject.fy = event.subject.y;
    }

    function dragged(event) {
      event.subject.fx = event.x;
      event.subject.fy = event.y;
    }

    function dragended(event) {
      if (!event.active) simulation.alphaTarget(0);
      event.subject.fx = null;
      event.subject.fy = null;
    }

    return () => {
      simulation.stop();
    };
  }, [selectedMovie, recentMovies]);

  return (
    <>
      <p style={{ color: '#666', marginBottom: '16px', fontSize: '14px', textAlign: 'center' }}>
       Glissez-déposez les nœuds pour réorganiser.
      </p>
      <div style={{ 
        border: '1px solid #ddd', 
        borderRadius: '8px', 
        overflow: 'hidden',
        backgroundColor: '#f9f9f9',
        marginBottom: '16px'
      }}>
        <svg ref={svgRef} style={{ display: 'block' }} />
      </div>
    </>
  );
}

function App() {
  const [query, setQuery] = useState('');
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searched, setSearched] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [recentMovies, setRecentMovies] = useState([]);
  const [modalLoading, setModalLoading] = useState(false);

  const svgRef = useRef(null);

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

  const openMovieModal = async (movie) => {
    if (!movie.directorUri) return;

    setSelectedMovie(movie);
    setShowModal(true);
    setModalLoading(true);

    try {
      let directorUri = movie.directorUri;
      if (!directorUri.startsWith('http')) {
      directorUri = `http://dbpedia.org/resource/${movie.directorUri.replace(/ /g, '_')}`;
      }

      const encodedUri = encodeURIComponent(directorUri);
      const response = await axios.get(
      `http://localhost:8080/api/movies/recent-by-director?directorUri=${encodedUri}&limit=10`
      );
      console.log(`http://localhost:8080/api/movies/recent-by-director?directorUri=${encodedUri}&limit=10`); //test
      console.log(response.data); //test
      setRecentMovies(response.data); 
    } catch (err) {
      console.error(err);
      setRecentMovies([]);
    } finally {
      setModalLoading(false);
    }
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
                <div key={index} className="movie-card" onClick={() => openMovieModal(movie)}>
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
      {showModal && (
      <div className="modal-overlay" onClick={() => setShowModal(false)}>
        <div className="modal-content modal-content-wide" onClick={(e) => e.stopPropagation()}>
          <h1 style={{ 
            margin: '0 0 24px 0', 
            color: '#1d1d1f', 
            fontSize: '2rem', 
            fontWeight: 600,
            textAlign: 'center',
            borderBottom: '2px solid #d2d2d7',
            paddingBottom: '16px'
          }}>
            Informations complémentaires à propos du film : {selectedMovie?.title || extractMovieName(selectedMovie?.uri)}
          </h1>
          
          <div className="graph-split-container">
            <div className="graph-left">
              <p style={{ 
                textAlign: 'center', 
                color: '#6e6e73', 
                fontSize: '0.9375rem'
              }}>
                Principaux films des acteurs ayant joués dans le film
              </p>
            </div>

            <div className="graph-right">
              <h2 style={{ margin: '0 0 16px 0', textAlign: 'center' }}>
                Films de {selectedMovie?.director}
              </h2>

              {modalLoading ? (
                <div style={{ textAlign: 'center', padding: '40px' }}>
                  <p>Chargement...</p>
                </div>
              ) : recentMovies.length === 0 ? (
                <p style={{ textAlign: 'center', padding: '40px' }}>
                  Aucun autre film trouvé pour ce réalisateur
                </p>
              ) : (
                <>
                  <GraphVisualization 
                    selectedMovie={selectedMovie}
                    recentMovies={recentMovies}
                    svgRef={svgRef}
                  />
                  <p style={{ 
                    fontSize: '12px', 
                    color: '#999', 
                    textAlign: 'center',
                    marginTop: '16px'
                  }}>
                    {recentMovies.length + 1} film{recentMovies.length + 1 > 1 ? 's' : ''} au total
                  </p>
                </>
              )}
            </div>
          </div>

          <button className="close-button" onClick={() => setShowModal(false)}>
            Fermer
          </button>
        </div>
      </div>
    )}
</div>
);
}

export default App;