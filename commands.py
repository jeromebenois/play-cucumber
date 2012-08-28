# Here you can create play commands that are specific to the module, and extend existing commands
import sys
import os
import subprocess
import shutil
import getopt
import urllib2
import webbrowser
import time
import signal

from play.utils import *

MODULE = 'cucumber'

# Commands that are specific to your module

COMMANDS = ['cucumber', 'cukes']
HELP = {
    'cucumber': "Automatically run all cucumber tests",
    'cukes': "Automatically run all cucumber tests"
}


def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == "cucumber" or command == 'cukes':
        cukes(app, args)

def cukes(app, args):
    app.check()
    print "~ Running in test mode"
    print "~ Ctrl+C to stop"
    print "~ "

    print "~ Deleting %s" % os.path.normpath(os.path.join(app.path, 'tmp'))
    if os.path.exists(os.path.join(app.path, 'tmp')):
        shutil.rmtree(os.path.join(app.path, 'tmp'))
    print "~"

    # Kill if exists
    http_port = 9000
    protocol = 'http'
    if app.readConf('https.port'):
        http_port = app.readConf('https.port')
        protocol = 'https'
    else:
        http_port = app.readConf('http.port')
    try:
        proxy_handler = urllib2.ProxyHandler({})
        opener = urllib2.build_opener(proxy_handler)        
        opener.open('http://localhost:%s/@kill' % http_port)
    except Exception, e:
        pass
      
    # Run app
    #test_result = os.path.join(app.path, 'test-result')
    #if os.path.exists(test_result):
    #    shutil.rmtree(test_result)
    sout = open(os.path.join(app.log_path(), 'system.out'), 'w')
    #sout = sys.stdout
    java_cmd = app.java_cmd(args)
    try:
        play_process = subprocess.Popen(java_cmd, env=os.environ, stdout=sout)        
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
    soutint = open(os.path.join(app.log_path(), 'system.out'), 'r')
    while True:
        if play_process.poll():
            print "~"
            print "~ Oops, application has not started?"
            print "~"
            sys.exit(-1)
        line = soutint.readline().strip()        
        if line:
            print line
            if line.find('@cukes') > -1:
                #soutint.close()
                break
  
    # Run Cucumber tests
    try:
        proxy_handler = urllib2.ProxyHandler({})
        opener = urllib2.build_opener(proxy_handler)
        print "~ Run Cukes: "+('http://localhost:%s/@cukes/run.cli' % http_port)        
        result = opener.open('http://localhost:%s/@cukes/run.cli' % http_port)
        print result.read()
    except Exception, e:
        print e        
        pass
        
    #time.sleep(1)
        
    # Kill if exists    
    try:
        proxy_handler = urllib2.ProxyHandler({})
        opener = urllib2.build_opener(proxy_handler)
        opener.open('%s://localhost:%s/@kill' % (protocol, http_port))
    except Exception, e:
        pass
  
    sys.exit(1)
        
# This will be executed before any command (new, run...)
def before(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")


# This will be executed after any command (new, run...)
def after(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == "new":
        pass
