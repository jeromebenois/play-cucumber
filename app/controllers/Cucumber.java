package controllers;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;

import play.Play;
import play.cucumber.CucumberService;
import play.cucumber.CucumberService.RunResult;
import play.libs.IO;
import play.mvc.Controller;
import cucumber.runtime.model.CucumberFeature;

public class Cucumber extends Controller {

	public static void index() {
		List<CucumberFeature> features = CucumberService.loadFeatures();
		render(features);
	}

	public static void showFeature(String uri) {		
		CucumberFeature feature = CucumberService.findFeatureByUri(uri);		
		render(feature);
	}

	public static void showFeatureExecResult(String uri) {
		String result = IO.readContentAsString(Play.getFile("test-result/cucumber/"+uri+".html"));
		renderHtml(result);
	}
	
	public static void runAllFromCommandLine() {
		PrintStream stream = new PrintStream(response.out);
		long start = System.currentTimeMillis();
		CucumberService.runAllFeatures(stream);
		printElapsedTime(start, stream);	
	}
	
	public static void runAll() {		
		PrintStream stream = System.out;
		long start = System.currentTimeMillis();
		List<RunResult> runResults = CucumberService.runAllFeatures(stream);
		printElapsedTime(start, stream);
		render(runResults);
	}

	public static void runFeature(String uri) {		
		long start = System.currentTimeMillis();
		CucumberService.runFeature(uri);
		printElapsedTime(start, System.out);
		showFeatureExecResult(uri);
	}
	
	private static void printElapsedTime(long start, PrintStream out){
		long stop = System.currentTimeMillis();
		long elapsedTime = stop-start;
		DecimalFormat mask = new DecimalFormat("#0.##");
		out.print("~ Run cucumber Tests in: ");		 
		if(elapsedTime>1000){
			float seconds = (float)elapsedTime/1000;
			out.println(mask.format(seconds)+"s.");
		}else{
			out.println(mask.format(elapsedTime)+"ms.");	
		}		
		out.println("~");
	}

}