package fr.insalyon.websem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Genre {
    private String name; // le genre normalisé (premier mot)
    private int count;   // nombre de films ayant ce genre (pour dbp ou dbo)
    private List<String> rawGenres; // optionnel : liste de labels complets associés au premier mot
}