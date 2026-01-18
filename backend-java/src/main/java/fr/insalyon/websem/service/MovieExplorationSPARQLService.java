package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
import fr.insalyon.websem.model.Actor;
import fr.insalyon.websem.model.Genre;
import org.apache.jena.query.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MovieExplorationSPARQLService {

    private static final String DBPEDIA_ENDPOINT = "https://dbpedia.org/sparql";


    // Recherche de films Barre de recherche 
    public List<Movie> searchMovies(String movieName) {
        String sparqlQuery = buildSearchMovieQuery(movieName);
        ResultSet results = executeSparqlQuery(sparqlQuery);

        List<Movie> movies = new ArrayList<>();
        if (results != null) {
            while (results.hasNext()) {
                movies.add(mapSolutionToMovie(results.nextSolution()));
            }
        }
        return movies;
    }

    // si on veut le titre exact utilsier : FILTER(?title = "Avatar"@en)
    // TODO : AJOUTER DES FILTER dans la requete pour les filtres de la barre de recherches 
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


    // Graphe des acteurs : acteurs stars du film + leur film le plus rentable
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


    
    // Films récents d’un réalisateur (director)
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



    // Distribution des genres (camembert)
    // méthode get qui renvoie 
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












    // Top films par budget



    // Méthodes utilitaires
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

    private String getStringValue(QuerySolution solution, String varName) {
        if (solution.contains(varName)) {
            String value = solution.get(varName).toString();
            if (value.contains("@")) {
                value = value.substring(0, value.lastIndexOf("@"));
            }
            return value;
        }
        return null;
    }

    private String escapeString(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("'", "\\'");
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    // TODO : ajouter des methodes utilitaires de nettoyages pour les autres attributs, gross, budget... et quand il y 
    // a une liste de valeurs avec virgule (soit changer la nature de l'attribut de Movie et consrtuire une methode qui récupère les infos entre virgule)
    // voir le test pour comprendre l'affichage 
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

    private Actor mapSolutionToActor(QuerySolution sol) {
        Actor actor = new Actor();
        actor.setActorUri(getStringValue(sol, "actor"));
        actor.setActorName(getStringValue(sol, "actorName"));
        actor.setTopMovieUri(getStringValue(sol, "topMovie"));
        actor.setTopMovieTitle(getStringValue(sol, "topMovieTitle"));

        String grossStr = getStringValue(sol, "maxGross");
        if (grossStr != null) {
            actor.setMaxGross(Double.parseDouble(grossStr.replaceAll("[^0-9.]", "")));
        }

        return actor;
    }



}
