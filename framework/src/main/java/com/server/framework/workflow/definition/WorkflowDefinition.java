package com.server.framework.workflow.definition;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class WorkflowDefinition {
    
    private String name;
    private String version;
    private String description;
    private String initialState;
    private Map<String, WorkflowStep> steps;
    private Map<String, List<WorkflowTransition>> transitions;

    public WorkflowDefinition() {
    }

    @PostConstruct
    public void initializeSteps() {
        initializeDefinition();
    }

    protected abstract void initializeDefinition();


    public List<WorkflowTransition> getValidTransitions(String fromState, String eventType) {
        return transitions.getOrDefault(fromState, List.of())
            .stream()
            .filter(t -> t.getEventType().equals(eventType))
            .collect(Collectors.toList());
    }

    public WorkflowStep getStep(String stateName) {
        return steps.get(stateName);
    }

    public boolean isTerminalState(String stateName) {
        return !transitions.containsKey(stateName) || transitions.get(stateName).isEmpty();
    }

    public String getName() { return name; }
    protected void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    protected void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    protected void setDescription(String description) { this.description = description; }

    public String getInitialState() { return initialState; }
    protected void setInitialState(String initialState) { this.initialState = initialState; }

    public Map<String, WorkflowStep> getSteps() { return steps; }
    protected void setSteps(Map<String, WorkflowStep> steps) { this.steps = steps; }

    public Map<String, List<WorkflowTransition>> getTransitions() { return transitions; }
    protected void setTransitions(Map<String, List<WorkflowTransition>> transitions) { this.transitions = transitions; }

    protected void addStep(String stateName, WorkflowStep step) {
        steps.put(stateName, step);
    }

    protected void addTransition(String fromState, WorkflowTransition transition) {
        transitions.computeIfAbsent(fromState, k -> new java.util.ArrayList<>()).add(transition);
    }

    protected void addTransitions(String fromState, List<WorkflowTransition> transitions) {
        this.transitions.computeIfAbsent(fromState, k -> new java.util.ArrayList<>()).addAll(transitions);
    }
}
