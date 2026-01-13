package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilmExplorationSPARQLServiceTest {

    private FilmExplorationSPARQLService service;

    @BeforeEach
    void setUp() {
        service = new FilmExplorationSPARQLService();
    }

    @Test
    void testConnexionDBPedia() {
        // On exécute une requête minimale pour voir si DBpedia répond
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
        System.out.println("Résumé : " + first.getAbstract_());
        System.out.println("Réalisateur : " + first.getDirector());
        
    }
}
