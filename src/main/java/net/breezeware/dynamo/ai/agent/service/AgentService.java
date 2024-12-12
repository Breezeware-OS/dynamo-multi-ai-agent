package net.breezeware.dynamo.ai.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class to interact with the AI-powered chat client for processing user inputs and generating responses.
 */
@Service
@Slf4j
public class AgentService {
    private final ChatClient chatClient;

    /**
     * Constructor for initializing the AgentService with a configured ChatClient.
     *
     * @param chatClientBuilder Builder for creating a ChatClient with predefined configurations.
     */
    public AgentService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are an AI assistant providing weather information.")
                .defaultFunctions("currentWeatherFunction")
                .build();
    }

    /**
     * Executes a user query by passing it to the chat client and returning the response content.
     *
     * @param input    The user input or query to process.
     * @param messages A list of {@link ToolResponseMessage} providing context or tools for the AI to use.
     * @return The response content as a string.
     */
    public String execute(String input, List<ToolResponseMessage> messages) {
        try {
            // Validate input
            if (input == null || input.isBlank()) {
                throw new IllegalArgumentException("Input query must not be null or empty.");
            }

            // Validate messages
            if (messages == null) {
                throw new IllegalArgumentException("Messages list must not be null.");
            }

            log.info("Executing query: {} with {} tool messages.", input, messages.size());

            return chatClient
                    .prompt()
                    .user(input)
                    .messages(messages.toArray(ToolResponseMessage[]::new))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error executing query '{}': {}", input, e.getMessage(), e);
            throw new RuntimeException("Failed to process the query. Please try again later.", e);
        }
    }
}