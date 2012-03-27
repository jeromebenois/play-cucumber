package play.cucumber;

import play.Play;
import play.PlayPlugin;
import play.mvc.Router;
import play.plugins.PluginCollection;

public class CucumberPlugin extends PlayPlugin {

	@Override
	public void onApplicationReady() {
		String protocol = "http";
		String port = "9000";
		if (Play.configuration.getProperty("https.port") != null) {
			port = Play.configuration.getProperty("https.port");
			protocol = "https";
		} else if (Play.configuration.getProperty("http.port") != null) {
			port = Play.configuration.getProperty("http.port");
		}
		System.out.println("~");
		System.out.println("~ Go to " + protocol + "://localhost:" + port
				+ "/@cukes to run the cucumber tests");
		System.out.println("~");
	}

	@Override
	public void onRoutesLoaded() {
		if (Play.mode.isDev()) {
			Router.addRoute("GET", "/@cukes/run.cli", "Cucumber.runAllFromCommandLine");
			Router.addRoute("GET", "/@cukes/run", "Cucumber.runAll");					
			Router.addRoute("GET", "/@cukes/run/{<.*>uri}","Cucumber.runFeature");
			Router.addRoute("GET", "/@cukes/result/{<.*>uri}","Cucumber.showFeatureExecResult");
			Router.addRoute("GET", "/@cukes/feature/{<.*>uri}","Cucumber.showFeature");
			Router.addRoute("GET", "/@cukes", "Cucumber.index");
		}
	}

}
