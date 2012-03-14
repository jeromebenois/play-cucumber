package play.cucumber;

import gherkin.formatter.model.Step;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import play.Play;
import play.classloading.ApplicationClassloader;
import play.test.FunctionalTest;
import play.test.TestEngine;
import cucumber.annotation.After;
import cucumber.annotation.Before;
import cucumber.annotation.Order;
import cucumber.fallback.runtime.java.DefaultJavaObjectFactory;
import cucumber.io.ClasspathResourceLoader;
import cucumber.io.ResourceLoader;
import cucumber.runtime.Backend;
import cucumber.runtime.CucumberException;
import cucumber.runtime.Glue;
import cucumber.runtime.UnreportedStepExecutor;
import cucumber.runtime.Utils;
import cucumber.runtime.java.ClasspathMethodScanner;
import cucumber.runtime.java.JavaBackend;
import cucumber.runtime.java.JavaHookDefinition;
import cucumber.runtime.java.JavaSnippet;
import cucumber.runtime.java.JavaStepDefinition;
import cucumber.runtime.java.ObjectFactory;
import cucumber.runtime.java.ObjectFactoryHolder;
import cucumber.runtime.java.StepDefAnnotation;
import cucumber.runtime.snippets.SnippetGenerator;

public class PlayBackend extends JavaBackend {
	public PlayBackend(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	@Override
	public void loadGlue(Glue glue, List<String> gluePaths) {			
		super.loadGlue(glue, gluePaths);
		//add 'test' source folder in Application Classloader
		if(!Play.javaPath.contains("test")){		
			Play.javaPath.add(Play.getVirtualFile("test"));
			//Force reload Application Classloader
			Play.classloader = new ApplicationClassloader();            
		}		
		Collection<Class<? extends Annotation>> cucumberAnnotationClasses = getClasspathMethodScanner().findCucumberAnnotationClasses();
		for (Class glueCodeClass : Play.classloader.getAllClasses()){							
			while (glueCodeClass != Object.class
					&& !Utils.isInstantiable(glueCodeClass)) {
				// those can't be instantiated without container class present.
				glueCodeClass = glueCodeClass.getSuperclass();
			}
			for (Method method : glueCodeClass.getMethods()) {
				getClasspathMethodScanner().scan(glueCodeClass, method, cucumberAnnotationClasses,this);
			}			
		}
	}
	
}
