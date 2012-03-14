package controllers;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Feature;

import java.io.File;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


import org.junit.runner.notification.Failure;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.cucumber.CucumberService;
import play.cucumber.CucumberService.RunResult;
import play.libs.IO;
import play.mvc.Controller;
import play.mvc.results.RenderStatic;
import play.templates.JavaExtensions;
import play.templates.Template;
import play.test.TestEngine.TestResult;
import play.vfs.VirtualFile;
import cucumber.annotation.After;
import cucumber.annotation.Before;
import cucumber.annotation.Order;
import cucumber.cli.DefaultRuntimeFactory;
import cucumber.cli.RuntimeFactory;
import cucumber.formatter.FormatterFactory;
import cucumber.formatter.MultiFormatter;
import cucumber.io.ClasspathResourceLoader;
import cucumber.io.FileResourceLoader;
import cucumber.io.Resource;
import cucumber.io.ResourceLoader;
import cucumber.runtime.Glue;
import cucumber.runtime.Runtime;
import cucumber.runtime.Utils;
import cucumber.runtime.java.JavaBackend;
import cucumber.runtime.java.StepDefAnnotation;
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
		out.println("~ Run cucumber Tests in: "+(stop-start)+"ms.");
		out.println("~");
	}

}