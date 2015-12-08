package play.cucumber;

import cucumber.runtime.CucumberException;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.formatter.CucumberJSONFormatter;
import cucumber.runtime.io.FileResourceLoader;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Tag;
import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.libs.IO;
import play.templates.Template;

import java.io.File;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static play.cucumber.formatters.FormattersFactory.*;

public class CucumberService {

	public final static String CUCUMBER_RESULT_PATH = "test-result/cucumber/";
	private static StringWriter jsonWriter;

    private static <T> boolean containsAny(List<T> from, List<T> what) {
        for (T value : from) {
            if (what.contains(value)) return true;
        }
        return false;
    }

	public static List<CucumberFeature> loadFeatures() {
		return CucumberFeature.load(new FileResourceLoader(), Collections.singletonList("features"), emptyList());
	}

    public static List<CucumberFeature> loadFeatures(final List<String> tags) {
        FileResourceLoader resourceLoader = new FileResourceLoader();
        List<CucumberFeature> features = new ArrayList<>();
        try {
            features = CucumberFeature.load(resourceLoader, Collections.singletonList("features"), emptyList());
            if (!tags.isEmpty()) {

                features = features.stream()
                        .filter(feature -> containsAny(feature.getGherkinFeature().getTags().stream().map(Tag::getName).collect(Collectors.toList()), tags))
                        .collect(Collectors.toList());
            }
        } catch (CucumberException e) {
            Logger.error(e.getMessage());
        }

        return features;
    }

	public static CucumberFeature findFeatureByUri(String uri) {
		for (CucumberFeature feature : loadFeatures()) {
			if (uri.equals(feature.getPath())) {
				return feature;
			}
		}
		return null;
	}

	public static  List<RunResult> runAllFeatures(PrintStream consoleStream, List<String> tags) {
		List<CucumberFeature> features = CucumberService.loadFeatures(tags);
		consoleStream.println("~");
		if (!tags.isEmpty()){
			consoleStream.println("~ " + tags.size() + " Cucumber tag selected.");
		}
		consoleStream.println("~ " + features.size() + " Cucumber tests to run:");
		consoleStream.println("~");
		ArrayList<RunResult> runResults = new ArrayList<>();
		int maxLength = 0;
		for (CucumberFeature feature : features) {
			if (feature.getPath().length() > maxLength) {
				maxLength = feature.getPath().length();
			}
		}
		for (CucumberFeature feature : features) {
			RunResult runResult = runFeature(feature, createJUnitFormatter(feature), createJsonFormatter(feature));
			consoleStream.print("~ " + feature.getPath() + " : ");
			for (int i = 0; i < maxLength - feature.getPath().length(); i++) {
				consoleStream.print(" ");
			}
			if (runResult.passed) {
				consoleStream.println("  PASSED");
			} else {
				if (runResult.snippets.size() > 0) {
					consoleStream.println("  SKIPPED !  ");
				} else {
					consoleStream.println("  FAILED  !  ");
				}
			}
			runResults.add(runResult);
		}
		consoleStream.println("~");
		return runResults;
	}

	public static RunResult runFeature(String uri) {
		CucumberFeature cucumberFeature = findFeatureByUri(uri);
		Formatter jUnitFormatter = createJUnitFormatter(cucumberFeature);
		return runFeature(cucumberFeature, jUnitFormatter);
	}

	private static RunResult runFeature(CucumberFeature cucumberFeature, Formatter... formatters) {
		RuntimeOptions runtimeOptions = prepareRuntimeOptions(formatters);
		ExecFeature execFeature = new ExecFeature(cucumberFeature, runtimeOptions).exec();
		return serializeExecutionResult(cucumberFeature, execFeature);
	}

	private static RuntimeOptions prepareRuntimeOptions(Formatter[] formatters) {
		RuntimeOptions runtimeOptions = new RuntimeOptions(createOptions());
		runtimeOptions.addFormatter(createConsolePrettyFormatter());
		jsonWriter = addJSONFormatter(runtimeOptions);
		for (Formatter formatter : formatters) {
			runtimeOptions.addFormatter(formatter);
		}
		return runtimeOptions;
	}

