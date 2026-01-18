package fr.insalyon.websem.service;

import fr.insalyon.websem.model.Movie;
import fr.insalyon.websem.model.Genre;
import fr.insalyon.websem.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.stream.IntStream;


import java.util.*;

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

   
    @Test
    void testTopActorsByMovie_Inception() {

        String inceptionUri = "http://dbpedia.org/resource/Inception";

        List<Actor> actors = service.getTopActorsByMovie(inceptionUri);

        assertNotNull(actors, "La liste des acteurs ne doit pas être null");
        assertFalse(actors.isEmpty(), "La liste des acteurs ne doit pas être vide");

        System.out.println("\nActeurs stars de Inception et leur film le plus rentable :");

        actors.forEach(a -> {
            System.out.println("Acteur : " + a.getActorName());
            System.out.println("Top film : " + a.getTopMovieTitle());
            System.out.println("Gross max : " + a.getMaxGross());
            System.out.println("-----------");
        });

        // Vérifier qu'on trouve Leonardo DiCaprio
        Optional<Actor> leoOpt = actors.stream()
                .filter(a -> a.getActorName().toLowerCase().contains("leonardo"))
                .findFirst();

        assertTrue(leoOpt.isPresent(), "Leonardo DiCaprio doit être présent dans les acteurs");

        Actor leo = leoOpt.get();

        assertNotNull(leo.getTopMovieTitle(), "Leonardo doit avoir un top film");
        assertNotNull(leo.getTopMovieUri(), "Leonardo doit avoir une URI de top film");
        assertTrue(leo.getMaxGross() > 0, "Leonardo doit avoir un gross > 0");

        System.out.println("\nVérification Leonardo DiCaprio :");
        System.out.println("Top film : " + leo.getTopMovieTitle());
        System.out.println("Gross : " + leo.getMaxGross());

        assertTrue(
                leo.getTopMovieTitle().toLowerCase().contains("titanic") 
                || leo.getTopMovieTitle().toLowerCase().contains("avatar") 
                || leo.getMaxGross() > 1_000_000_000,
                "Le film le plus rentable de Leonardo devrait être Titanic (ou équivalent très gros succès)"
        );
    }



    @Test
    void testFetchNormalizedGenres() {

        Map<String, Genre> genres = service.fetchNormalizedGenres();

        assertNotNull(genres, "La map de genres ne doit pas être null");
        assertFalse(genres.isEmpty(), "La map de genres ne doit pas être vide");

        Genre oneGenre = genres.values().iterator().next();

        assertNotNull(oneGenre.getName(), "Le nom du genre ne doit pas être null");
        assertNotNull(oneGenre.getRawGenres(), "La liste rawGenres ne doit pas être null");

        System.out.println("Nombre de genres normalisés : " + genres.size());

        // Afficher 10 genres pour voir la tête du résultat
        genres.values().stream()
                .limit(10)
                .forEach(g -> System.out.println(
                        "Genre normalisé : " + g.getName() +
                        " | exemples : " + g.getRawGenres()
                ));
    }


    @Test
    void testGetAllNormalizedGenresByYear_1989() {

        List<Genre> genres = service.getAllNormalizedGenresByYear("1989");

        assertNotNull(genres, "La liste de genres ne doit pas être null");
        assertFalse(genres.isEmpty(), "La liste de genres ne doit pas être vide");

        assertFalse(genres.isEmpty(), "Il doit y avoir au moins un genre pour 1989");

        System.out.println("\nGenres normalisés pour l'année 1989 :");

        genres.stream()
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .forEach(g -> System.out.println(
                        g.getName() + " | " + g.getCount() + " | " + g.getRawGenres()
                ));

        for (Genre g : genres) {
            assertNotNull(g.getName(), "Le nom du genre ne doit pas être null");
            assertTrue(g.getCount() > 0, "Le nombre de films doit être > 0");
            assertNotNull(g.getRawGenres(), "rawGenres ne doit pas être null");
        }
    }

    @Test
    void testCountMoviesByYear_1989() {
        String year = "1989";

        int totalMovies = service.countMoviesByYear(year);

        assertTrue(totalMovies >= 0, "Le nombre de films ne doit pas être négatif");
        System.out.println("Nombre total de films sortis en " + year + " : " + totalMovies);

        assertTrue(totalMovies > 0, "Il doit y avoir au moins un film en 1989");
    }

    
    @Test
    void testTopBudgetMoviesByYear_2010() {
        String year = "2010";

        List<Movie> topMovies = service.getTopBudgetMoviesByYear(year);

        assertNotNull(topMovies, "La liste des films ne doit pas être null");
        assertFalse(topMovies.isEmpty(), "La liste des films ne doit pas être vide");
        assertTrue(topMovies.size() <= 10, "On doit récupérer au maximum 10 films");

        System.out.println("\nTop films par budget pour l'année " + year + " :");

        topMovies.forEach(m -> {
            assertNotNull(m.getTitle(), "Chaque film doit avoir un titre");
            assertNotNull(m.getUri(), "Chaque film doit avoir une URI");
            assertNotNull(m.getBudget(), "Chaque film doit avoir un budget");
            System.out.println("Titre : " + m.getTitle());
            System.out.println("URI : " + m.getUri());
            System.out.println("Budget : " + m.getBudget());
            System.out.println("-----------");
        });

    }


}
