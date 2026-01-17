package fr.insalyon.websem.service;

import fr.insalyon.websem.dto.ConversationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class ConversationService {

    @Value("${backend.python.url:http://backend-python:8000}")
    private String pythonBackendUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DBPEDIA_ENDPOINT = "https://dbpedia.org/sparql";

    public ConversationResponse processQuestion(String question) {
        ConversationResponse response = new ConversationResponse();
        response.setQuestion(question);

        try {
            String sparqlQuery = callPythonBackend(question);
            response.setSparqlQuery(sparqlQuery);

            try {
                List<Map<String, String>> results = executeSparqlQuery(sparqlQuery);
                
                if (results.isEmpty()) {
                    String aiAnswer = generateAIAnswer(question);
                    response.setResults(new ArrayList<>());
                    response.setAiAnswer(aiAnswer);
                } else {
                    response.setResults(results);
                }
            } catch (Exception sparqlException) {
                System.err.println("Requête SPARQL invalide, utilisation du fallback IA: " + sparqlException.getMessage());
                String aiAnswer = generateAIAnswer(question);
                response.setResults(new ArrayList<>()); 
                response.setAiAnswer(aiAnswer);
            }
        } catch (Exception e) {
            response.setError("Erreur lors du traitement : " + e.getMessage());
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    private String callPythonBackend(String question) throws Exception {
        String url = pythonBackendUrl + "/api/sparql/";

        String payload = "{\"sentence\": \"" + escapeJson(question) + "\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            if (rootNode.has("sparql")) {
                String sparqlBody = rootNode.get("sparql").asText();
                String completeSparql = buildCompleteSparqlQuery(sparqlBody);
                return completeSparql;
            } else {
                throw new Exception("Format de réponse inattendu du backend Python");
            }
        } else {
            throw new Exception("Le backend Python a retourné une erreur : " + response.getStatusCode());
        }
    }

    private String buildCompleteSparqlQuery(String whereClause) {
        String cleanedWhereClause = whereClause.trim();
        if (cleanedWhereClause.isEmpty()) {
            throw new IllegalArgumentException("Le corps SPARQL reçu du LLM est vide");
        }
        
        Set<String> variables = extractVariables(cleanedWhereClause);
        
        StringBuilder selectClause = new StringBuilder("SELECT ");
        
        if (!variables.isEmpty()) {
            for (String var : variables) {
                selectClause.append("?").append(var).append(" ");
            }
        } else {
            selectClause.append("?result ");
        }
        
        String prefixes = "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                         "PREFIX dbr: <http://dbpedia.org/resource/>\n" +
                         "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                         "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n\n";
        
        String completeSparql = prefixes + selectClause.toString() + "\nWHERE {\n" + 
                               cleanedWhereClause + "\n}\nLIMIT 200";
        
        System.out.println("Requête SPARQL construite:\n" + completeSparql);
        
        return completeSparql;
    }

    private Set<String> extractVariables(String whereClause) {
        Set<String> variables = new LinkedHashSet<>();
        
        String[] tokens = whereClause.split("\\s+");
        
        for (String token : tokens) {
            String cleaned = token.replaceAll("[^a-zA-Z0-9_?]", "");
            
            if (cleaned.startsWith("?") && cleaned.length() > 1) {
                String varName = cleaned.substring(1); // Enlever le ?
                if (!varName.isEmpty() && Character.isLetter(varName.charAt(0))) {
                    variables.add(varName);
                }
            }
        }
        
        return variables;
    }

private List<Map<String, String>> executeSparqlQuery(String sparqlQuery) throws Exception {
    List<Map<String, String>> results = new ArrayList<>();

    try {
        String url = DBPEDIA_ENDPOINT;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/sparql-results+json");

        String body = "query=" + java.net.URLEncoder.encode(sparqlQuery, "UTF-8") + "&format=json";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            
            if (!rootNode.has("results") || !rootNode.get("results").has("bindings")) {
                return results;
            }

            JsonNode bindings = rootNode.get("results").get("bindings");
            Set<String> seenUris = new HashSet<>();
            
            for (JsonNode binding : bindings) {
                try {
                    Map<String, String> row = new HashMap<>();
                    String filmUri = null;

                    Iterator<Map.Entry<String, JsonNode>> fields = binding.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        String varName = entry.getKey();
                        JsonNode value = entry.getValue();
                        
                        if (value.has("value")) {
                            String stringValue = value.get("value").asText();
                            
                            if (value.get("type").asText().equals("uri")) {
                                filmUri = stringValue;
                            }
                            
                            String displayValue = stringValue;
                            if (displayValue.contains("/resource/")) {
                                displayValue = displayValue.substring(displayValue.lastIndexOf("/") + 1)
                                        .replace("_", " ");
                            }
                            row.put(varName, displayValue);
                        }
                    }

                    if (filmUri != null && !seenUris.contains(filmUri) && !row.isEmpty()) {
                        seenUris.add(filmUri);
                        results.add(row);
                        
                        if (results.size() >= 20) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
    } catch (Exception e) {
        System.err.println("Erreur SPARQL : " + e.getMessage());
        throw new Exception("Erreur lors de l'exécution de la requête SPARQL : " + e.getMessage());
    }

    return results;
}

    private String generateAIAnswer(String question) throws Exception {
        String url = pythonBackendUrl + "/api/sparql/answer";
        
        Map<String, String> request = new HashMap<>();
        request.put("sentence", question);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            if (rootNode.has("answer")) {
                return rootNode.get("answer").asText();
            }
        }
        return "Je n'ai pas pu générer une réponse à votre question.";
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
