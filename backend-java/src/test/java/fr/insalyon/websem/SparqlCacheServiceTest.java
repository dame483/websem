package fr.insalyon.websem;

import fr.insalyon.websem.service.SparqlCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests du service de cache SPARQL")
class SparqlCacheServiceTest {

    private SparqlCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new SparqlCacheService();
    }

    @Test
    @DisplayName("Doit stocker et récupérer des résultats en cache")
    void testCacheStorage() {
        String query = "SELECT * FROM dbpedia";
        List<Map<String, Object>> testData = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("title", "Test Film");
        testData.add(entry);

        cacheService.cacheResults(query, testData);
        List<Map<String, Object>> retrieved = cacheService.getCachedResults(query);

        assertNotNull(retrieved, "Les résultats en cache ne doivent pas être null");
        assertEquals(1, retrieved.size(), "Le cache doit contenir 1 entrée");
    }

    @Test
    @DisplayName("Doit retourner null si pas de cache")
    void testNoCacheHit() {
        String unknownQuery = "SELECT * FROM unknown_database_xyz";
        List<Map<String, Object>> result = cacheService.getCachedResults(unknownQuery);
        assertNull(result, "Doit retourner null si pas de cache");
    }

    @Test
    @DisplayName("Doit vider le cache correctement")
    void testClearCache() {
        // Stocker une requête
        String query = "SELECT * FROM dbpedia";
        List<Map<String, Object>> testData = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("title", "Film Cache");
        testData.add(entry);
        cacheService.cacheResults(query, testData);

        // Vérifier que le cache a quelque chose
        List<Map<String, Object>> beforeClear = cacheService.getCachedResults(query);
        assertNotNull(beforeClear, "Le cache doit avoir des données avant la suppression");

        // Vider le cache
        cacheService.clearCache();

        // Vérifier que le cache est vide
        List<Map<String, Object>> afterClear = cacheService.getCachedResults(query);
        assertNull(afterClear, "Le cache doit être vide après clearCache()");
    }
}
