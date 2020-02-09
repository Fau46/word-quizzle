#!/bin/bash

# mvn clean
mvn compile
mvn install 
cd Server
mvn exec:java 
