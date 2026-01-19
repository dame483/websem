package fr.insalyon.websem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    private String uri;
    private String title;
    private String description;
    private String releaseDate;
    private String director;
    private String directorUri;
    private String thumbnail;
    private String country;
    private String language;
    private String producer;
    private String editor;
    private String studio;
    private String musicComposer;
    private String runtime;
    private String distributor;
    private String gross;
    private String budget;

    // Nouvel attribut pour l'étude de similarité
    private List<String> subjects;

}
