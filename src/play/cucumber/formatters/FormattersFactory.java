package play.cucumber.formatters;

import cucumber.runtime.formatter.FormatterFactory;
import cucumber.runtime.model.CucumberFeature;
import gherkin.formatter.Formatter;
import play.Play;
import play.cucumber.CucumberService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class FormattersFactory {
    @SuppressWarnings("unchecked")
    public static Formatter createConsolePrettyFormatter() {
        Formatter prettyFormatter = null;
        try {
            Class prettyFormatterClass = Class.forName("cucumber.runtime.formatter.CucumberPrettyFormatter");
            Constructor<Formatter> constructor = prettyFormatterClass.getDeclaredConstructor(Appendable.class);
            constructor.setAccessible(true);
            prettyFormatter = constructor.newInstance(new ConsoleAppendable());
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
        return prettyFormatter;
    }

    public static Formatter createJUnitFormatter(CucumberFeature cucumberFeature) {
        String reportFileName = reportFilePath(cucumberFeature) + "-junit-report.xml";
        Play.getFile(CucumberService.CUCUMBER_RESULT_PATH).mkdir();
        return new CustomJUnitFormatter(Play.getFile(CucumberService.CUCUMBER_RESULT_PATH + reportFileName));
    }

    private static String reportFilePath(CucumberFeature cucumberFeature) {
        return escapeSlashAndBackSlash(cucumberFeature.getPath());
    }

    private static String escapeSlashAndBackSlash(String s) {
        return s.replaceAll("\\\\", "_").replaceAll("/", "_");
    }

    public static Formatter createJsonFormatter(CucumberFeature cucumberFeature) {
        return new FormatterFactory().create("json:" + CucumberService.CUCUMBER_RESULT_PATH + reportFilePath(cucumberFeature) + ".json");
    }
}
