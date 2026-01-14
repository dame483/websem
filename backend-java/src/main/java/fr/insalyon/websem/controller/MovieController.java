package fr.insalyon.websem.controller;

import fr.insalyon.websem.model.Movie;
import fr.insalyon.websem.service.DBpediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "http://localhost:3000")
public class MovieController {

    @Autowired
    private DBpediaService dbpediaService;

    @GetMapping("/search")
    public ResponseEntity<List<Movie>> searchMovies(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Movie> movies = dbpediaService.searchMovies(query);
        return ResponseEntity.ok(movies);
    }
}
