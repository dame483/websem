package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
import fr.insalyon.websem.model.Actor;
import fr.insalyon.websem.model.Genre;
import org.apache.jena.query.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MovieExplorationSPARQLService {

    private static final String DBPEDIA_ENDPOINT = "https://dbpedia.org/sparql";
    
    @Autowired
    private SparqlCacheService cacheService;


    /**
     * Recherche des films dans DBpedia à partir d’un mot-clé sur le titre.
     * Utilisé pour la barre de recherche principale.
     *
     * @param movieName mot-clé saisi par l’utilisateur
     * @return liste de films correspondant au mot-clé
     */
    public List<Movie> searchMovies(String movieName) {
        String sparqlQuery = buildSearchMovieQuery(movieName);
        
        List<Map<String, Object>> cachedResults = cacheService.getCachedResults(sparqlQuery);
        if (cachedResults != null) {
            return convertMapResultsToMovies(cachedResults);
        }
        
        long startTime = System.currentTimeMillis();
        
        ResultSet results = executeSparqlQuery(sparqlQuery);
        
        List<Movie> movies = new ArrayList<>();
        List<Map<String, Object>> resultsToCache = new ArrayList<>();
        
        if (results != null) {
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                movies.add(mapSolutionToMovie(solution));
                
                // Stocker pour le cache
                Map<String, Object> row = new HashMap<>();
                for (String var : results.getResultVars()) {
                    if (solution.contains(var)) {
                        row.put(var, solution.get(var).toString());
                    }
                }
                resultsToCache.add(row);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Mettre en cache
            cacheService.cacheResults(sparqlQuery, resultsToCache);
        }
        
        return movies;
    }

    public List<Movie> searchMoviesWithFilters(String title, String language, String country, 
                                               String director, String producer, String yearFrom, 
                                               String yearTo, String distributor) {
        String sparqlQuery = buildAdvancedSearchQuery(title, language, country, director, producer, 
                                                      yearFrom, yearTo, distributor);
        
        // Vérifier le cache d'abord
        List<Map<String, Object>> cachedResults = cacheService.getCachedResults(sparqlQuery);
        if (cachedResults != null) {
            return convertMapResultsToMovies(cachedResults);
        }
        
        // Exécuter la requête si pas en cache
        long startTime = System.currentTimeMillis();
        
        ResultSet results = executeSparqlQuery(sparqlQuery);
        
        List<Movie> movies = new ArrayList<>();
        List<Map<String, Object>> resultsToCache = new ArrayList<>();
        
        if (results != null) {
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                movies.add(mapSolutionToMovie(solution));
                
                // Stocker pour le cache
                Map<String, Object> row = new HashMap<>();
                for (String var : results.getResultVars()) {
                    if (solution.contains(var)) {
                        row.put(var, solution.get(var).toString());
                    }
                }
                resultsToCache.add(row);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Mettre en cache
            cacheService.cacheResults(sparqlQuery, resultsToCache);
        }
        
        return movies;
    }


    /**
     * Construit la requête SPARQL de recherche de films.
     * La requête récupère les principales informations du film :
     * titre, description, année, réalisateurs, producteurs, budget, gross, etc.
     *
     * @param movieName mot-clé à rechercher dans le titre
     * @return requête SPARQL complète
     */
    private String buildSearchMovieQuery(String movieName) {
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX dbp: <http://dbpedia.org/property/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

            SELECT ?movie 
                (SAMPLE(?titleLabel) AS ?title)
                (SAMPLE(?descriptionLabel) AS ?description)
                (SAMPLE(?thumbnailLabel) AS ?thumbnail)
                (SAMPLE(?runtimeLabel) AS ?runtime)
                (SAMPLE(?grossLabel) AS ?gross)
                (SAMPLE(?budgetLabel) AS ?budget)
                (MAX(?extracted_year) AS ?year)
                (GROUP_CONCAT(DISTINCT STR(?directorRes); separator=", ") AS ?directorUris)
                (GROUP_CONCAT(DISTINCT ?director; separator=", ") AS ?directors)
                (GROUP_CONCAT(DISTINCT ?producer; separator=", ") AS ?producers)
                (GROUP_CONCAT(DISTINCT ?editor; separator=", ") AS ?editors)
                (GROUP_CONCAT(DISTINCT ?studio; separator=", ") AS ?studios)
                (GROUP_CONCAT(DISTINCT ?musicComposer; separator=", ") AS ?musicComposers)
                (GROUP_CONCAT(DISTINCT ?distributor; separator=", ") AS ?distributors)
                (GROUP_CONCAT(DISTINCT ?country; separator=", ") AS ?countries)
                (GROUP_CONCAT(DISTINCT ?language; separator=", ") AS ?languages)
            WHERE {
            # Filtrer les films d'abord (le plus tôt possible)
            ?movie a dbo:Film .
            ?movie rdfs:label ?titleLabel .
            FILTER(LANG(?titleLabel) = "en")
            FILTER(REGEX(?titleLabel, "%s", "i"))
            
            # Extraction de l'année directement (sans sous-requête)
            OPTIONAL { 
                ?movie dbo:description ?desc_with_year .
                FILTER(REGEX(?desc_with_year, "^[0-9]{4}"))
                BIND(xsd:integer(REPLACE(STR(?desc_with_year), "^([0-9]{4}).*", "$1")) AS ?extracted_year)
            }
            
            OPTIONAL { ?movie dbo:description ?descriptionLabel . FILTER(LANG(?descriptionLabel) = "en") }
            OPTIONAL { ?movie dbo:director ?directorRes . ?directorRes rdfs:label ?director . FILTER(LANG(?director) = "en") }
            OPTIONAL { ?movie dbo:producer ?producerRes . ?producerRes rdfs:label ?producer . FILTER(LANG(?producer) = "en") }
            OPTIONAL { ?movie dbo:editing ?editorRes . ?editorRes rdfs:label ?editor . FILTER(LANG(?editor) = "en") }
            OPTIONAL { ?movie dbo:studio ?studio . }
            OPTIONAL { ?movie dbo:musicComposer ?musicComposerRes . ?musicComposerRes rdfs:label ?musicComposer . FILTER(LANG(?musicComposer) = "en") }
            OPTIONAL { ?movie dbo:distributor ?distributorRes . ?distributorRes rdfs:label ?distributor . FILTER(LANG(?distributor) = "en") }
            OPTIONAL { ?movie dbp:country ?country . }
            OPTIONAL { ?movie dbp:language ?language . }
            OPTIONAL { ?movie dbo:runtime ?runtimeLabel . }
            OPTIONAL { ?movie dbo:gross ?grossLabel . FILTER(DATATYPE(?grossLabel) = <http://dbpedia.org/datatype/usDollar>) }
            OPTIONAL { ?movie dbo:budget ?budgetLabel . FILTER(DATATYPE(?budgetLabel) = <http://dbpedia.org/datatype/usDollar>) }
            OPTIONAL { ?movie dbo:thumbnail ?thumbnailLabel }
            }
            GROUP BY ?movie 
            LIMIT 20
        """, escapeString(movieName));
        }

        private String buildAdvancedSearchQuery(String title, String language, String country, 
                                            String director, String producer, String yearFrom, 
                                            String yearTo, String distributor) {
        StringBuilder filters = new StringBuilder();
        
        // Filtre sur le titre
        if (title != null && !title.trim().isEmpty()) {
            filters.append(String.format("FILTER(REGEX(?titleLabel, \"%s\", \"i\"))\n", escapeString(title)));
        }
        
        // Filtre sur la langue
        if (language != null && !language.trim().isEmpty()) {
            filters.append(String.format("FILTER(REGEX(STR(?language), \"%s\", \"i\"))\n", escapeString(language)));
        }
        
        // Filtre sur le pays
        if (country != null && !country.trim().isEmpty()) {
            filters.append(String.format("FILTER(REGEX(STR(?country), \"%s\", \"i\"))\n", escapeString(country)));
        }
        
        // Filtre sur le réalisateur
        if (director != null && !director.trim().isEmpty()) {
            filters.append(String.format("FILTER(REGEX(?director, \"%s\", \"i\"))\n", escapeString(director)));
        }
        
        // Filtre sur le producteur
        if (producer != null && !producer.trim().isEmpty()) {
            filters.append(String.format("FILTER(REGEX(?producer, \"%s\", \"i\"))\n", escapeString(producer)));
        }
        
        // Filtre sur les années
        if (yearFrom != null && !yearFrom.trim().isEmpty()) {
            try {
                int year = Integer.parseInt(yearFrom);
                filters.append(String.format("FILTER(?extracted_year >= %d)\n", year));
            } catch (Exception e) {}
        }
        if (yearTo != null && !yearTo.trim().isEmpty()) {
            try {
                int year = Integer.parseInt(yearTo);
                filters.append(String.format("FILTER(?extracted_year <= %d)\n", year));
            } catch (Exception e) {}
        }
        
        // Filtre sur le distributeur
        if (distributor != null && !distributor.trim().isEmpty()) {
            filters.append(String.format("FILTER(REGEX(?distributor, \"%s\", \"i\"))\n", escapeString(distributor)));
        }
        
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX dbp: <http://dbpedia.org/property/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

            SELECT ?movie 
                   (SAMPLE(?titleLabel) AS ?title)
                   (SAMPLE(?descriptionLabel) AS ?description)
                   (SAMPLE(?thumbnailLabel) AS ?thumbnail)
                   (SAMPLE(?runtimeLabel) AS ?runtime)
                   (SAMPLE(?grossLabel) AS ?gross)
                   (SAMPLE(?budgetLabel) AS ?budget)
                   (MAX(?extracted_year) AS ?year)
                   (GROUP_CONCAT(DISTINCT STR(?directorRes); separator=", ") AS ?directorUris)
                   (GROUP_CONCAT(DISTINCT ?director; separator=", ") AS ?directors)
                   (GROUP_CONCAT(DISTINCT ?producer; separator=", ") AS ?producers)
                   (GROUP_CONCAT(DISTINCT ?editor; separator=", ") AS ?editors)
                   (GROUP_CONCAT(DISTINCT ?studio; separator=", ") AS ?studios)
                   (GROUP_CONCAT(DISTINCT ?musicComposer; separator=", ") AS ?musicComposers)
                   (GROUP_CONCAT(DISTINCT ?distributor; separator=", ") AS ?distributors)
                   (GROUP_CONCAT(DISTINCT ?country; separator=", ") AS ?countries)
                   (GROUP_CONCAT(DISTINCT ?language; separator=", ") AS ?languages)
            WHERE {
              ?movie a dbo:Film .
              ?movie rdfs:label ?titleLabel .
              FILTER(LANG(?titleLabel) = "en")

              OPTIONAL { ?movie dbo:description ?descriptionLabel . FILTER(LANG(?descriptionLabel) = "en") }
               
              OPTIONAL { 
                  SELECT ?movie (MAX(xsd:integer(REPLACE(STR(?desc), "^([0-9]{4}).*", "$1"))) AS ?extracted_year)
                  WHERE {
                  ?movie dbo:description ?desc .
                  FILTER(REGEX(?desc, "^[0-9]{4}"))
                  }
                  GROUP BY ?movie
              }
              OPTIONAL { ?movie dbo:director ?directorRes . ?directorRes rdfs:label ?director .FILTER(LANG(?director) = "en")}
              OPTIONAL { ?movie dbo:producer ?producerRes . ?producerRes rdfs:label ?producer . FILTER(LANG(?producer) = "en") }
              OPTIONAL { ?movie dbo:editing ?editorRes . ?editorRes rdfs:label ?editor . FILTER(LANG(?editor) = "en") }
              OPTIONAL { ?movie dbo:studio ?studio . }
              OPTIONAL { ?movie dbo:musicComposer ?musicComposerRes . ?musicComposerRes rdfs:label ?musicComposer . FILTER(LANG(?musicComposer) = "en") }
              OPTIONAL { ?movie dbo:distributor ?distributorRes . ?distributorRes rdfs:label ?distributor . FILTER(LANG(?distributor) = "en") }
              OPTIONAL { ?movie dbp:country ?country . }
              OPTIONAL { ?movie dbp:language ?language . }
              OPTIONAL { ?movie dbo:runtime ?runtimeLabel . }
              OPTIONAL { ?movie dbo:gross ?grossLabel . FILTER(DATATYPE(?grossLabel) = <http://dbpedia.org/datatype/usDollar>) }
              OPTIONAL { ?movie dbo:budget ?budgetLabel . FILTER(DATATYPE(?budgetLabel) = <http://dbpedia.org/datatype/usDollar>) }
              OPTIONAL { ?movie dbo:thumbnail ?thumbnailLabel }
              
              %s
            }
            GROUP BY ?movie 
            LIMIT 20
        """, filters.toString());
    }


    /**
     * Récupère les acteurs principaux d’un film ainsi que,
     * pour chacun d’eux, le film le plus rentable de leur carrière.
     * Utilisé pour construire le graphe des acteurs.
     *
     * @param movieUri URI DBpedia du film
     * @return liste d’acteurs avec leur film le plus rentable
     */
    public List<Actor> getTopActorsByMovie(String movieUri) {
        String sparqlQuery = buildTopActorsByMovieQuery(movieUri);
        ResultSet results = executeSparqlQuery(sparqlQuery);

        List<Actor> actors = new ArrayList<>();

        if (results != null) {
            while (results.hasNext()) {
                actors.add(mapSolutionToActor(results.nextSolution()));
            }
        }
        return actors;
    }


    /**
     * Construit la requête SPARQL permettant de :
     * - récupérer les acteurs d’un film
     * - calculer pour chacun leur film ayant le plus gros box-office
     *
     * @param movieUri URI DBpedia du film
     * @return requête SPARQL complète
     */
    private String buildTopActorsByMovieQuery(String movieUri) {
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

            SELECT ?actor ?actorName ?topMovie ?topMovieTitle ?maxGross
            WHERE {
            {
                SELECT ?actor (MAX(xsd:decimal(?gross)) AS ?maxGross)
                WHERE {
                <%s> dbo:starring ?actor .
                ?anyMovie dbo:starring ?actor .
                ?anyMovie dbo:gross ?gross .
                FILTER(DATATYPE(?gross) = <http://dbpedia.org/datatype/usDollar>)
                }
                GROUP BY ?actor
            }

            ?topMovie dbo:starring ?actor .
            ?topMovie dbo:gross ?grossValue .
            FILTER(xsd:decimal(?grossValue) = ?maxGross)
            FILTER(DATATYPE(?grossValue) = <http://dbpedia.org/datatype/usDollar>)

            ?topMovie rdfs:label ?topMovieTitle .
            FILTER(LANG(?topMovieTitle) = "en")

            ?actor rdfs:label ?actorName .
            FILTER(LANG(?actorName) = "en")
            }
            ORDER BY DESC(?maxGross)
            """, movieUri);
    }


    
    /**
     * Récupère les films les plus récents d’un réalisateur donné.
     *
     * @param directorUri URI DBpedia du réalisateur
     * @return liste des 5 films les plus récents
     */
    public List<Movie> getRecentMoviesByDirector(String directorUri) {
        String sparqlQuery = buildRecentMoviesByDirectorQuery(directorUri);
        ResultSet results = executeSparqlQuery(sparqlQuery);

        List<Movie> movies = new ArrayList<>();
        if (results != null) {
            while (results.hasNext()) {
                movies.add(mapSolutionToMovie(results.nextSolution()));
            }
        }
        return movies;
    }


    /**
    * Construit la requête SPARQL permettant d’obtenir
    * les films les plus récents d’un réalisateur,
    * en extrayant l’année depuis la description.
    *
    * @param directorUri URI DBpedia du réalisateur
    * @return requête SPARQL complète
    */
    private String buildRecentMoviesByDirectorQuery(String directorUri) {
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

            SELECT ?movie (SAMPLE(?titleLabel) AS ?title) (MAX(?yearLabel) AS ?year) (SAMPLE(?descriptionLabel) AS ?description)
            WHERE {
            ?movie dbo:director <%s> .
            ?movie a dbo:Film .
            ?movie rdfs:label ?titleLabel .
            ?movie dbo:description ?descriptionLabel .
            
            FILTER(REGEX(?descriptionLabel, "^[0-9]{4}"))
            
            BIND(xsd:integer(REPLACE(STR(?descriptionLabel), "^([0-9]{4}).*", "$1")) AS ?yearLabel)
            }
            GROUP BY ?movie
            ORDER BY DESC(?year)
            LIMIT 5
        """, directorUri);
    }



    /**
     * Récupère la distribution des genres normalisés pour une année donnée.
     * Les genres sont regroupés par premier mot (ex: "Action film" → "Action").
     *
     * @param year année recherchée (ex: "1999")
     * @return liste de genres utilisés cette année avec leur nombre de films
     */
    public List<Genre> getAllNormalizedGenresByYear(String year) {

        Map<String, Genre> normalizedGenres = fetchNormalizedGenres();
        ResultSet dbpResults = executeSparqlQuery(buildGenreDistributionQuery(year));

        while (dbpResults != null && dbpResults.hasNext()) {
            QuerySolution sol = dbpResults.nextSolution();

            String firstWord = sol.get("cleanGenre").asLiteral().getString();
            int count = sol.get("count").asLiteral().getInt();
            String rawGenre = sol.get("genre").toString();

            if (normalizedGenres.containsKey(firstWord)) {
                Genre g = normalizedGenres.get(firstWord);
                g.setCount(g.getCount() + count);   
                g.getRawGenres().add(rawGenre);
            }
        }

         return normalizedGenres.values().stream()
            .filter(g -> g.getCount() > 0)
            .sorted(Comparator.comparingInt(Genre::getCount).reversed())
            .toList();
    }


    /**
     * Récupère tous les genres existants dans DBpedia et construit
     * une base de genres normalisés (premier mot) initialisés à 0.
     *
     * @return map <genre normalisé, objet Genre>
     */
    public Map<String, Genre> fetchNormalizedGenres() {
        String dboQuery = """
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

            SELECT DISTINCT ?genre ?genreLabel ?firstWord
            WHERE {
                ?movie a dbo:Film .
                ?movie dbo:genre ?genre .

                OPTIONAL { ?genre rdfs:label ?genreLabel . FILTER(LANG(?genreLabel) = "en") }

                BIND(
                    IF(BOUND(?genreLabel), STR(?genreLabel), STR(?genre)) AS ?rawGenre
                )

                BIND(REPLACE(?rawGenre, "^([^ ]+).*", "$1") AS ?firstWord)
            }
        """;

        ResultSet results = executeSparqlQuery(dboQuery);
        Map<String, Genre> normalizedGenres = new HashMap<>();

        while (results != null && results.hasNext()) {
            QuerySolution sol = results.nextSolution();
            String firstWord = sol.get("firstWord").asLiteral().getString();
            String rawGenre = sol.contains("genreLabel") ? sol.get("genreLabel").asLiteral().getString() : sol.get("genre").toString();
            normalizedGenres.put(firstWord, new Genre(firstWord, 0, new ArrayList<>(List.of(rawGenre))));
        }

        return normalizedGenres;
    }


   
    /**
     * Construit la requête SPARQL qui compte le nombre de films par genre
     * pour une année donnée.
     *
     * @param year année recherchée
     * @return requête SPARQL complète
     */
    private String buildGenreDistributionQuery(String year) {
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX dbp: <http://dbpedia.org/property/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            
            SELECT ?genre ?cleanGenre (COUNT(DISTINCT ?movie) AS ?count)
            WHERE {
            ?movie a dbo:Film .
            ?movie dbo:description ?desc .
            ?movie dbp:genre ?genre .
            
            FILTER(REGEX(?desc, "^%s"))
            
            OPTIONAL { 
                ?genre rdfs:label ?genreLabel . 
                FILTER(LANG(?genreLabel) = "en") 
            }
            
            BIND(
                IF(BOUND(?genreLabel), 
                STR(?genreLabel), 
                STR(?genre)
                ) AS ?rawGenre
            )
            
            BIND(REPLACE(?rawGenre, "^([^ ]+).*", "$1") AS ?cleanGenre)
            
            FILTER(?cleanGenre != "" && !REGEX(?cleanGenre, "^http"))
            }
            GROUP BY ?genre ?cleanGenre
            ORDER BY DESC(?count)
            """, year);
    }
    

    /**
     * Compte le nombre total de films sortis une année donnée.
     * Utile pour calculer des proportions ou statistiques globales.
     *
     * @param year année recherchée
     * @return nombre total de films
     */
    public int countMoviesByYear(String year) {
        String query = String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            SELECT (COUNT(DISTINCT ?movie) AS ?total)
            WHERE {
                ?movie a dbo:Film .
                ?movie dbo:description ?desc .
                FILTER(REGEX(?desc, "^%s"))
            }
        """, year);

        ResultSet results = executeSparqlQuery(query);
        if(results != null && results.hasNext()) {
            return results.nextSolution().getLiteral("total").getInt();
        }
        return 0;
    }


    /**
     * Récupère les 10 films à plus gros budget pour une année donnée.
     *
     * @param year année des films à récupérer
     * @return liste de Movie triés par budget décroissant
     */
    public List<Movie> getTopBudgetMoviesByYear(String year) {
        String sparqlQuery = buildTopBudgetMoviesQuery(year);
        ResultSet results = executeSparqlQuery(sparqlQuery);

        List<Movie> movies = new ArrayList<>();
        if (results != null) {
            while (results.hasNext()) {
                movies.add(mapSolutionToTopBudgetMovie(results.nextSolution()));
            }
        }
        return movies;
    }

    /**
     * Construit la requête SPARQL pour récupérer les 10 films à plus gros budget d’une année donnée.
     *
     * @param year année des films
     * @return requête SPARQL complète
     */
    private String buildTopBudgetMoviesQuery(String year) {
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX dbp: <http://dbpedia.org/property/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

            SELECT DISTINCT ?movie ?title ?budget
            WHERE {
            ?movie a dbo:Film .
            ?movie rdfs:label ?title .
            ?movie dbo:description ?desc .

            FILTER(LANG(?title) = "en")
            FILTER(REGEX(?desc, "^%s"))

            {
                ?movie dbo:budget ?budget .
                FILTER(DATATYPE(?budget) = <http://dbpedia.org/datatype/usDollar>)
            }
            UNION
            {
                ?movie dbp:budget ?budget .
                FILTER(DATATYPE(?budget) = <http://dbpedia.org/datatype/usDollar>)
            }
            }
            ORDER BY DESC(xsd:decimal(?budget))
            LIMIT 10
        """, year);
    }



    // Méthodes utilitaires

    /**
     * Exécute une requête SPARQL sur l’endpoint DBpedia.
     *
     * @param sparqlQuery requête SPARQL complète
     * @return ResultSet contenant les résultats ou null en cas d’erreur
     */
    private ResultSet executeSparqlQuery(String sparqlQuery) {
        try {
            QueryExecution qexec = QueryExecutionFactory.sparqlService(DBPEDIA_ENDPOINT, sparqlQuery);
            return qexec.execSelect();
        } catch (Exception e) {
            System.err.println("Erreur lors de la requête SPARQL : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Convertit une List de Maps (provenant du cache) en List de Movies
     */
    private List<Movie> convertMapResultsToMovies(List<Map<String, Object>> results) {
        List<Movie> movies = new ArrayList<>();
        for (Map<String, Object> row : results) {
            Movie movie = new Movie();
            movie.setUri(getStringFromMap(row, "movie"));
            movie.setTitle(getStringFromMap(row, "title"));
            movie.setDescription(truncateText(getStringFromMap(row, "description"), 300));
            movie.setReleaseDate(getStringFromMap(row, "year"));
            movie.setDirector(getStringFromMap(row, "directors"));
            movie.setDirectorUri(getStringFromMap(row, "directorUris"));
            movie.setProducer(getStringFromMap(row, "producers"));
            movie.setEditor(getStringFromMap(row, "editors"));
            movie.setStudio(getStringFromMap(row, "studios"));
            movie.setMusicComposer(getStringFromMap(row, "musicComposers"));
            movie.setRuntime(formatRuntime(getStringFromMap(row, "runtime")));
            movie.setDistributor(getStringFromMap(row, "distributors"));
            movie.setCountry(getStringFromMap(row, "countries"));
            movie.setLanguage(getStringFromMap(row, "languages"));
            movie.setGross(formatCurrency(getStringFromMap(row, "gross")));
            movie.setBudget(formatCurrency(getStringFromMap(row, "budget")));
            movie.setThumbnail(getStringFromMap(row, "thumbnail"));
            movies.add(movie);
        }
        return movies;
    }
    
    /**
     * Récupère une valeur String d'une Map
     */
    private String getStringFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        String strValue = value.toString();
        // Nettoyer les suffixes de type RDF (^^http://...)
        if (strValue.contains("^^http")) {
            strValue = strValue.substring(0, strValue.indexOf("^^http"));
        }
        // Nettoyer les suffixes de langue (@en, @fr, etc.)
        if (strValue.contains("@")) {
            strValue = strValue.substring(0, strValue.lastIndexOf("@"));
        }
        return strValue.isEmpty() ? null : strValue;
    }


    /**
     * Récupère proprement la valeur d’une variable SPARQL.
     * Supprime les tags de langue si présents.
     *
     * @param solution solution SPARQL courante
     * @param varName nom de la variable
     * @return valeur sous forme de chaîne ou null
     */
    private String getStringValue(QuerySolution solution, String varName) {
        if (!solution.contains(varName)) return null;

        if (solution.get(varName).isLiteral()) {
            return solution.getLiteral(varName).getLexicalForm();
        }

        return solution.get(varName).toString();
    }


    /**
     * Protège une chaîne contre les caractères spéciaux pour SPARQL.
     */
    private String escapeString(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("'", "\\'");
    }


    /**
     * Tronque un texte à une longueur maximale.
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Formate une valeur de runtime (ex: "120" en "120 min").
     */
    private String formatRuntime(String runtime) {
        if (runtime == null || runtime.isEmpty()) return null;
        return runtime + " min";
    }

    /**
     * Formate une valeur de devise (ex: "1000000" en "$1,000,000").
     */
    private String formatCurrency(String currency) {
        if (currency == null || currency.isEmpty()) return null;
        try {
            long value = Long.parseLong(currency);
            return String.format("$%,d", value);
        } catch (NumberFormatException e) {
            return currency;
        }
    }

    /**
     * Transforme une solution SPARQL en objet Movie.
     */
    private Movie mapSolutionToMovie(QuerySolution solution) {
        Movie movie = new Movie();
        movie.setUri(getStringValue(solution, "movie"));
        movie.setTitle(getStringValue(solution, "title"));
        movie.setDescription(truncateText(getStringValue(solution, "description"), 300));
        movie.setReleaseDate(getStringValue(solution, "year")); // année extraite
        movie.setDirector(getStringValue(solution, "directors"));
        movie.setDirectorUri(getStringValue(solution, "directorUris"));
        movie.setProducer(getStringValue(solution, "producers"));
        movie.setEditor(getStringValue(solution, "editors"));
        movie.setStudio(getStringValue(solution, "studios"));
        movie.setMusicComposer(getStringValue(solution, "musicComposers"));
        movie.setRuntime(getStringValue(solution, "runtime"));
        movie.setDistributor(getStringValue(solution, "distributors"));
        movie.setCountry(getStringValue(solution, "countries"));
        movie.setLanguage(getStringValue(solution, "languages"));
        movie.setGross(getStringValue(solution, "gross"));
        movie.setBudget(getStringValue(solution, "budget"));
        movie.setThumbnail(getStringValue(solution, "thumbnail"));
        return movie;
    }

    
    /**
     * Transforme une solution SPARQL en objet Actor.
     */
    private Actor mapSolutionToActor(QuerySolution sol) {
        Actor actor = new Actor();
        actor.setActorUri(getStringValue(sol, "actor"));
        actor.setActorName(getStringValue(sol, "actorName"));
        actor.setTopMovieUri(getStringValue(sol, "topMovie"));
        actor.setTopMovieTitle(getStringValue(sol, "topMovieTitle"));

        if (sol.contains("maxGross") && sol.get("maxGross").isLiteral()) {
            actor.setMaxGross(sol.get("maxGross").asLiteral().getDouble());
        }

        return actor;
    }

    /**
     * Transforme une solution SPARQL en objet Movie pour le top des budgets.
     *
     * @param sol solution SPARQL contenant au moins ?movie, ?title et éventuellement ?budget
     * @return Movie avec URI, titre et budget (si présent)
     */
    private Movie mapSolutionToTopBudgetMovie(QuerySolution sol) {
        Movie movie = new Movie();
        movie.setUri(getStringValue(sol, "movie"));
        movie.setTitle(getStringValue(sol, "title"));
        
        if (sol.contains("budget") && sol.get("budget").isLiteral()) {
            movie.setBudget(sol.get("budget").asLiteral().getString());
        }
        return movie;
    }
}