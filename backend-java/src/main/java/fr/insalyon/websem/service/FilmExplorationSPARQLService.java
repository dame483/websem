package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
import fr.insalyon.websem.model.ActorGraphNode;
import fr.insalyon.websem.model.ProducerGraphNode;
import org.apache.jena.query.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FilmExplorationSPARQLService {

    private static final String DBPEDIA_ENDPOINT = "https://dbpedia.org/sparql";

    // =====================
    // 1️⃣ Recherche de films
    // =====================
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
              FILTER(REGEX(?title, "Inception", "i"))
              OPTIONAL { ?movie dbo:description ?description . FILTER(LANG(?description) = "en") }
              OPTIONAL { ?movie dbo:releaseDate ?date }
              OPTIONAL { ?movie dbo:director ?directorRes . ?directorRes rdfs:label ?director . FILTER(LANG(?director) = "en") }
              OPTIONAL { ?movie dbo:thumbnail ?thumbnail }
            }
            LIMIT 20
        """, escapeString(movieName));
    }

/*
    // =====================
    // 2️⃣ Graphe des acteurs
    // =====================
    public List<ActorGraphNode> getActorGraph(String movieUri) {
        String sparqlQuery = buildActorGraphQuery(movieUri);
        ResultSet results = executeSparqlQuery(sparqlQuery);

        List<ActorGraphNode> graph = new ArrayList<>();
        if (results != null) {
            while (results.hasNext()) {
                graph.add(mapSolutionToActorNode(results.nextSolution()));
            }
        }
        return graph;
    }

    private String buildActorGraphQuery(String movieUri) {
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

            SELECT ?actor ?otherMovie ?otherTitle ?budget
            WHERE {
              <%s> dbo:starring ?actor .
              ?otherMovie dbo:starring ?actor .
              ?otherMovie dbo:budget ?budget .
              ?otherMovie rdfs:label ?otherTitle .
              FILTER(LANG(?otherTitle) = "en")
            }
            ORDER BY DESC(?budget)
            LIMIT 1
        """, movieUri);
    }

    // =====================
    // 3️⃣ Graphe des producteurs
    // =====================
    public List<ProducerGraphNode> getProducerGraph(String movieUri) {
        String sparqlQuery = buildProducerGraphQuery(movieUri);
        ResultSet results = executeSparqlQuery(sparqlQuery);

        List<ProducerGraphNode> graph = new ArrayList<>();
        if (results != null) {
            while (results.hasNext()) {
                graph.add(mapSolutionToProducerNode(results.nextSolution()));
            }
        }
        return graph;
    }

    private String buildProducerGraphQuery(String movieUri) {
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

            SELECT ?producer ?otherMovie ?otherTitle ?releaseDate
            WHERE {
              <%s> dbo:producer ?producer .
              ?otherMovie dbo:producer ?producer .
              ?otherMovie rdfs:label ?otherTitle .
              ?otherMovie dbo:releaseDate ?releaseDate .
              FILTER(LANG(?otherTitle) = "en")
            }
            ORDER BY DESC(?releaseDate)
            LIMIT 10
        """, movieUri);
    }

    // =====================
    // 4️⃣ Distribution des genres (camembert)
    // =====================
    public Map<String, Integer> getGenreDistribution(int year) {
        String sparqlQuery = buildGenreDistributionQuery(year);
        ResultSet results = executeSparqlQuery(sparqlQuery);

        Map<String, Integer> distribution = new HashMap<>();
        if (results != null) {
            while (results.hasNext()) {
                QuerySolution sol = results.nextSolution();
                String genre = getStringValue(sol, "genre");
                int count = sol.getLiteral("count").getInt();
                distribution.put(genre, count);
            }
        }
        return distribution;
    }

    private String buildGenreDistributionQuery(int year) {
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

            SELECT ?genre (COUNT(?film) AS ?count)
            WHERE {
              ?film a dbo:Film .
              ?film dbo:releaseDate ?date .
              ?film dbo:genre ?genre .
              FILTER(YEAR(?date) = %d)
            }
            GROUP BY ?genre
        """, year);
    }

    // =====================
    // 5️⃣ Top films par budget
    // =====================
    public List<Movie> getTopBudgetMovies(int year) {
        String sparqlQuery = buildTopBudgetMoviesQuery(year);
        ResultSet results = executeSparqlQuery(sparqlQuery);

        List<Movie> movies = new ArrayList<>();
        if (results != null) {
            while (results.hasNext()) {
                movies.add(mapSolutionToMovie(results.nextSolution()));
            }
        }
        return movies;
    }

    private String buildTopBudgetMoviesQuery(int year) {
        return String.format("""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

            SELECT ?movie ?title ?budget ?date
            WHERE {
              ?movie a dbo:Film .
              ?movie dbo:releaseDate ?date .
              ?movie dbo:budget ?budget .
              ?movie rdfs:label ?title .
              FILTER(YEAR(?date) = %d)
              FILTER(LANG(?title) = "en")
            }
            ORDER BY DESC(?budget)
            LIMIT 10
        """, year);
    }
*/
    // =====================
    // Méthodes utilitaires
    // =====================
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
        movie.setAbstract_(truncateText(getStringValue(solution, "abstract"), 300));
        movie.setReleaseDate(getStringValue(solution, "date"));
        movie.setDirector(getStringValue(solution, "director"));
        movie.setThumbnail(getStringValue(solution, "thumbnail"));
        return movie;
    }

    private ActorGraphNode mapSolutionToActorNode(QuerySolution solution) {
        ActorGraphNode node = new ActorGraphNode();
        node.setActor(getStringValue(solution, "actor"));
        node.setOtherMovie(getStringValue(solution, "otherMovie"));
        node.setOtherTitle(getStringValue(solution, "otherTitle"));
        node.setBudget(getStringValue(solution, "budget"));
        return node;
    }

    private ProducerGraphNode mapSolutionToProducerNode(QuerySolution solution) {
        ProducerGraphNode node = new ProducerGraphNode();
        node.setProducer(getStringValue(solution, "producer"));
        node.setOtherMovie(getStringValue(solution, "otherMovie"));
        node.setOtherTitle(getStringValue(solution, "otherTitle"));
        node.setReleaseDate(getStringValue(solution, "releaseDate"));
        return node;
    }
}
