package fr.insalyon.websem.algorithm;

import fr.insalyon.websem.algorithm.CosineSimilarity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CosineSimilarityTest {

    @Test
    void identicalVectors_shouldReturnOne() {
        Map<String, Integer> v1 = Map.of(
                "science_fiction", 1,
                "action", 1,
                "decade_2020", 1
        );

        Map<String, Integer> v2 = Map.of(
                "science_fiction", 1,
                "action", 1,
                "decade_2020", 1
        );

        double similarity = CosineSimilarity.compute(v1, v2);

        assertEquals(1.0, similarity, 0.0001);
    }

    @Test
    void orthogonalVectors_shouldReturnZero() {
        Map<String, Integer> v1 = Map.of(
                "science_fiction", 1
        );

        Map<String, Integer> v2 = Map.of(
                "romantic_comedy", 1
        );

        double similarity = CosineSimilarity.compute(v1, v2);

        assertEquals(0.0, similarity, 0.0001);
    }

    @Test
    void partialOverlap_shouldReturnBetweenZeroAndOne() {
        Map<String, Integer> avatar = new HashMap<>();
        avatar.put("science_fiction", 1);
        avatar.put("action", 1);
        avatar.put("decade_2020", 1);

        Map<String, Integer> dune = new HashMap<>();
        dune.put("science_fiction", 1);
        dune.put("drama", 1);
        dune.put("decade_2020", 1);

        double similarity = CosineSimilarity.compute(avatar, dune);

        assertTrue(similarity > 0.0);
        assertTrue(similarity < 1.0);
    }

    @Test
    void emptyVector_shouldReturnZero() {
        Map<String, Integer> v1 = new HashMap<>();
        Map<String, Integer> v2 = Map.of("science_fiction", 1);

        double similarity = CosineSimilarity.compute(v1, v2);

        assertEquals(0.0, similarity, 0.0001);
    }
}
