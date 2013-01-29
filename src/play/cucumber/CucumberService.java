package play.cucumber;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import gherkin.formatter.Formatter;
import gherkin.formatter.JSONFormatter;
import gherkin.formatter.JSONPrettyFormatter;
import gherkin.formatter.Reporter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.libs.IO;
import play.templates.Template;
import cucumber.runtime.CucumberException;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.ClasspathResourceLoader;
import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.model.CucumberFeature;

public class CucumberService {
	
	public static List<CucumberFeature> loadFeatures() {
		FileResourceLoader resourceLoader = new FileResourceLoader();
		List<CucumberFeature> features = Collections.emptyList();
		try{
			features=CucumberFeature.load(resourceLoader, asList("features"), emptyList());
		}catch(CucumberException e){
			//nothing to do when no features found
		}
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
		consoleStream.println("~ " + features.size() + " Cucumber tests to run:");
		consoleStream.println("~");
		ArrayList<RunResult> runResults = new ArrayList<RunResult>();
		int maxLength = 0;
		for (CucumberFeature feature : features) {
			if (feature.getUri().length() > maxLength) {
				maxLength = feature.getUri().length();
			}
		}
		//Formatter jUnitFormatter = createJUnitFormatter();
		boolean dryRun = false;
		for (CucumberFeature feature : features) {			
			//RunResult runResult = runFeature(feature, dryRun, jUnitFormatter);
			RunResult runResult = runFeature(feature, dryRun);
			consoleStream.print("~ " + feature.getUri() + " : ");
			for (int i = 0; i < maxLength - feature.getUri().length(); i++) {
				consoleStream.print(" ");
			}
			if (runResult.passed) {
				consoleStream.println("  PASSED");
			} else {
				if(runResult.snippets.size()>0){
					consoleStream.println("  SKIPPED !  ");
				}else{
					consoleStream.println("  FAILED  !  ");	
				}				
			}
			runResults.add(runResult);
		}
		consoleStream.println("~");
		return runResults;
	}

	public static RunResult runFeature(String uri) {
		CucumberFeature cucumberFeature = CucumberService.findFeatureByUri(uri);
		//Formatter jUnitFormatter = createJUnitFormatter();
		boolean dryRun = false;
		//return runFeature(cucumberFeature, dryRun, jUnitFormatter);
		return runFeature(cucumberFeature, dryRun);
	}

	private final static String CUCUMBER_RESULT_PATH = "test-result/cucumber/";

	private static RunResult runFeature(CucumberFeature cucumberFeature, boolean dryRun, Formatter...formatters) {
		Properties properties = Play.configuration; 
		RuntimeOptions runtimeOptions = new RuntimeOptions(properties);

		// Remove the progress formater (because it closes the default output stream)
		for (Formatter formatter : runtimeOptions.formatters) {
			if (formatter.getClass().getSimpleName().equals("ProgressFormatter")) {
				runtimeOptions.formatters.remove(formatter);
				break;
			}
		}

		// Configure Runtime
		runtimeOptions.dryRun = dryRun;
		runtimeOptions.dotCucumber = Play.getFile(CUCUMBER_RESULT_PATH);
		//StringWriter prettyWriter = null;
		//prettyWriter = addPrettyFormatter(runtimeOptions);
		addPrettyFormatter(runtimeOptions);
		StringWriter jsonWriter = addJSONFormatter(runtimeOptions);
		for(Formatter formatter:formatters){
			runtimeOptions.formatters.add(formatter);
		}
		// Exec Feature
		final ClassLoader classLoader = Play.classloader;
		ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader(classLoader);
		final PlayBackend backend = new PlayBackend(resourceLoader);
		final Runtime runtime = new Runtime(resourceLoader, classLoader, asList(backend), runtimeOptions);
		Formatter formatter = runtimeOptions.formatter(classLoader);
		Reporter reporter = runtimeOptions.reporter(classLoader);
		cucumberFeature.run(formatter, reporter, runtime);
		formatter.done();
		//String prettyResult = prettyWriter.toString();
		//	System.out.println(prettyResult);
		//}
		String jsonResult = jsonWriter.toString();
		formatter.close();

		// Serialize the execution Result
		File targetFile = Play.getFile(CUCUMBER_RESULT_PATH + cucumberFeature.getUri() + ".html");
		createDirectory(targetFile.getParentFile());
		List<ErrorDetail> errorDetails = buildErrors(runtime.getErrors());
		Template template = play.templates.TemplateLoader.load("Cucumber/runFeature.html");
		HashMap<String, Object> args = new HashMap<String, Object>();
		args.put("feature", cucumberFeature);
		args.put("runtime", runtime);
		args.put("jsonResult", jsonResult);
		args.put("errorDetails", errorDetails);
		String result = template.render(args);

		IO.write(result.getBytes(), targetFile);
		return new RunResult(cucumberFeature, (errorDetails.size() + runtime.getSnippets().size() == 0)/*, prettyResult*/, errorDetails, runtime.getSnippets());
	}

