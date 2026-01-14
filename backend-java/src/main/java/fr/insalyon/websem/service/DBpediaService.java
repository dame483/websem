package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
import org.apache.jena.query.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DBpediaService {

    private static final String DBPEDIA_ENDPOINT = "https://dbpedia.org/sparql";

    public List<Movie> searchMovies(String movieName) {
        List<Movie> movies = new ArrayList<>();

        String sparqlQuery = String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX dbr: <http://dbpedia.org/resource/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX dct: <http://purl.org/dc/terms/>
            
            SELECT DISTINCT ?movie ?title ?abstract ?date ?director ?thumbnail
            WHERE {
              ?movie a dbo:Film .
              ?movie rdfs:label ?title .
              
              FILTER(LANG(?title) = "en")
              FILTER(REGEX(?title, "%s", "i"))
              
              OPTIONAL { ?movie dbo:abstract ?abstract . FILTER(LANG(?abstract) = "en") }
              OPTIONAL { ?movie dbo:releaseDate ?date }
              OPTIONAL { ?movie dbo:director ?directorRes . ?directorRes rdfs:label ?director . FILTER(LANG(?director) = "en") }
              OPTIONAL { ?movie dbo:thumbnail ?thumbnail }
            }
            LIMIT 20
            """, escapeString(movieName));

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(DBPEDIA_ENDPOINT, sparqlQuery)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();

                Movie movie = new Movie();
                movie.setUri(getStringValue(solution, "movie"));
                movie.setTitle(getStringValue(solution, "title"));
                movie.setAbstract_(truncateText(getStringValue(solution, "abstract"), 300));
                movie.setReleaseDate(getStringValue(solution, "date"));
                movie.setDirector(getStringValue(solution, "director"));
                movie.setThumbnail(getStringValue(solution, "thumbnail"));

                movies.add(movie);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la requête SPARQL : " + e.getMessage());
            e.printStackTrace();
        }

        return movies;
    }

    private String getStringValue(QuerySolution solution, String varName) {
        if (solution.contains(varName)) {
            String value = solution.get(varName).toString();
            // Nettoyer les tags de langue comme @en, @fr, etc.
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
}
