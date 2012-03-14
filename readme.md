# Play! Framework Cucumber plugin


This plugin provide a [cucumber-jvm](https://github.com/cucumber/cucumber-jvm) integration into the Play! Framework. [Cucumber](https://github.com/cucumber/cucumber/wiki/) (aka Cukes) is a tool that executes plain-text functional descriptions as automated tests. 

## Install
   
   Just add this line in your **conf/dependencies.yml** file:
<code>
<pre>
   - play -> cucumber 0.1 
</pre>
</code>

## Write your specification

   You must write your specifications in a [Business Readable, Domain Specific Language](http://martinfowler.com/bliki/BusinessReadableDSL.html). Cucumber supports the [Gherkin](https://github.com/cucumber/cucumber/wiki/Gherkin) syntax.

   You must store your .feature files in the **features** directory and write your Glue code in **test directory**. You can see an example here: [https://github.com/jeromebenois/play-cucumber-sample](https://github.com/jeromebenois/play-cucumber-sample)

## Getting Started in Web browser

Go to [http://localhost:9000/@cukes](http://localhost:9000/@cukes) to run the cucumber tests.
 
## Getting Started in command line

Run this command:
<code>
<pre>
    play cukes
</pre>
</code>



<center>Have Fun with play-cucumber!</center>