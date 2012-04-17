package play.cucumber;

import java.lang.reflect.Method;
import java.util.List;

import play.Play;
import cucumber.io.ResourceLoader;
import cucumber.runtime.Glue;
import cucumber.runtime.Utils;
import cucumber.runtime.java.JavaBackend;

public class PlayBackend extends JavaBackend {
	public PlayBackend(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	@Override
	public void loadGlue(Glue glue, List<String> gluePaths) {		
		super.loadGlue(glue, gluePaths);
		// add 'test' source folder in Application Classloader
		if (!Play.javaPath.contains("test")) {
			Play.javaPath.add(Play.getVirtualFile("test"));
			// Restart Applications
			Play.stop();
			Play.start();
		}
		for (Class glueCodeClass : Play.classloader.getAllClasses()) {
			while (glueCodeClass != Object.class 
					&& !glueCodeClass.isInterface() 					
					&& !Utils.isInstantiable(glueCodeClass)) {
				// those can't be instantiated without container class present.
				glueCodeClass = glueCodeClass.getSuperclass();
			}			
			for (Method method : glueCodeClass.getMethods()) {				
				loadGlue(glue, method);
			}				
		}
	}

}
