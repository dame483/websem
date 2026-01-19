package fr.insalyon.websem.algorithm;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CosineSimilarity {

    /**
     * Calcule la similarité cosinus entre deux vecteurs clairsemés
     */
    public static double compute(Map<String, Integer> v1,
                                 Map<String, Integer> v2) {

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(v1.keySet());
        allKeys.addAll(v2.keySet());

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String key : allKeys) {
            int a = v1.getOrDefault(key, 0);
            int b = v2.getOrDefault(key, 0);

            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
