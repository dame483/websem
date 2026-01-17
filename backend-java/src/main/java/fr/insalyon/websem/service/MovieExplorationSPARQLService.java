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

    // Recherche de films avec filtres avancés
    public List<Movie> searchMoviesWithFilters(String title, String language, String country, 
                                               String director, String producer, String yearFrom, 
                                               String yearTo, String distributor) {
        String sparqlQuery = buildAdvancedSearchQuery(title, language, country, director, producer, 
                                                      yearFrom, yearTo, distributor);
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
          ?movie a dbo:Film .
          ?movie rdfs:label ?titleLabel .
          FILTER(LANG(?titleLabel) = "en")
          FILTER(REGEX(?titleLabel, "%s", "i"))   

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
            // Nettoyer les suffixes de type RDF (^^http://...)
            if (value.contains("^^http")) {
                value = value.substring(0, value.indexOf("^^http"));
            }
            // Nettoyer les suffixes de langue (@en, @fr, etc.)
            if (value.contains("@")) {
                value = value.substring(0, value.lastIndexOf("@"));
            }
            
            // Nettoyer les URIs enchâssées (ex: "Bengali, http://dbpedia.org/resource/Bengali_language")
            // Supprimer tout ce qui ressemble à une URI DBpedia après la virgule et l'espace
            value = value.replaceAll(", http://dbpedia\\.org/resource/[^\\s,]*", "");
            
            // Nettoyer les URIs pour récupérer juste le nom final (au cas où il en reste)
            if (value.startsWith("http://dbpedia.org/resource/")) {
                value = value.replace("http://dbpedia.org/resource/", "").replace("_", " ");
            }
            
            // Nettoyer les espaces en double et les virgules mal placées
            value = value.replaceAll(",\\s*,", ",")  // Supprimer les virgules doubles
                    .replaceAll("^[,\\s]+|[,\\s]+$", "")  // Supprimer les virgules/espaces au début/fin
                    .replaceAll("\\s+", " ");  // Normaliser les espaces
            
            return value.trim();
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
        movie.setReleaseDate(getStringValue(solution, "year")); // année extraite
        movie.setDirector(getStringValue(solution, "directors"));
        movie.setDirectorUri(getStringValue(solution, "directorUris"));
        movie.setProducer(getStringValue(solution, "producers"));
        movie.setEditor(getStringValue(solution, "editors"));
        movie.setStudio(getStringValue(solution, "studios"));
        movie.setMusicComposer(getStringValue(solution, "musicComposers"));
        movie.setRuntime(formatRuntime(getStringValue(solution, "runtime")));
        movie.setDistributor(getStringValue(solution, "distributors"));
        movie.setCountry(getStringValue(solution, "countries"));
        movie.setLanguage(getStringValue(solution, "languages"));
        movie.setGross(formatCurrency(getStringValue(solution, "gross")));
        movie.setBudget(formatCurrency(getStringValue(solution, "budget")));
        movie.setThumbnail(getStringValue(solution, "thumbnail"));
        return movie;
    }

    private String formatRuntime(String runtime) {
        if (runtime == null) return null;
        try {
            // runtime est en secondes (double)
            double seconds = Double.parseDouble(runtime);
            int minutes = (int) (seconds / 60);
            return minutes + " min";
        } catch (Exception e) {
            return runtime;
        }
    }

    private String formatCurrency(String amount) {
        if (amount == null) return null;
        try {
            double value = Double.parseDouble(amount);
            if (value >= 1_000_000_000) {
                return String.format("$%.2f B", value / 1_000_000_000);
            } else if (value >= 1_000_000) {
                return String.format("$%.2f M", value / 1_000_000);
            } else if (value >= 1_000) {
                return String.format("$%.2f K", value / 1_000);
            } else {
                return String.format("$%.2f", value);
            }
        } catch (Exception e) {
            return amount;
        }
    }


}
