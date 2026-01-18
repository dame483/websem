package fr.insalyon.websem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieFilterRequest {
    private String language;
    private String country;
    private String director;
    private String producer;
    private String yearFrom;
    private String yearTo;
    private String runtime;
    private String distributor;
}