	private static void addPrettyFormatter(RuntimeOptions runtimeOptions) {
		//StringWriter prettyWriter = new StringWriter();
		Appendable consoleStream = new Appendable() {
			@Override
			public Appendable append(CharSequence csq, int start, int end) throws IOException {
				System.out.append(csq, start, end);
				return this;
			}
			
			@Override
			public Appendable append(char c) throws IOException {
				System.out.append(c);
				return this;
			}
			
			@Override
			public Appendable append(CharSequence csq) throws IOException {
				System.out.append(csq);
				return this;
			}
		};
		//CucumberPrettyFormatter prettyFormatter = new CucumberPrettyFormatter(prettyWriter);
		Formatter prettyFormatter=null;
		try {
			Class prettyFormatterClass = Class.forName("cucumber.runtime.formatter.CucumberPrettyFormatter");
			Constructor<Formatter> constructor = prettyFormatterClass.getDeclaredConstructor(Appendable.class);
			constructor.setAccessible(true);
			prettyFormatter = constructor.newInstance(consoleStream);			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}		
		runtimeOptions.formatters.add(prettyFormatter);
		//return prettyWriter;		
	}
	

	private static StringWriter addJSONFormatter(RuntimeOptions runtimeOptions) {
		StringWriter jsonWriter = new StringWriter();
		//JSONFormatter jsonFormatter = new JSONFormatter(jsonWriter);
		//JSONPrettyFormatter jsonFormatter = new JSONPrettyFormatter(jsonWriter); 
		Formatter jsonFormatter = new CustomJSONFormatter(jsonWriter);		
		runtimeOptions.formatters.add(jsonFormatter);
		return jsonWriter;
	}

	/*
	private static Formatter createJUnitFormatter() {
		//Formatter junitFormatter = new JUnitFormatter(Play.getFile(CUCUMBER_RESULT_PATH + "junit-report.xml"));
		Formatter junitFormatter=null;
		try {
			Class junitFormatterClass = Class.forName("cucumber.runtime.formatter.JUnitFormatter");
			Constructor<Formatter> constructor = junitFormatterClass.getDeclaredConstructor(File.class);
			constructor.setAccessible(true);
			junitFormatter = constructor.newInstance(Play.getFile(CUCUMBER_RESULT_PATH + "junit-report.xml"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}		
		return junitFormatter;
	}
	*/

	private static void createDirectory(File dir) {
		if (!dir.getParentFile().exists()) {
			createDirectory(dir.getParentFile());
		}
		if (!dir.exists()) {
			dir.mkdir();
		}
	}

	public static class RunResult {
		CucumberFeature feature;
		boolean passed;
		//String prettyResult;
		List<ErrorDetail> errorDetails;
		HashSet<String> snippets;

		public RunResult(CucumberFeature cucumberFeature, boolean passed, /*String prettyResult,*/ List<ErrorDetail> errorDetails, List<String> snippets) {
			this.feature = cucumberFeature;
			this.passed = passed;
			//this.prettyResult = prettyResult;
			this.errorDetails = errorDetails;
			this.snippets = new HashSet<String>();
			this.snippets.addAll(snippets);
		}

	}

	private static List<ErrorDetail> buildErrors(List<Throwable> failures) {
		List<ErrorDetail> errorDetails = new ArrayList<ErrorDetail>();
		for (Throwable failure : failures) {
			ErrorDetail errorDetail = new ErrorDetail();
			errorDetail.failure=failure;
			for (StackTraceElement stackTraceElement : failure.getStackTrace()) {
				String className = stackTraceElement.getClassName();
				ApplicationClass applicationClass = Play.classes.getApplicationClass(className);
				if (applicationClass != null) {
					errorDetail.sourceFile = Play.classes.getApplicationClass(className).javaFile.relativePath();
					errorDetail.addSourceCode(Play.classes.getApplicationClass(className).javaSource, stackTraceElement.getLineNumber());
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
		public Throwable failure;

		public void addSourceCode(String javaSource, int errorLine) {
			this.sourceCode = new ArrayList<SourceLine>();
			this.errorLine = errorLine;
			String[] lines = javaSource.split("\n");
			int from = lines.length - 5 >= 0 && errorLine <= lines.length ? errorLine - 5 : 0;
			if(from>0){
				int to = errorLine + 5 < lines.length ? errorLine + 5 : lines.length - 1;
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
	}

	public static class SourceLine {
		String code;
		int lineNumber;
		boolean isInError = false;
	}

}
