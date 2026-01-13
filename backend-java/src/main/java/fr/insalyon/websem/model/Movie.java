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
    private String abstract_;
    private String releaseDate;
    private String director;
    private String thumbnail;
}
