package net.breezeware.dynamo.ai.agent.agentExecutor;

import net.breezeware.dynamo.ai.agent.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Service responsible for executing agent workflows. This includes handling weather, travel,
 * and food recommendations based on user inputs.
 */
@Slf4j
@Service
public class AgentExecutor {

    private final AgentService agentService;

    /**
     * Constructor to inject dependencies.
     *
     * @param agentService The service used for agent interactions.
     */
    public AgentExecutor(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Calls the Weather Agent to retrieve weather information based on user input.
     *
     * @param state The current state of the workflow.
     * @return A map containing the weather details as output.
     */
    Map<String, Object> callWeatherAgent(State state) {
        log.info("callWeatherAgent: {}", state);

        log.info("Weather Agent Input: {}", state.getInput());

        var query = (String) state.getInput().get("query");
        var response = agentService.execute(query, List.of());

        Map<String, Object> output = new HashMap<>();
        output.put("weather", response);
        log.info("Weather Agent Output: {}", output);

        return Map.of(State.OUTPUT, output);
    }

    /**
     * Calls the Travel Agent to provide travel recommendations based on the weather.
     *
     * @param state The current state of the workflow.
     * @return A map containing travel recommendations as output.
     */
    Map<String, Object> callTravelAgent(State state) {
        log.info("callTravelAgent: {}", state);

        var weather = (String) state.getOutput().get("weather");

        String recommendation;
        if (weather.contains("rain")) {
            recommendation = "Visit indoor attractions like museums, art galleries, or enjoy a day at the mall.";
        } else if (weather.contains("cloudy")) {
            recommendation = "Consider activities like visiting an aquarium, a science center, or enjoying a cozy café.";
        } else if (weather.contains("storm") || weather.contains("thunder")) {
            recommendation = "Stay safe indoors. Enjoy a good book, watch a movie, or try indoor yoga.";
        } else if (weather.contains("snow")) {
            recommendation = "Explore indoor winter activities like skating in an indoor rink or sipping hot chocolate by a fireplace.";
        } else if (weather.contains("misty")) {
            recommendation = "Visit indoor attractions like museums, art galleries, or enjoy a day at the mall.";
        } else {
            recommendation = "Enjoy outdoor activities like hiking, biking, or a picnic in the park!";
        }

        Map<String, Object> output = new HashMap<>();
        output.put("recommendation", recommendation);
        log.info("Travel Agent Output: {}", output);

        return Map.of(State.MID, output);
    }

    /**
     * Calls the Food Agent to provide food suggestions based on travel recommendations.
     *
     * @param state The current state of the workflow.
     * @return A map containing food suggestions as output.
     */
    Map<String, Object> callFoodAgent(State state) {
        log.info("callFoodAgent: {}", state);

        var recommendation = (String) state.getMID().get("recommendation");

        String foodSuggestion;
        if (recommendation.contains("outdoor")) {
            foodSuggestion = "Pack some easy-to-carry snacks like sandwiches, granola bars, and fresh fruit. Don't forget plenty of water!";
        } else if (recommendation.contains("cloudy")) {
            foodSuggestion = "Warm up with comfort food like soups, hot beverages, or enjoy a cozy brunch at a nearby bakery.";
        } else if (recommendation.contains("snow") || recommendation.contains("rain") || recommendation.contains("misty")) {
            foodSuggestion = "Enjoy hearty meals like stews, hot chocolate, or baked goods to keep you warm.";
        } else {
            foodSuggestion = "Explore the local street food scene or grab a quick bite from food trucks in the area.";
        }

        Map<String, Object> output = new HashMap<>();
        output.put("food", foodSuggestion);
        log.info("Food Agent Output: {}", output);

        return Map.of(State.FOOD, output);
    }

    /**
     * Provides a builder to construct the workflow graph.
     *
     * @return An instance of GraphBuilder.
     */
    public GraphBuilder graphBuilder() {
        return new GraphBuilder();
    }

    /**
     * Represents the state of the workflow, including input, intermediate, and output data.
     */
    public static class State extends AgentState {
        public static final String INPUT = "question";
        public static final String MID = "recommendation";
        public static final String OUTPUT = "weather";
        public static final String FOOD = "food";

        static Map<String, Channel<?>> SCHEMA = Map.of(
                INPUT, Channel.of(() -> new HashMap<>()),
                OUTPUT, Channel.of(() -> new HashMap<>()),
                MID, Channel.of(() -> new HashMap<>())
        );

        /**
         * Constructor to initialize state with given data.
         *
         * @param initData Initial data for the state.
         */
        public State(Map<String, Object> initData) {
            super(initData);
        }

        public Map<String, Object> getInput() {
            return this.<Map<String, Object>>value(INPUT).orElseGet(HashMap::new);
        }

        public Map<String, Object> getOutput() {
            return this.<Map<String, Object>>value(OUTPUT).orElseGet(HashMap::new);
        }

        public Map<String, Object> getMID() {
            return this.<Map<String, Object>>value(MID).orElseGet(HashMap::new);
        }
    }

    /**
     * Builder class to construct a StateGraph for the agent workflow.
     */
    public class GraphBuilder {

        /**
         * Builds the workflow graph by defining nodes and transitions.
         *
         * @return The constructed StateGraph.
         * @throws GraphStateException If the graph cannot be constructed.
         */
        public StateGraph<State> build() throws GraphStateException {
            var shouldContinue = (EdgeAction<State>) state -> {
                log.info("shouldContinue state: {}", state);
                return state.getInput().containsKey("recommendations") ? "travelAgent" : "end";
            };

            return new StateGraph<>(State.SCHEMA, State::new)
                    .addEdge(START, "weatherAgent")
                    .addNode("weatherAgent", node_async(AgentExecutor.this::callWeatherAgent))
                    .addConditionalEdges("weatherAgent",
                            edge_async(shouldContinue),
                            Map.of(
                                    "travelAgent", "travelAgent",
                                    "end", END
                            )
                    )
                    .addNode("travelAgent", node_async(AgentExecutor.this::callTravelAgent))
                    .addEdge("travelAgent", "foodAgent")
                    .addNode("foodAgent", node_async(AgentExecutor.this::callFoodAgent))
                    .addEdge("foodAgent", END);
        }
    }
}
