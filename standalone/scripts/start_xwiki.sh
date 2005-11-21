#!/bin/sh

export LANG=fr_FR.ISO8859-1
JETTY_HOME=.
JETTY_PORT=8080

java -Dfile.encoding=iso-8859-1 -Djetty.port=$JETTY_PORT -Djetty.home=$JETTY_HOME -jar $JETTY_HOME/start.jar

