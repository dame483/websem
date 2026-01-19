package fr.insalyon.websem.controller;

import fr.insalyon.websem.dto.MovieFilterRequest;
import fr.insalyon.websem.dto.MovieFilterRequest;
import fr.insalyon.websem.model.Actor;
import fr.insalyon.websem.model.Genre;
import fr.insalyon.websem.model.Movie;
import fr.insalyon.websem.service.MovieExplorationSPARQLService;
import fr.insalyon.websem.service.SparqlCacheService;
import fr.insalyon.websem.service.MovieSimilarityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Autowired
    private MovieExplorationSPARQLService MovieExplorationSPARQLService;
    
    @Autowired
    private SparqlCacheService cacheService;

    @Autowired
    private MovieSimilarityService MovieSimilarityService;

   

    @GetMapping("/search")
    public ResponseEntity<List<Movie>> searchMovies(@RequestParam String query) {

         System.out.println(">>> searchMovies called with query = " + query);

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Movie> movies = MovieExplorationSPARQLService.searchMovies(query);

        System.out.println(" Found " + movies.size() + " movies");
        
        return ResponseEntity.ok(movies);
    }

    @PostMapping("/search-advanced")
    public ResponseEntity<List<Movie>> searchMoviesAdvanced(@RequestBody MovieFilterRequest filters) {
        List<Movie> movies = MovieExplorationSPARQLService.searchMoviesWithFilters(
            filters.getTitle(),
            filters.getLanguage(),
            filters.getCountry(),
            filters.getDirector(),
            filters.getProducer(),
            filters.getYearFrom(),
            filters.getYearTo(),
            filters.getDistributor()
        );
        return ResponseEntity.ok(movies);
    }
    
    @GetMapping("/cache/info")
    public ResponseEntity<Map<String, Object>> getCacheInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("cacheSize", cacheService.getCacheSizeInMB());
        info.put("message", "Taille du cache SPARQL");
        return ResponseEntity.ok(info);
    }
    
    @DeleteMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        cacheService.clearCache();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache nettoyé avec succès");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent-by-director")
    public List<Movie> getRecentMoviesByDirector(
            @RequestParam String directorUri,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return MovieExplorationSPARQLService.getRecentMoviesByDirector(directorUri)
                           .stream()
                           .limit(limit)
                           .toList();
    }

    @GetMapping("/top-actors-by-movie")
    public List<Actor> getTopActorsByMovie(@RequestParam String movieUri) {
        return MovieExplorationSPARQLService.getTopActorsByMovie(movieUri);
    }

    @GetMapping("/distribution-by-year")
    public List<Genre> getGenreDistributionByYear(@RequestParam String year) {
        return MovieExplorationSPARQLService.getAllNormalizedGenresByYear(year);
    }

    @GetMapping("/top-budget-by-year")
    public List<Movie> getTopBudgetByYear(@RequestParam String year) {
        return MovieExplorationSPARQLService.getTopBudgetMoviesByYear(year);
    }

    @GetMapping("/similar")
    public ResponseEntity<List<Movie>> getSimilarMovies(@RequestParam String uri, @RequestParam(defaultValue = "20") int limit
    ) {
        if (uri == null || uri.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        System.out.println("Received URI: " + uri);

        // Récupérer le film sélectionné via son URI
        Movie targetMovie = MovieExplorationSPARQLService.getMovieByUri(uri);

        if (targetMovie == null) {
            System.out.println(" No movie found for URI: " + uri);
            return ResponseEntity.notFound().build();
        }

        System.out.println(" Found target movie: " + targetMovie.getTitle() + " - Release Date: " + targetMovie.getReleaseDate());

        // Appel du service de similarité
        List<Movie> similarMovies = MovieSimilarityService.getSimilarMovies(targetMovie, limit);

        System.out.println(" Found " + similarMovies.size() + " similar movies");
        return ResponseEntity.ok(similarMovies);
    }

}
