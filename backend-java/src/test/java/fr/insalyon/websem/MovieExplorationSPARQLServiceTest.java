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
        System.out.println("Date Sortie : " + first.getReleaseDate());
        System.out.println("Description : " + first.getDescription());
        System.out.println("Réalisateur(s) : " + first.getDirector());
        System.out.println("Réalisateur(s) URI: " + first.getDirectorUri());
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

    @Test
    void testRecentMoviesByDirector() {

        // URI DBpedia de Christopher Nolan
        String directorUri = "http://dbpedia.org/resource/Christopher_Nolan";

        List<Movie> movies = service.getRecentMoviesByDirector(directorUri);

        // Vérifications de base
        assertNotNull(movies, "La liste ne doit pas être null");
        assertFalse(movies.isEmpty(), "La requête doit retourner au moins un film");
        assertTrue(movies.size() <= 5, "On doit récupérer au maximum 5 films");

        System.out.println("\nFilms récents de Christopher Nolan :");

        for (Movie m : movies) {
            assertNotNull(m.getUri(), "Chaque film doit avoir une URI");
            assertNotNull(m.getTitle(), "Chaque film doit avoir un titre");
            assertNotNull(m.getDescription(), "Chaque film doit avoir une description");
            assertNotNull(m.getReleaseDate(), "Chaque film doit avoir une année extraite");

         
            System.out.println("Titre : " + m.getTitle());
            System.out.println("URI : " + m.getUri());
            System.out.println("Année (extraite) : " + m.getReleaseDate());
            System.out.println("Description : " + m.getDescription());
        }
    }

}
