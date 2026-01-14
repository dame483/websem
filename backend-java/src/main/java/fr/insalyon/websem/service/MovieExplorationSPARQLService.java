package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
import org.apache.jena.query.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MovieExplorationSPARQLService {

    private static final String DBPEDIA_ENDPOINT = "https://dbpedia.org/sparql";


    // Recherche de films
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

    private String buildSearchMovieQuery(String movieName) {
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            
            SELECT DISTINCT ?movie ?title ?description ?date ?director ?thumbnail
            WHERE {
              ?movie a dbo:Film .
              ?movie rdfs:label ?title .
              FILTER(LANG(?title) = "en")
              FILTER(REGEX(?title, "%s", "i"))
              OPTIONAL { ?movie dbo:description ?description . FILTER(LANG(?description) = "en") }
              OPTIONAL { ?movie dbo:releaseDate ?date }
              OPTIONAL { ?movie dbo:director ?directorRes . ?directorRes rdfs:label ?director . FILTER(LANG(?director) = "en") }
              OPTIONAL { ?movie dbo:thumbnail ?thumbnail }
            }
            LIMIT 20
        """, escapeString(movieName));
    }

    // Graphe des acteurs
    
    // Graphe des producteurs
    
    // Distribution des genres (camembert)

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

    private Movie mapSolutionToMovie(QuerySolution solution) {
        Movie movie = new Movie();
        movie.setUri(getStringValue(solution, "movie"));
        movie.setTitle(getStringValue(solution, "title"));
        movie.setDescription(truncateText(getStringValue(solution, "description"), 300));
        movie.setReleaseDate(getStringValue(solution, "date"));
        movie.setDirector(getStringValue(solution, "director"));
        movie.setThumbnail(getStringValue(solution, "thumbnail"));
        return movie;
    }
}
