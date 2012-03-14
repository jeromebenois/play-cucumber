package play.cucumber;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;

import java.io.File;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.ApplicationClassloader;
import play.libs.IO;
import play.templates.Template;
import cucumber.formatter.FormatterFactory;
import cucumber.formatter.MultiFormatter;
import cucumber.io.ClasspathResourceLoader;
import cucumber.io.FileResourceLoader;
import cucumber.runtime.Runtime;
import cucumber.runtime.Utils;
import cucumber.runtime.java.JavaBackend;
import cucumber.runtime.model.CucumberFeature;

public class CucumberService {

	public static List<CucumberFeature> loadFeatures() {
		FileResourceLoader resourceLoader = new FileResourceLoader();
		List<CucumberFeature> features = CucumberFeature.load(resourceLoader,
				asList("features"), emptyList());
		return features;
	}

	public static CucumberFeature findFeatureByUri(String uri) {
		for (CucumberFeature feature : loadFeatures()) {
			if (uri.equals(feature.getUri())) {
				return feature;
			}
		}
		return null;
	}

	public static List<RunResult> runAllFeatures(PrintStream consoleStream) {
		List<CucumberFeature> features = CucumberService.loadFeatures();
		consoleStream.println("~");
		consoleStream.println("~ "+features.size()+" Cucumber tests to run:");
		consoleStream.println("~");
		ArrayList<RunResult> runResults = new ArrayList<RunResult>();
		Runtime runtime = createRuntime();	
		int maxLength=0;
		for (CucumberFeature feature : features) {
			if(feature.getUri().length()>maxLength){
				maxLength=feature.getUri().length();
			}
		}	    		
		for (CucumberFeature feature : features) {
			RunResult runResult = runFeature(feature, runtime);
			consoleStream.print("~ "+feature.getUri()+" : ");
			for (int i = 0; i < maxLength - feature.getUri().length(); i++) {
				consoleStream.print(" ");
	        }
			if(runResult.passed){
				consoleStream.println("  PASSED");
			}else{
				consoleStream.println("  FAILED  !  ");
			}
			runResults.add(runResult);
		}
		consoleStream.println("~");
		return runResults;
	}

	public static RunResult runFeature(String uri) {
		CucumberFeature cucumberFeature = CucumberService.findFeatureByUri(uri);		
		Runtime runtime = createRuntime();		
		return runFeature(cucumberFeature, runtime);
	}

	private static RunResult runFeature(CucumberFeature cucumberFeature, Runtime runtime) {
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		FormatterFactory formatterFactory = new FormatterFactory(classLoader);
		// Formatters: progress, html, json, json-pretty, pretty
		MultiFormatter multiFormatter = new MultiFormatter(classLoader);
		// StringWriter prettyWriter = new StringWriter();
		// multiFormatter.add(formatterFactory.createFormatter("pretty",prettyWriter));
		StringWriter jsonWriter = new StringWriter();
		multiFormatter
				.add(formatterFactory.createFormatter("json", jsonWriter));
		Formatter formatter = multiFormatter.formatterProxy();
		Reporter reporter = multiFormatter.reporterProxy();

		runtime.run(cucumberFeature, formatter, reporter);
		formatter.done();
		formatter.close();				
		
		// String ansiResult = prettyWriter.toString();
		// System.out.println(ansiResult);
		String jsonResult = jsonWriter.toString();
		List<ErrorDetail> errorDetails = buildErrors(runtime.getErrors());

		Template template = play.templates.TemplateLoader
				.load("Cucumber/runFeature.html");
		HashMap<String, Object> args = new HashMap<String, Object>();
		args.put("feature", cucumberFeature);
		args.put("runtime", runtime);
		args.put("jsonResult", jsonResult);
		args.put("errorDetails", errorDetails);
		String result = template.render(args);
		
		File targetFile = Play.getFile("test-result/cucumber/" + cucumberFeature.getUri() + ".html");		
		createDirectory(targetFile.getParentFile());
		
		IO.write(result.getBytes(), targetFile);
		return new RunResult(cucumberFeature, (errorDetails.size()
				+ runtime.getSnippets().size() == 0));
	}
	
	private static void createDirectory(File dir){		
		if(!dir.getParentFile().exists()){
			createDirectory(dir.getParentFile());			
		}
		if(!dir.exists()){
			dir.mkdir();	
		}		
	}
	
	public static class RunResult {
		CucumberFeature feature;
		boolean passed;

		public RunResult(CucumberFeature cucumberFeature, boolean passed) {
			this.feature = cucumberFeature;
			this.passed = passed;
		}

	}

	private static Runtime createRuntime() {
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader(classLoader);
		List<String> gluePaths = new ArrayList<String>();
		final PlayBackend backend = new PlayBackend(resourceLoader);
		boolean isDryRun = false;
		final Runtime runtime = new Runtime(resourceLoader, gluePaths,
				classLoader, asList(backend), isDryRun);
		return runtime;
	}

	private static List<ErrorDetail> buildErrors(List<Throwable> failures) {
		List<ErrorDetail> errorDetails = new ArrayList<ErrorDetail>();
		for (Throwable failure : failures) {
			ErrorDetail errorDetail = new ErrorDetail();
			for (StackTraceElement stackTraceElement : failure.getStackTrace()) {
				String className = stackTraceElement.getClassName();
				ApplicationClass applicationClass = Play.classes
						.getApplicationClass(className);
				if (applicationClass != null) {
					errorDetail.sourceFile = Play.classes
							.getApplicationClass(className).javaFile
							.relativePath();
					errorDetail
							.addSourceCode(
									Play.classes.getApplicationClass(className).javaSource,
									stackTraceElement.getLineNumber());
				}
			}
			errorDetails.add(errorDetail);
		}
		return errorDetails;
	}

	public static class ErrorDetail {
		public String sourceFile;
		public int errorLine;
		public List<SourceLine> sourceCode;

		public void addSourceCode(String javaSource, int errorLine) {
			this.sourceCode = new ArrayList<SourceLine>();
			this.errorLine = errorLine;
			String[] lines = javaSource.split("\n");
			int from = lines.length - 5 >= 0 && errorLine <= lines.length ? errorLine - 5
					: 0;
			int to = errorLine + 5 < lines.length ? errorLine + 5
					: lines.length - 1;
			for (int i = from; i < to; i++) {
				SourceLine sourceLine = new SourceLine();
				sourceLine.code = lines[i];
				sourceLine.lineNumber = i + 1;
				if (sourceLine.lineNumber == errorLine) {
					sourceLine.isInError = true;
				}
				sourceCode.add(sourceLine);
			}
		}
	}

	public static class SourceLine {
		String code;
		int lineNumber;
		boolean isInError = false;
	}

}
