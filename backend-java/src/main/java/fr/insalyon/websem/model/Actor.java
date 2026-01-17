package fr.insalyon.websem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Actor {
    private String actorUri;
    private String actorName;
    private String topMovieUri;
    private String topMovieTitle;
    private Double maxGross;
}