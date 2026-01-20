package fr.insalyon.websem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SparqlTranslationResponse {
    private String question;
    private String sparqlQuery;
    private String error;
}
