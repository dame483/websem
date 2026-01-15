package fr.insalyon.websem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    private String uri;
    private String title;
    private String description;
    private String releaseDate;
    private String director;
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

}
