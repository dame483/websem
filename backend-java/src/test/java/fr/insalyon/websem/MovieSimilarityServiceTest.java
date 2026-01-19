package fr.insalyon.websem.service;

import fr.insalyon.websem.algorithm.CosineSimilarity;
import fr.insalyon.websem.model.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovieSimilarityServiceTest {

    private MovieSimilarityService similarityService;

    @BeforeEach
    void setup() {
        // Création d'une instance du service
        similarityService = new MovieSimilarityService();

        // On mock le service SPARQL pour renvoyer des films "candidats"
        similarityService.sparqlService = new MovieExplorationSPARQLService() {
            @Override
            public List<Movie> getMoviesByDecade(int start, int end) {
                // On simule 3 films candidats
                Movie film1 = new Movie();
                film1.setTitle("Sci-Fi Action 2024");
                film1.setReleaseDate("2024");
                film1.setSubjects(List.of("Science fiction films", "Action films"));

                Movie film2 = new Movie();
                film2.setTitle("Romantic Drama 2023");
                film2.setReleaseDate("2023");
                film2.setSubjects(List.of("Romantic films", "Drama films"));

                Movie film3 = new Movie();
                film3.setTitle("Sci-Fi 2021");
                film3.setReleaseDate("2021");
                film3.setSubjects(List.of("Science fiction films"));

                return List.of(film1, film2, film3);
            }
        };
    }

    @Test
    void testGetSimilarMovies() {

        // Film cible
        Movie target = new Movie();
        target.setTitle("Avatar");
        target.setReleaseDate("2025");
        target.setSubjects(List.of("Science fiction films", "Action films"));

        // Appel de la méthode
        List<Movie> results = similarityService.getSimilarMovies(target, 3);

        // Vérifications
        assertEquals(3, results.size());

        // Le plus similaire doit être "Sci-Fi Action 2024"
        assertEquals("Sci-Fi Action 2024", results.get(0).getTitle());

        // Ensuite "Sci-Fi 2021"
        assertEquals("Sci-Fi 2021", results.get(1).getTitle());

        // Enfin "Romantic Drama 2023"
        assertEquals("Romantic Drama 2023", results.get(2).getTitle());
    }
}
