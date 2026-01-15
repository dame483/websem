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
        List<Movie> movies = service.searchMovies("Inception");
        assertNotNull(movies, "La liste ne doit pas être null");
        assertFalse(movies.isEmpty(), "La recherche doit retourner au moins un film");
    }

    @Test
    void testRequeteSPARQLComplete() {
        String filmName = "Inception";
        List<Movie> movies = service.searchMovies(filmName);

        assertNotNull(movies, "La liste ne doit pas être null");
        assertFalse(movies.isEmpty(), "La recherche doit retourner au moins un film");

        // Vérification du premier film
        Movie first = movies.get(0);

        // Champs obligatoires
        assertNotNull(first.getTitle(), "Le titre du film doit être présent");
        assertNotNull(first.getUri(), "L'URI du film doit être présente");

        // Champs optionnels 
        System.out.println("Titre : " + first.getTitle());
        System.out.println("Description : " + first.getDescription());
        System.out.println("Réalisateur(s) : " + first.getDirector());
        System.out.println("Producteur(s) : " + first.getProducer());
        System.out.println("Éditeur(s) : " + first.getEditor());
        System.out.println("Studio(s) : " + first.getStudio());
        System.out.println("Compositeur : " + first.getMusicComposer());
        System.out.println("Durée : " + first.getRuntime());
        System.out.println("Distributeur : " + first.getDistributor());
        System.out.println("Pays : " + first.getCountry());
        System.out.println("Langue : " + first.getLanguage());
        System.out.println("Budget : " + first.getBudget());
        System.out.println("Gross : " + first.getGross());
        System.out.println("Thumbnail : " + first.getThumbnail());

    }
}
