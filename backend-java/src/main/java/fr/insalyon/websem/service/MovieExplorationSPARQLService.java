package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
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

        SELECT ?movie ?title ?description ?thumbnail ?runtime ?gross ?budget
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
          ?movie rdfs:label ?title .
          FILTER(LANG(?title) = "en")
          FILTER(REGEX(?title, "%s", "i"))   

          OPTIONAL { ?movie dbo:description ?description . FILTER(LANG(?description) = "en") }
          OPTIONAL { ?movie dbo:director ?directorRes . ?directorRes rdfs:label ?director . FILTER(LANG(?director) = "en") }
          OPTIONAL { ?movie dbo:producer ?producerRes . ?producerRes rdfs:label ?producer . FILTER(LANG(?producer) = "en") }
          OPTIONAL { ?movie dbo:editing ?editorRes . ?editorRes rdfs:label ?editor . FILTER(LANG(?editor) = "en") }
          OPTIONAL { ?movie dbo:studio ?studio . }
          OPTIONAL { ?movie dbo:musicComposer ?musicComposerRes . ?musicComposerRes rdfs:label ?musicComposer . FILTER(LANG(?musicComposer) = "en") }
          OPTIONAL { ?movie dbo:distributor ?distributor . }
          OPTIONAL { ?movie dbp:country ?country . }
          OPTIONAL { ?movie dbp:language ?language . }
          OPTIONAL { ?movie dbo:runtime ?runtime . }
          OPTIONAL { ?movie dbo:gross ?gross . FILTER(DATATYPE(?gross) = <http://dbpedia.org/datatype/usDollar>) }
          OPTIONAL { ?movie dbo:budget ?budget . FILTER(DATATYPE(?budget) = <http://dbpedia.org/datatype/usDollar>) }
          OPTIONAL { ?movie dbo:thumbnail ?thumbnail }
        }
        GROUP BY ?movie ?title ?description ?thumbnail ?runtime ?gross ?budget
        LIMIT 20
    """, escapeString(movieName));
}


    // Graphe des acteurs
    
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

            SELECT ?movie ?title (MAX(?year) AS ?year) (SAMPLE(?description) AS ?description)
            WHERE {
            ?movie dbo:director <%s> .
            ?movie a dbo:Film .
            ?movie rdfs:label ?title .
            ?movie dbo:description ?description .

            FILTER(REGEX(?description, "^[0-9]{4}"))

            BIND(xsd:integer(REPLACE(STR(?description), "^([0-9]{4}).*", "$1")) AS ?year)
            }
            GROUP BY ?movie ?title
            ORDER BY DESC(?year)
            LIMIT 5
        """, directorUri);
    }

    
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

    // TODO : ajouter des methodes de nettoyages pour les autres attributs, gross, budget... et quand il y 
    // a une liste de valeurs avec virgule (soit changer la nature de l'attribut de Movie et consrtuire une methode qui récupère les infos entre virgule)
   private Movie mapSolutionToMovie(QuerySolution solution) {
        Movie movie = new Movie();
        movie.setUri(getStringValue(solution, "movie"));
        movie.setTitle(getStringValue(solution, "title"));
        movie.setDescription(truncateText(getStringValue(solution, "description"), 300));
        //movie.setReleaseDate(getStringValue(solution, "year")); // année extraite
        movie.setDirector(getStringValue(solution, "directors"));
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


}
