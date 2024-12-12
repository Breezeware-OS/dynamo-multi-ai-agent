package net.breezeware.dynamo.ai.agent;

import net.breezeware.dynamo.ai.agent.agentExecutor.AgentExecutor;
import net.breezeware.dynamo.ai.agent.service.WeatherConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Map;

/**
 * Entry point for the Multi-Agent AI Application. This application initializes the agent workflow,
 * processes user queries, and generates results based on the configured state graph.
 */
@SpringBootApplication
@Slf4j
@EnableConfigurationProperties(WeatherConfigProperties.class)
public class AgentApplication implements CommandLineRunner {

    private final AgentExecutor agentExecutor;

    /**
     * Constructor for injecting the required dependencies.
     *
     * @param agentExecutor The executor responsible for managing agent workflows.
     */
    public AgentApplication(AgentExecutor agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    /**
     * Main method to bootstrap the Spring Boot application.
     *
     * @param args Command-line arguments passed during application startup.
     */
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }

    /**
     * Executes after the application context is loaded. Builds and invokes the agent workflow graph.
     *
     * @param args Command-line arguments.
     * @throws Exception If an error occurs during graph execution.
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Multi-Agent AI Application");

        // Build the state graph using the AgentExecutor
        StateGraph<AgentExecutor.State> graph = agentExecutor.graphBuilder().build();
        var app = graph.compile();

        // Input data to initialize the workflow
        var inputData = Map.<String, Object>of(
                AgentExecutor.State.INPUT, Map.of("query", "Current weather in Atlanta")
        );

        // Input data to initialize the workflow for full node execution
//        var inputData = Map.<String, Object>of(
//                AgentExecutor.State.INPUT, Map.of("query", "Current weather in Atlanta & Travel recommendations")
//        );
        

        // Execute the workflow and log the final output
        var result = app.invoke(inputData);
        log.info("Final Output: {}", result);
    }
}