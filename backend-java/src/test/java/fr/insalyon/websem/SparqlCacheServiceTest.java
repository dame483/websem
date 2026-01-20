package fr.insalyon.websem;

import fr.insalyon.websem.service.SparqlCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SparqlCacheServiceTest {

    private SparqlCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new SparqlCacheService();
    }

    @Test
    void testCacheStorage() {
        String query = "SELECT * FROM dbpedia";
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("title", "Test Film");
        data.add(item);

        cacheService.cacheResults(query, data);
        List<Map<String, Object>> result = cacheService.getCachedResults(query);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testNoCacheHit() {
        List<Map<String, Object>> result = cacheService.getCachedResults("SELECT * FROM xyz");
        assertNull(result);
    }

    @Test
    void testClearCache() {
        String query = "SELECT * FROM dbpedia";
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("title", "Test Film");
        data.add(item);
        cacheService.cacheResults(query, data);

        assertNotNull(cacheService.getCachedResults(query));
        cacheService.clearCache();
        assertNull(cacheService.getCachedResults(query));
    }
}