	private static RunResult serializeExecutionResult(CucumberFeature cucumberFeature,
                                                      ExecFeature execFeature) {
		// Serialize the execution Result
		File targetFile = Play.getFile(CUCUMBER_RESULT_PATH + cucumberFeature.getPath() + ".html");
		createDirectory(targetFile.getParentFile());
		List<ErrorDetail> errorDetails = buildErrors(execFeature.getRuntime().getErrors());
		Template template = play.templates.TemplateLoader.load("Cucumber/runFeature.html");
		HashMap<String, Object> args = new HashMap<>();
		args.put("feature", cucumberFeature);
		args.put("runtime", execFeature.getRuntime());
		args.put("jsonResult", execFeature.getJsonResult());
		args.put("errorDetails", errorDetails);
		String result = template.render(args);

		IO.write(result.getBytes(), targetFile);
		return new RunResult(cucumberFeature, (errorDetails.size() + execFeature.getRuntime().getSnippets().size() == 0),
				errorDetails, execFeature.getRuntime().getSnippets());
	}

	private static List<String> createOptions() {
		return asList("--dotcucumber", ".cucumber");
	}

	private static StringWriter addJSONFormatter(RuntimeOptions runtimeOptions) {
		StringWriter jsonWriter = new StringWriter();
		Formatter jsonFormatter = new CucumberJSONFormatter(jsonWriter);
		runtimeOptions.addFormatter(jsonFormatter);
		return jsonWriter;
	}

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
		List<ErrorDetail> errorDetails;
		HashSet<String> snippets;

		public RunResult(CucumberFeature cucumberFeature, boolean passed, List<ErrorDetail> errorDetails,
						 List<String> snippets) {
			this.feature = cucumberFeature;
			this.passed = passed;
			this.errorDetails = errorDetails;
			this.snippets = new HashSet<>();
			this.snippets.addAll(snippets);
		}

	}

	private static List<ErrorDetail> buildErrors(List<Throwable> failures) {
		List<ErrorDetail> errorDetails = new ArrayList<>();
		for (Throwable failure : failures) {
			ErrorDetail errorDetail = new ErrorDetail();
			errorDetail.failure = failure;
			for (StackTraceElement stackTraceElement : failure.getStackTrace()) {
				String className = stackTraceElement.getClassName();
				ApplicationClass applicationClass = Play.classes.getApplicationClass(className);
				if (applicationClass != null) {
					errorDetail.sourceFile = Play.classes.getApplicationClass(className).javaFile.relativePath();
					errorDetail.addSourceCode(Play.classes.getApplicationClass(className).javaSource,
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
		public Throwable failure;

		public void addSourceCode(String javaSource, int errorLine) {
			this.sourceCode = new ArrayList<>();
			this.errorLine = errorLine;
			String[] lines = javaSource.split("\n");
			int from = lines.length - 5 >= 0 && errorLine <= lines.length ? errorLine - 5 : 0;
			if (from > 0) {
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

	private static class ExecFeature {
		private CucumberFeature cucumberFeature;
		private RuntimeOptions runtimeOptions;
		private Runtime runtime;
		private String jsonResult;

		public ExecFeature(CucumberFeature cucumberFeature, RuntimeOptions runtimeOptions) {
			this.cucumberFeature = cucumberFeature;
			this.runtimeOptions = runtimeOptions;
		}

		public Runtime getRuntime() {
			return runtime;
		}

		public String getJsonResult() {
			return jsonResult;
		}

		public ExecFeature exec() {
			final ClassLoader classLoader = Play.classloader;
			ResourceLoader resourceLoader = new MultiLoader(classLoader);
			final PlayBackend backend = new PlayBackend(resourceLoader);
			runtime = new Runtime(resourceLoader, classLoader, Collections.singletonList(backend), runtimeOptions);
			Formatter formatter = runtimeOptions.formatter(classLoader);
			Reporter reporter = runtimeOptions.reporter(classLoader);
			cucumberFeature.run(formatter, reporter, runtime);
			formatter.done();
			jsonResult = jsonWriter.toString();
			formatter.close();
			return this;
		}
	}
}
