package fr.insalyon.websem.controller;

import fr.insalyon.websem.dto.MovieFilterRequest;
import fr.insalyon.websem.dto.MovieFilterRequest;
import fr.insalyon.websem.model.Actor;
import fr.insalyon.websem.model.Genre;
import fr.insalyon.websem.model.Movie;
import fr.insalyon.websem.service.MovieExplorationSPARQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Autowired
    private MovieExplorationSPARQLService MovieExplorationSPARQLService;

    @GetMapping("/search")
    public ResponseEntity<List<Movie>> searchMovies(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Movie> movies = MovieExplorationSPARQLService.searchMovies(query);
        return ResponseEntity.ok(movies);
    }

    @PostMapping("/search-advanced")
    public ResponseEntity<List<Movie>> searchMoviesAdvanced(@RequestParam String title, @RequestBody MovieFilterRequest filters) {
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Movie> movies = MovieExplorationSPARQLService.searchMoviesWithFilters(
            title,
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
}
