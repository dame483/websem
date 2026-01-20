package fr.insalyon.websem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.jena.query.ResultSet;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SparqlCacheService {
    
    private static final String CACHE_DIR = "sparql-cache";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final Cache<String, List<Map<String, Object>>> memoryCache = Caffeine.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .maximumSize(1000)
        .build();
    
    public SparqlCacheService() {
        // Créer le répertoire de cache s'il n'existe pas
        try {
            Files.createDirectories(Paths.get(CACHE_DIR));
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du répertoire de cache: " + e.getMessage());
        }
    }
    
    /**
     * Génère un hash SHA-256 de la requête SPARQL
     */
    private String generateQueryHash(String sparqlQuery) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sparqlQuery.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(sparqlQuery.hashCode());
        }
    }
    
    /**
     * Récupère le chemin du fichier de cache pour une requête
     */
    private Path getCacheFilePath(String queryHash) {
        return Paths.get(CACHE_DIR, queryHash + ".json");
    }
    
    /**
     * Récupère les résultats du cache si disponibles
     */
    public List<Map<String, Object>> getCachedResults(String sparqlQuery) {
        String queryHash = generateQueryHash(sparqlQuery);
        
        List<Map<String, Object>> cachedResults = memoryCache.getIfPresent(queryHash);
        if (cachedResults != null) {
            return cachedResults;
        }
        
        Path cacheFile = getCacheFilePath(queryHash);
        
        if (Files.exists(cacheFile)) {
            try {
                String cachedContent = new String(Files.readAllBytes(cacheFile));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = objectMapper.readValue(
                    cachedContent,
                    List.class
                );
                memoryCache.put(queryHash, results);
                return results;
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture du cache: " + e.getMessage());
            }
        }
        
        return null; // Pas de cache trouvé
    }
    
    /**
     * Stocke les résultats en cache
     */
    public void cacheResults(String sparqlQuery, List<Map<String, Object>> results) {
        String queryHash = generateQueryHash(sparqlQuery);
        memoryCache.put(queryHash, results);
        
        Path cacheFile = getCacheFilePath(queryHash);
        
        try {
            String jsonContent = objectMapper.writeValueAsString(results);
            Files.write(cacheFile, jsonContent.getBytes());
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du cache: " + e.getMessage());
        }
    }
    
    /**
     * Vide tout le cache
     */
    public void clearCache() {
        memoryCache.invalidateAll();
        try {
            Files.walk(Paths.get(CACHE_DIR))
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        System.err.println("Erreur lors de la suppression: " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("Erreur lors du nettoyage du cache: " + e.getMessage());
        }
    }
    
    /**
     * Obtient la taille du cache en MB
     */
    public double getCacheSizeInMB() {
        try {
            long totalSize = Files.walk(Paths.get(CACHE_DIR))
                .filter(Files::isRegularFile)
                .mapToLong(file -> {
                    try {
                        return Files.size(file);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
            return totalSize / (1024.0 * 1024.0);
        } catch (IOException e) {
            return 0;
        }
    }
}
