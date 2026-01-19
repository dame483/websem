package fr.insalyon.websem.controller;

import fr.insalyon.websem.dto.ConversationRequest;
import fr.insalyon.websem.dto.ConversationResponse;
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

        ConversationResponse response = conversationService.processQuestion(request.getQuestion());
        return ResponseEntity.ok(response);
    }
}
