import React, { useState } from 'react';
import axios from 'axios';
import './App.css';

function App() {
  const [query, setQuery] = useState('');
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searched, setSearched] = useState(false);

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
                
                {movie.director && (
                  <p className="movie-director">
                    Réalisé par {movie.director}
                  </p>
                )}
                
                {movie.description_ && (
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
      </div>
    </div>
  );
}

export default App;
