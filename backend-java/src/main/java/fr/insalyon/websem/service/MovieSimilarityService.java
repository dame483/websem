package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
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
        System.out.println("getSimilarMovies called for: " + targetMovie.getTitle());

        // Vérifier la date
        String dateStr = targetMovie.getReleaseDate();
        if (dateStr == null || dateStr.isEmpty() || targetMovie.getSubjects() == null) {
            return List.of();
        }

        int year;
        try {
            dateStr = dateStr.substring(0, 4);
            year = Integer.parseInt(dateStr);
        } catch (Exception e) {
            return List.of();
        }

        int startDecade = (year / 10) * 10;
        int endDecade = startDecade + 10;
        System.out.println("Searching films from " + startDecade + " to " + endDecade);

        // Films candidats
        List<Movie> candidates = new ArrayList<>(sparqlService.getMoviesByDecade(startDecade, endDecade));
        System.out.println("Found " + candidates.size() + " candidate films");

        // Retirer le film cible
        candidates.removeIf(m -> m.getUri() != null && m.getUri().equals(targetMovie.getUri()));
        System.out.println("After filtering target: " + candidates.size() + " films");

        // Normaliser les subjects du film cible
        List<String> targetSubjects = new ArrayList<>();
        for (String s : targetMovie.getSubjects()) {
            targetSubjects.add(normalize(s));
        }

        // Calcul de la distance globale pour chaque film candidat
        List<Map.Entry<Movie, Double>> scoredMovies = new ArrayList<>();
        for (Movie candidate : candidates) {
            double distance = computeDistance(targetSubjects, candidate.getSubjects());
            scoredMovies.add(Map.entry(candidate, distance));
        }

        // Tri par distance croissante (distance faible = film plus similaire)
        scoredMovies.sort(Map.Entry.comparingByValue());

        // Retourner les films triés par similarité
        List<Movie> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scoredMovies.size()); i++) {
            result.add(scoredMovies.get(i).getKey());
        }

        return result;
    }

    /**
     * Calcule la distance du film candidat par rapport au film cible
     * Utilise la similarité des subjects
     */
    private double computeDistance(List<String> targetSubjects, List<String> candidateSubjects) {
        if (candidateSubjects == null || candidateSubjects.isEmpty()) {
            return Double.MAX_VALUE; // Film sans subject = très éloigné
        }

        double sumSquares = 0.0;

        for (String target : targetSubjects) {
            double bestSim = 0.0;
            for (String candidate : candidateSubjects) {
                double sim = subjectSimilarity(target, normalize(candidate));
                if (sim > bestSim) bestSim = sim;
            }
            // Distance = 1 - similarité maximale
            double coordValue = 1.0 - bestSim;
            sumSquares += coordValue * coordValue;
        }

        return Math.sqrt(sumSquares); // Norme Euclidienne
    }

    /**
     * Similarité entre deux subjects (0 = pas similaire, 1 = identique)
     * Ici on fait simple : égalité exacte = 1, sinon on peut utiliser trigrammes ou Cosine sur texte
     */
    private double subjectSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;

        // Variante simple : fraction de mots en commun
        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : ((double) intersection.size()) / union.size();
    }

    /**
     * Normalisation simple
     */
    private String normalize(String value) {
        return value
                .toLowerCase()
                .replace("films", "")
                .replace("film", "")
                .trim();
    }
}
