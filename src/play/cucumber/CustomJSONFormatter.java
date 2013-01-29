package play.cucumber;


import gherkin.deps.com.google.gson.Gson;
import gherkin.deps.com.google.gson.GsonBuilder;
import gherkin.deps.net.iharder.Base64;
import gherkin.formatter.Argument;
import gherkin.formatter.Formatter;
import gherkin.formatter.NiceAppendable;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomJSONFormatter implements Reporter, Formatter {
    private final List<Map<String, Object>> featureMaps = new ArrayList<Map<String, Object>>();
    private final NiceAppendable out;

    private Map<String, Object> featureMap;
    private String uri;
    //private Map currentStepOrHook;

    public CustomJSONFormatter(Appendable out) {
        this.out = new NiceAppendable(out);
    }

    @Override
    public void uri(String uri) {
        this.uri = uri;
    }

    private int currentStep;
    @Override
    public void feature(Feature feature) {
    	featureMap = feature.toMap();
        featureMap.put("uri", uri);
        featureMaps.add(featureMap);
    }

    @Override
    public void background(Background background) {
    	currentStep=0;    	
        getFeatureElements().add(background.toMap());
    }

    @Override
    public void scenario(Scenario scenario) {
    	currentStep=0;    	
        getFeatureElements().add(scenario.toMap());
    }

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {    	
        getFeatureElements().add(scenarioOutline.toMap());
    }

    @Override
    public void examples(Examples examples) {
        getAllExamples().add(examples.toMap());
    }

    @Override
    public void step(Step step) {
    	getSteps().add(step.toMap());
    }

    @Override
    public void match(Match match) {
    	Map step = getStep(currentStep);
    	if(step!=null){
    	    step.put("match", match.toMap());
    	}
    }
    
    private Map getStep(int index){
    	List<Map> steps =  getFeatureElement().get("steps");
    	if(steps.size()>currentStep){
    		Map step = getFeatureElement().get("steps").get(index);
	    	return step;
    	}else{
    		System.out.println("Impossible de trouver la step "+index+" steps: "+steps.size());
    		return null;
    	}
    }

    @Override
    public void embedding(String mimeType, byte[] data) {
        final Map<String, String> embedding = new HashMap<String, String>();
        embedding.put("mime_type", mimeType);
        embedding.put("data", Base64.encodeBytes(data));
        getEmbeddings().add(embedding);
    }

    @Override
    public void write(String text) {    	
        getOutput().add(text);
    }

    @Override
    public void result(Result result) {
        Map step = getStep(currentStep);
    	if(step!=null){
    	    step.put("result", result.toMap());
    	}        
        currentStep++;
    }
    
    @Override
    public void before(Match match, Result result) {
    }

    @Override
    public void after(Match match, Result result) {
    }

    @Override
    public void eof() {
    }

    @Override
    public void done() {
        out.append(gson().toJson(featureMaps));
        // We're *not* closing the stream here.
        // https://github.com/cucumber/gherkin/issues/151
        // https://github.com/cucumber/cucumber-jvm/issues/96
    }

    @Override
    public void close() {
        out.close();
    }

    @Override
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {
    	//TODO report syntax error in browser
    	System.err.println("#### syntaxError "+uri+" line: "+line);       
    }

    private List<Map<String, Object>> getFeatureElements() {
        List<Map<String, Object>> featureElements = (List) featureMap.get("elements");
        if (featureElements == null) {
            featureElements = new ArrayList<Map<String, Object>>();
            featureMap.put("elements", featureElements);
        }
        return featureElements;
    }

    private Map<Object, List<Map>> getFeatureElement() {
        return (Map) getFeatureElements().get(getFeatureElements().size() - 1);
    }

    private List<Map> getAllExamples() {
        List<Map> allExamples = getFeatureElement().get("examples");
        if (allExamples == null) {
            allExamples = new ArrayList<Map>();
            getFeatureElement().put("examples", allExamples);
        }
        return allExamples;
    }

    private List<Map> getSteps() {
        List<Map> steps = getFeatureElement().get("steps");
        if (steps == null) {
            steps = new ArrayList<Map>();
            getFeatureElement().put("steps", steps);
        }
        return steps;
    }

    private List<Map<String, String>> getEmbeddings() {
    	//TODO write screenshot in tempfile and render in dashboard
    	/*
        List<Map<String, String>> embeddings = (List<Map<String, String>>) currentStepOrHook.get("embeddings");
        if (embeddings == null) {
            embeddings = new ArrayList<Map<String, String>>();
            currentStepOrHook.put("embeddings", embeddings);
        }
        return embeddings;
        */
    	return Collections.emptyList();
    }

    private List<String> getOutput() {
    	/*
        List<String> output = (List<String>) currentStepOrHook.get("output");
        if (output == null) {
            output = new ArrayList<String>();
            currentStepOrHook.put("output", output);
        }        
        return output;
        */
    	return Collections.emptyList();
    }

    protected Gson gson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }
}