import * as d3 from 'd3';
import React, { useState, useRef, useEffect } from 'react';
import axios from 'axios';
import './App.css';

function extractMovieName(uri) {
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

    const width = 600;
    const height = 400;
    svg.attr('width', width).attr('height', height);

    const nodes = [
      { id: 'main', name: selectedMovie.director, isMain: true },
      ...recentMovies.map((m, i) => ({
        id: `movie-${i}`,
        name: m.title || extractMovieName(m.uri),
        year: m.releaseDate
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

    svg.call(
      d3.zoom()
        .scaleExtent([0.5, 3])
        .on('zoom', e => g.attr('transform', e.transform))
    );

    const link = g.append('g')
      .selectAll('line')
      .data(links)
      .join('line')
      .attr('stroke', '#fff')
      .attr('stroke-width', 2);

    const node = g.append('g')
      .selectAll('g')
      .data(nodes)
      .join('g')
      .call(
        d3.drag()
          .on('start', e => {
            if (!e.active) simulation.alphaTarget(0.3).restart();
            e.subject.fx = e.subject.x;
            e.subject.fy = e.subject.y;
          })
          .on('drag', e => {
            e.subject.fx = e.x;
            e.subject.fy = e.y;
          })
          .on('end', e => {
            if (!e.active) simulation.alphaTarget(0);
            e.subject.fx = null;
            e.subject.fy = null;
          })
      );

    node.append('circle')
      .attr('r', d => (d.isMain ? 30 : 20))
      .attr('fill', d => (d.isMain ? '#ff6b6b' : '#4ecdc4'));

    node.append('text')
      .text(d => d.name)
      .attr('dy', -30)
      .attr('text-anchor', 'middle');

    simulation.on('tick', () => {
      link
        .attr('x1', d => d.source.x)
        .attr('y1', d => d.source.y)
        .attr('x2', d => d.target.x)
        .attr('y2', d => d.target.y);

      node.attr('transform', d => `translate(${d.x},${d.y})`);
    });

    return () => simulation.stop();
  }, [selectedMovie, recentMovies, svgRef]);

  return (
    <>
      <svg ref={svgRef} />
    </>
  );
}

function ActorsGraphVisualization({ selectedMovie, topActors, svgRef }) {
  useEffect(() => {
    if (!selectedMovie || topActors.length === 0) return;

    const svg = d3.select(svgRef.current);
    svg.selectAll('*').remove();

    const width = 600;
    const height = 400;
    svg.attr('width', width).attr('height', height);

    const nodes = [
      {
        id: 'movie',
        name: selectedMovie.title || extractMovieName(selectedMovie.uri),
        isMain: true
      },
      ...topActors.flatMap((a, i) => [
        { id: `actor-${i}`, name: a.actorName, type: 'actor' },
        { id: `film-${i}`, name: a.topMovieTitle, type: 'movie' }
      ])
    ];

    const links = topActors.flatMap((_, i) => [
      { source: 'movie', target: `actor-${i}` },
      { source: `actor-${i}`, target: `film-${i}` }
    ]);

    const simulation = d3.forceSimulation(nodes)
      .force('link', d3.forceLink(links).id(d => d.id).distance(120))
      .force('charge', d3.forceManyBody().strength(-200))
      .force('center', d3.forceCenter(width / 2, height / 2));

    const g = svg.append('g');

    svg.call(
      d3.zoom()
        .scaleExtent([0.5, 3])
        .on('zoom', e => g.attr('transform', e.transform))
    );

    const link = g.append('g')
      .selectAll('line')
      .data(links)
      .join('line')
      .attr('stroke', '#fff')
      .attr('stroke-width', 2);

    const node = g.append('g')
      .selectAll('g')
      .data(nodes)
      .join('g')
      .call(
        d3.drag()
          .on('start', e => {
            if (!e.active) simulation.alphaTarget(0.3).restart();
            e.subject.fx = e.subject.x;
            e.subject.fy = e.subject.y;
          })
          .on('drag', e => {
            e.subject.fx = e.x;
            e.subject.fy = e.y;
          })
          .on('end', e => {
            if (!e.active) simulation.alphaTarget(0);
            e.subject.fx = null;
            e.subject.fy = null;
          })
      );

    node.append('circle')
      .attr('r', d => (d.isMain ? 30 : 20))
      .attr('fill', d =>
        d.isMain ? '#ff6b6b' : d.type === 'actor' ? '#ffa500' : '#4ecdc4'
      );

    node.append('text')
      .text(d => d.name)
      .attr('dy', -25)
      .attr('text-anchor', 'middle');

    simulation.on('tick', () => {
      link
        .attr('x1', d => d.source.x)
        .attr('y1', d => d.source.y)
        .attr('x2', d => d.target.x)
        .attr('y2', d => d.target.y);

      node.attr('transform', d => `translate(${d.x},${d.y})`);
    });

    return () => simulation.stop();
  }, [selectedMovie, topActors, svgRef]);

  return <svg ref={svgRef} />;
}

function GenreHistogram({ genres }) {
  const ref = useRef();

  useEffect(() => {
    if (!genres || genres.length === 0) return;

    const svg = d3.select(ref.current);
    svg.selectAll('*').remove();

    const width = 500;
    const height = 250;
    const margin = { top: 20, right: 20, bottom: 60, left: 40 };

    const data = genres.slice(0, 10); // top 10 genres

    const x = d3.scaleBand()
      .domain(data.map(d => d.name))
      .range([margin.left, width - margin.right])
      .padding(0.2);

    const y = d3.scaleLinear()
      .domain([0, d3.max(data, d => d.count)])
      .nice()
      .range([height - margin.bottom, margin.top]);

    svg.attr('width', width).attr('height', height);

    svg.append('g')
      .selectAll('rect')
      .data(data)
      .join('rect')
      .attr('x', d => x(d.name))
      .attr('y', d => y(d.count))
      .attr('height', d => y(0) - y(d.count))
      .attr('width', x.bandwidth())
      .attr('fill', '#4ecdc4');

    svg.append('g')
      .attr('transform', `translate(0,${height - margin.bottom})`)
      .call(d3.axisBottom(x))
      .selectAll('text')
      .attr('transform', 'rotate(-30)')
      .style('text-anchor', 'end')
      .style('fill', '#fff');

    svg.append('g')
      .attr('transform', `translate(${margin.left},0)`)
      .call(d3.axisLeft(y))
      .selectAll('text')
      .style('fill', '#fff');

    // Style les lignes des axes en blanc
    svg.selectAll('.domain')
      .style('stroke', '#fff');
    svg.selectAll('.tick line')
      .style('stroke', '#fff');

  }, [genres]);

  return <svg ref={ref} />;
}

function TopBudgetTable({ movies }) {
  return (
    <table className="budget-table">
      <thead>
        <tr>
          <th>Film</th>
          <th>Budget ($)</th>
        </tr>
      </thead>
      <tbody>
        {movies.map((m, i) => (
          <tr key={i}>
            <td>{m.title}</td>
            <td>{Number(m.budget).toLocaleString()}</td>
          </tr>
        ))}
      </tbody>
    </table>
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
  const [topActors, setTopActors] = useState([]);
  const [genresByYear, setGenresByYear] = useState([]);
  const [topBudgetMovies, setTopBudgetMovies] = useState([]);
  const [genresLoading, setGenresLoading] = useState(false);
  const [budgetLoading, setBudgetLoading] = useState(false);

  const svgRef = useRef(null);
  const svgRefActors = useRef(null);

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
  const [activeTab, setActiveTab] = useState('search'); // 'search', 'conversation', 'sparql' ou 'dbpedia'
  
  // État pour la traduction SPARQL
  const [sparqlQuestion, setSparqlQuestion] = useState('');
  const [sparqlResult, setSparqlResult] = useState('');
  const [sparqlLoading, setSparqlLoading] = useState(false);

  // État pour les requêtes DBpedia
  const [dbpediaQuestion, setDbpediaQuestion] = useState('');
  const [dbpediaResult, setDbpediaResult] = useState(null);
  const [dbpediaLoading, setDbpediaLoading] = useState(false);
  const [dbpediaSparql, setDbpediaSparql] = useState('');

  // Etats pour films similaires
  const [similarMovies, setSimilarMovies] = useState([]);
  const [similarLoading, setSimilarLoading] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault();
    
    // Vérifier qu'au moins un critère est renseigné
    const hasAnyFilter = query.trim() || Object.values(filters).some(val => val.trim());
    if (!hasAnyFilter) {
      setError('Veuillez entrer au moins un critère de recherche (titre ou filtre)');
      return;
    }

    setLoading(true);
    setError(null);
    setSearched(true);
    setMovies([]); // Effacer les résultats précédents

    try {
      // Si des filtres sont remplis, utiliser la recherche avancée
      if (Object.values(filters).some(val => val.trim())) {
        const response = await axios.post(
          `http://localhost:8080/api/movies/search-advanced`,
          {
            title: query,
            ...filters
          }
        );
        setMovies(response.data);
        if (response.data.length === 0) {
          setError('Aucun film trouvé avec ces critères');
        }
      } else {
        // Sinon, recherche simple par titre
        const response = await axios.get(`http://localhost:8080/api/movies/search`, {
          params: { query: query }
        });
        setMovies(response.data);
        if (response.data.length === 0) {
          setError('Aucun film trouvé');
        }
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
    
    // Vérifier qu'au moins un filtre est renseigné
    const hasAnyFilter = query.trim() || Object.values(filters).some(val => val.trim());
    if (!hasAnyFilter) {
      setError('Veuillez entrer au moins un critère de recherche (titre ou filtre)');
      return;
    }

    setLoading(true);
    setError(null);
    setSearched(true);
    setMovies([]); // Effacer les résultats précédents

    try {
      const response = await axios.post(
        `http://localhost:8080/api/movies/search-advanced`,
        {
          title: query,
          ...filters
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

  const handleSparqlTranslation = async (e) => {
    e.preventDefault();
    
    if (!sparqlQuestion.trim()) {
      return;
    }

    setSparqlLoading(true);
    setError(null);

    try {
      const response = await axios.post(`http://localhost:8080/api/conversation/translate-to-sparql`, {
        question: sparqlQuestion
      });
      
      if (response.data.error) {
        setSparqlResult(response.data.error);
      } else {
        setSparqlResult(response.data.sparqlQuery);
      }
    } catch (err) {
      setSparqlResult('Erreur lors de la traduction: ' + (err.response?.data?.error || err.message));
      console.error('Erreur:', err);
    } finally {
      setSparqlLoading(false);
    }
  };

  const handleDBpediaQuery = async (e) => {
    e.preventDefault();
    
    if (!dbpediaQuestion.trim()) {
      return;
    }

    setDbpediaLoading(true);
    setError(null);
    setDbpediaResult(null);
    setDbpediaSparql('');

    try {
      const response = await axios.post(`http://localhost:8080/api/conversation/query-dbpedia`, {
        question: dbpediaQuestion
      });
      
      setDbpediaSparql(response.data.sparqlQuery || '');
      
      if (response.data.error) {
        setDbpediaResult({
          type: 'error',
          content: response.data.error
        });
      } else if (response.data.results && response.data.results.length > 0) {
        setDbpediaResult({
          type: 'results',
          content: response.data.results
        });
      } else if (response.data.aiAnswer) {
        setDbpediaResult({
          type: 'ai',
          content: response.data.aiAnswer
        });
      } else {
        setDbpediaResult({
          type: 'empty',
          content: 'Aucun résultat trouvé'
        });
      }
    } catch (err) {
      setDbpediaResult({
        type: 'error',
        content: 'Erreur lors de la requête: ' + (err.response?.data?.error || err.message)
      });
      console.error('Erreur:', err);
    } finally {
      setDbpediaLoading(false);
    }
  };

  const handleClearCache = async () => {
    try {
      await axios.delete(`http://localhost:8080/api/movies/cache/clear`);
      setError('Cache nettoyé avec succès');
      setTimeout(() => setError(null), 2500);
    } catch (err) {
      setError('Erreur lors du nettoyage du cache.');
      console.error('Erreur:', err);
    }
  };

  // Fonction pour récupérer films similaires
  const fetchSimilarMovies = async (movie) => {
    if (!movie.uri) return;

    setSimilarLoading(true);
    setError(null);
    setSimilarMovies([]);

    try {
      const response = await axios.get(`http://localhost:8080/api/movies/similar`, {
        params: { uri: movie.uri }
      });
      setSimilarMovies(response.data);
      if (response.data.length === 0) {
        setError('Aucun film similaire trouvé');
      }
    } catch (err) {
      console.error('Erreur lors de la récupération des films similaires:', err);
      setError('Erreur lors de la récupération des films similaires');
    } finally {
      setSimilarLoading(false);
    }
  };


  const extractMovieName = (uri) => {
    if (!uri) return '';
    const parts = uri.split('/');
    return decodeURIComponent(parts[parts.length - 1].replace(/_/g, ' '));
  };

  const openMovieModal = async (movie) => {
    setSelectedMovie(movie);
    setShowModal(true);
    setModalLoading(true);

    setRecentMovies([]);
    setTopActors([]);
    setGenresByYear([]);
    setTopBudgetMovies([]);

    try {
      let directorUri = movie.directorUri;
      if (!directorUri.startsWith('http')) {
        directorUri = `http://dbpedia.org/resource/${movie.directorUri.replace(/ /g, '_')}`;
      }

      const encodedDirectorUri = encodeURIComponent(directorUri);
      const encodedMovieUri = encodeURIComponent(movie.uri);

      console.log('URL:', `http://localhost:8080/api/movies/recent-by-director?directorUri=${encodedDirectorUri}&limit=10`); //test

      const directorsResponse = await axios.get(
        `http://localhost:8080/api/movies/recent-by-director?directorUri=${encodedDirectorUri}&limit=10`
      );
      
      const  actorsResponse = await axios.get(
        `http://localhost:8080/api/movies/top-actors-by-movie?movieUri=${encodedMovieUri}`,
      );

      setRecentMovies(directorsResponse.data);
      setTopActors(actorsResponse.data);

    } catch (err) {
      console.error('Erreur graphes principaux', err);
      setRecentMovies([]);
      setTopActors([]);
    } finally {
      setModalLoading(false);
    }

    try {
      const releaseDate = movie.releaseDate;

      if (!releaseDate) {
        console.warn('Pas d’année de sortie pour le film');
        return;
      }

      setGenresLoading(true);
      setBudgetLoading(true);

      const [genresResponse, budgetResponse] = await Promise.all([
        axios.get(
          `http://localhost:8080/api/movies/distribution-by-year`,
          { params: { year: releaseDate } }
        ),
        axios.get(
          `http://localhost:8080/api/movies/top-budget-by-year`,
          { params: { year: releaseDate } }
        )
      ]);

      setGenresByYear(genresResponse.data);
      setTopBudgetMovies(budgetResponse.data);

    } catch (err) {
      console.error('Erreur stats annuelles', err);
      setGenresByYear([]);
      setTopBudgetMovies([]);
    } finally {
      setGenresLoading(false);
      setBudgetLoading(false);
    }
  };


  return (
    <div className="App">
      <div className="container">
        <header className="header">
          <h1>FilmPedia</h1>
          <button 
            className="cache-clear-button"
            onClick={handleClearCache}
            title="Nettoyer le cache des requêtes SPARQL"
          >
            Vider le cache
          </button>
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
          <button 
            className={`tab-button ${activeTab === 'sparql' ? 'active' : ''}`}
            onClick={() => setActiveTab('sparql')}
          >
            NLP → SPARQL
          </button>
          <button 
            className={`tab-button ${activeTab === 'dbpedia' ? 'active' : ''}`}
            onClick={() => setActiveTab('dbpedia')}
          >
            Requête DBpedia
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
                  placeholder="Entrez le nom d'un film (optionnel)"
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
                <div className="filter-header">
                  <p className="filter-hint">Combinez les filtres ci-dessous pour affiner votre recherche</p>
                </div>
                
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
                  <button type="submit" className="filter-search-button" disabled={loading}>
                    {loading ? 'Recherche en cours...' : 'Appliquer les filtres'}
                  </button>
                  <button 
                    type="button"
                    className="filter-reset-button"
                    onClick={() => {
                      setFilters({
                        language: '',
                        country: '',
                        director: '',
                        producer: '',
                        yearFrom: '',
                        yearTo: '',
                        distributor: ''
                      });
                    }}
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
                    <button
                      className="similar-button"
                      onClick={() => fetchSimilarMovies(movie)}
                      disabled={similarLoading}
                    >
                      {similarLoading ? 'Chargement...' : 'Films similaires'}
                    </button>

                  </div>
                </div>
              ))}
            </div>

            {similarLoading && (
              <div className="loading-container">
                <div className="loading-spinner"></div>
                <p>Chargement des films similaires...</p>
              </div>
            )}

            {similarMovies.length > 0 && (
              <div className="similar-movies-section">
                <h3>Films similaires</h3>

                <div className="movies-list">
                  {similarMovies.map((movie, index) => (
                    <div key={index} className="movie-card">
                      <div className="movie-content">
                        <h2 className="movie-title">
                          {movie.title || extractMovieName(movie.uri)}
                        </h2>

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
              </div>
            )}


            {loading && (
              <div className="loading-container">
                <div className="loading-spinner"></div>
                <p className="loading-text">Recherche en cours...</p>
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
                          <div className="conversation-results-list">
                            {message.results.map((result, i) => (
                              <div key={i} className="result-item">
                                <div className="result-title">{result.title || result.label || extractMovieName(result.film || result.movie)}</div>
                                {result.director && <div className="result-detail"><span className="result-label">Réalisateur:</span> {result.director}</div>}
                                {result.releaseDate && <div className="result-detail"><span className="result-label">Année:</span> {result.releaseDate}</div>}
                                {result.country && <div className="result-detail"><span className="result-label">Pays:</span> {result.country}</div>}
                                {result.language && <div className="result-detail"><span className="result-label">Langue:</span> {result.language}</div>}
                                {result.description && <div className="result-description">{result.description}</div>}
                              </div>
                            ))}
                          </div>
                        ) : message.aiAnswer ? (
                          <div className="conversation-answer">
                            <div className="answer-text">{message.aiAnswer}</div>
                          </div>
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

        {/* Tab: SPARQL Translation */}
        {activeTab === 'sparql' && (
          <>
            <div className="sparql-container">
              <div className="sparql-section">
                <h2>Traduction NLP → SPARQL</h2>
                <p className="sparql-description">Transformez une phrase en langage naturel en requête SPARQL pour DBpedia</p>
                
                <form onSubmit={handleSparqlTranslation} className="sparql-form">
                  <div className="sparql-input-container">
                    <textarea
                      value={sparqlQuestion}
                      onChange={(e) => setSparqlQuestion(e.target.value)}
                      placeholder="Ex: Quels sont les films réalisés par Christopher Nolan?"
                      className="sparql-textarea"
                      rows="2"
                    />
                    <button type="submit" className="translate-button" disabled={sparqlLoading}>
                      {sparqlLoading ? (
                        <span className="loader"></span>
                      ) : (
                        'Traduire en SPARQL'
                      )}
                    </button>
                  </div>
                </form>

                {sparqlResult && (
                  <div className="sparql-result">
                    <h3>Requête SPARQL générée:</h3>
                    <pre className="sparql-code">{sparqlResult}</pre>
                    <button 
                      className="copy-button"
                      onClick={() => {
                        navigator.clipboard.writeText(sparqlResult);
                        alert('Requête copiée!');
                      }}
                    >
                      Copier la requête
                    </button>
                  </div>
                )}

                {error && (
                  <div className="error-message">
                    {error}
                  </div>
                )}
              </div>
            </div>
          </>
        )}

        {/* Tab: DBpedia Query */}
        {activeTab === 'dbpedia' && (
          <>
            <div className="dbpedia-container">
              <div className="dbpedia-section">
                <h2>Requête DBpedia</h2>
                <p className="dbpedia-description">Posez une question, elle sera traduite en SPARQL et exécutée sur DBpedia</p>
                
                <form onSubmit={handleDBpediaQuery} className="dbpedia-form">
                  <div className="dbpedia-input-container">
                    <textarea
                      value={dbpediaQuestion}
                      onChange={(e) => setDbpediaQuestion(e.target.value)}
                      placeholder="Ex: Quels sont les films réalisés par Christopher Nolan?"
                      className="dbpedia-textarea"
                      rows="2"
                    />
                    <button type="submit" className="query-button" disabled={dbpediaLoading}>
                      {dbpediaLoading ? (
                        <span className="loader"></span>
                      ) : (
                        'Exécuter la requête'
                      )}
                    </button>
                  </div>
                </form>

                {dbpediaSparql && (
                  <div className="sparql-info">
                    <h3>Requête SPARQL utilisée:</h3>
                    <pre className="sparql-code">{dbpediaSparql}</pre>
                  </div>
                )}

                {dbpediaResult && (
                  <div className={`dbpedia-result dbpedia-result-${dbpediaResult.type}`}>
                    {dbpediaResult.type === 'results' && (
                      <>
                        <h3>Résultats DBpedia ({dbpediaResult.content.length}):</h3>
                        <div className="dbpedia-results-list">
                          {dbpediaResult.content.map((result, i) => (
                            <div key={i} className="result-item">
                              <div className="result-title">{result.title || result.label || extractMovieName(result.film || result.movie)}</div>
                              {result.director && <div className="result-detail"><span className="result-label">Réalisateur:</span> {result.director}</div>}
                              {result.releaseDate && <div className="result-detail"><span className="result-label">Année:</span> {result.releaseDate}</div>}
                              {result.country && <div className="result-detail"><span className="result-label">Pays:</span> {result.country}</div>}
                              {result.language && <div className="result-detail"><span className="result-label">Langue:</span> {result.language}</div>}
                            </div>
                          ))}
                        </div>
                      </>
                    )}
                    {dbpediaResult.type === 'ai' && (
                      <>
                        <p className="ai-result-label">DBpedia n'a pas trouvé de résultats. Voici la réponse de l'IA:</p>
                        <div className="ai-result-text">{dbpediaResult.content}</div>
                      </>
                    )}
                    {dbpediaResult.type === 'error' && (
                      <p className="error-text">Erreur: {dbpediaResult.content}</p>
                    )}
                    {dbpediaResult.type === 'empty' && (
                      <p className="empty-text">{dbpediaResult.content}</p>
                    )}
                  </div>
                )}

                {error && (
                  <div className="error-message">
                    {error}
                  </div>
                )}
              </div>
            </div>
          </>
        )}
      </div>
      {showModal && (
      <div className="modal-overlay" onClick={() => setShowModal(false)}>
        <div className="modal-content modal-content-wide" onClick={(e) => e.stopPropagation()}>
          <h1 style={{ 
            margin: '0 0 24px 0', 
            color: 'white', 
            fontSize: '2rem', 
            fontWeight: 700,
            textAlign: 'center',
            borderBottom: '2px solid rgba(255, 255, 255, 0.2)',
            paddingBottom: '16px',
            textShadow: '0 2px 8px rgba(0, 0, 0, 0.2)'
          }}>
            Informations complémentaires à propos du film : {selectedMovie?.title || extractMovieName(selectedMovie?.uri)}
          </h1>
          
          <div className="graph-split-container">
            <div className="graph-left">
              <h2 style={{ margin: '0 0 16px 0', textAlign: 'center', color: 'white', fontWeight: 600 }}>
                Acteurs et leurs succès
              </h2>

              {modalLoading ? (
                <div style={{ textAlign: 'center', padding: '40px' }}>
                  <p>Chargement...</p>
                </div>
              ) : topActors.length === 0 ? (
                <p style={{ textAlign: 'center', padding: '40px' }}>
                  Aucun acteur trouvé pour ce film
                </p>
              ) : (
                <>
                  <ActorsGraphVisualization 
                    selectedMovie={selectedMovie}
                    topActors={topActors}
                    svgRef={svgRefActors}
                  />
                  <p style={{ 
                    fontSize: '12px', 
                    color: '#fff', 
                    textAlign: 'center',
                    marginTop: '16px'
                  }}>
                    {topActors.length} acteur{topActors.length > 1 ? 's' : ''}
                  </p>
                </>
                )}
                <h3 style={{ textAlign: 'center', marginTop: '24px', color: 'white', fontWeight: 600 }}>
                  Répartition des genres ({selectedMovie?.releaseDate})
                </h3>

                {genresLoading ? (
                  <div style={{ textAlign: 'center', padding: '20px' }}>
                    <p>Chargement...</p>
                  </div>
                ) : genresByYear.length === 0 ? (
                  <p style={{ textAlign: 'center' }}>Aucune donnée de genre</p>
                ) : (
                  <GenreHistogram genres={genresByYear} />
                )}
              </div>

            <div className="graph-right">
              <h2 style={{ margin: '0 0 16px 0', textAlign: 'center', color: 'white', fontWeight: 600 }}>
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
                    color: '#fff', 
                    textAlign: 'center',
                    marginTop: '16px'
                  }}>
                    {recentMovies.length + 1} film{recentMovies.length + 1 > 1 ? 's' : ''} au total
                  </p>
                </>
              )}
              <h3 style={{ textAlign: 'center', marginTop: '24px', color: 'white', fontWeight: 600 }}>
                Films au plus gros budget ({selectedMovie?.releaseDate})
              </h3>

              {budgetLoading ? (
                <div style={{ textAlign: 'center', padding: '20px' }}>
                  <p>Chargement...</p>
                </div>
              ) : topBudgetMovies.length === 0 ? (
                <p style={{ textAlign: 'center' }}>Aucun budget trouvé</p>
              ) : (
                <TopBudgetTable movies={topBudgetMovies} />
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