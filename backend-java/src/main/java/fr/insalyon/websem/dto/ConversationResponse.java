package fr.insalyon.websem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private String question;
    private String sparqlQuery;
    private List<Map<String, String>> results;
    private String error;
    private String aiAnswer;
}
