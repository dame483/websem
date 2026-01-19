package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
import fr.insalyon.websem.algorithm.CosineSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MovieSimilarityService {

    @Autowired
    protected MovieExplorationSPARQLService sparqlService;

    /**
     * Point d'entrée principal
     */
    public List<Movie> getSimilarMovies(Movie targetMovie, int limit) {
        System.out.println(" getSimilarMovies called for: " + targetMovie.getTitle());

        //  Décennie du film sélectionné
        int year;
        try {
            String dateStr = targetMovie.getReleaseDate();
            if (dateStr == null || dateStr.isEmpty()) return List.of();

            // Extraire les 4 premiers chiffres
            dateStr = dateStr.substring(0, 4);
            year = Integer.parseInt(dateStr);
        } catch (Exception e) {
            return List.of();
        }

        int startDecade = (year / 10) * 10;
        int endDecade = startDecade + 10;
        System.out.println(" Searching films from " + startDecade + " to " + endDecade);

        // Films candidats (même décennie)
        List<Movie> candidates = new ArrayList<>(
                sparqlService.getMoviesByDecade(startDecade, endDecade)
        );
        System.out.println("Found " + candidates.size() + " candidate films");

        // Retirer le film cible
        candidates.removeIf(m ->
                m.getUri() != null &&
                m.getUri().equals(targetMovie.getUri())
        );
        System.out.println(" After filtering target: " + candidates.size() + " films");

        // Vecteur du film cible
        Map<String, Integer> targetVector = buildVector(targetMovie);

        //  Calcul similarité + tri
        return candidates.stream()
                .map(movie -> Map.entry(
                        movie,
                        CosineSimilarity.compute(
                                targetVector,
                                buildVector(movie)
                        )
                ))
                .sorted(Map.Entry.<Movie, Double>
                        comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Construction du vecteur de similarité
     * Subjects + décennie
     */
    private Map<String, Integer> buildVector(Movie movie) {

        Map<String, Integer> vector = new HashMap<>();

        // Subjects (genres / catégories)
        if (movie.getSubjects() != null) {
            for (String subject : movie.getSubjects()) {
                vector.put(
                        normalize(subject),
                        1
                );
            }
        }

        // Décennie
        if (movie.getReleaseDate() != null) {
            try {
                int year = Integer.parseInt(movie.getReleaseDate());
                int decade = (year / 10) * 10;
                vector.put("decade_" + decade, 1);
            } catch (NumberFormatException ignored) {}
        }

        return vector;
    }

    /**
     * Nettoyage simple des labels
     */
    private String normalize(String value) {
        return value
                .toLowerCase()
                .replace("films", "")
                .replace("film", "")
                .trim();
    }
}
