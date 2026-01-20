package fr.insalyon.websem.controller;

import fr.insalyon.websem.dto.ConversationRequest;
import fr.insalyon.websem.dto.ConversationResponse;
import fr.insalyon.websem.dto.SparqlTranslationResponse;
import fr.insalyon.websem.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @PostMapping("/ask")
    public ResponseEntity<ConversationResponse> askQuestion(@RequestBody ConversationRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ConversationResponse response = conversationService.generateAIAnswer(request.getQuestion());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/query-dbpedia")
    public ResponseEntity<ConversationResponse> queryDBpedia(@RequestBody ConversationRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ConversationResponse response = conversationService.queryDBpedia(request.getQuestion());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/translate-to-sparql")
    public ResponseEntity<SparqlTranslationResponse> translateToSparql(@RequestBody ConversationRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SparqlTranslationResponse response = conversationService.translateToSparql(request.getQuestion());
        return ResponseEntity.ok(response);
    }
}
