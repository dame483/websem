package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovieExplorationSPARQLServiceTest {

    private MovieExplorationSPARQLService service;

    @BeforeEach
    void setUp() {
        service = new MovieExplorationSPARQLService();
    }

    @Test
    void testConnexionDBPedia() {
        List<Movie> movies = service.searchMovies("Matrix");
        assertNotNull(movies, "La liste ne doit pas être null");
    }

    @Test
    void testRequeteSPARQL() {
        String filmName = "Inception";
        List<Movie> movies = service.searchMovies(filmName);

        assertNotNull(movies, "La liste ne doit pas être null");
        assertFalse(movies.isEmpty(), "La recherche doit retourner au moins un film");

        // Vérification des champs du premier film
        Movie first = movies.get(0);
        assertNotNull(first.getTitle(), "Le titre du film doit être présent");
        assertNotNull(first.getUri(), "L'URI du film doit être présente");

        // Champs optionnels
        System.out.println("Description : " + first.getDescription());
        System.out.println("Réalisateur : " + first.getDirector());
        
    }
}
