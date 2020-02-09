#!/bin/bash

mvn compile
mvn install 
cd Client
mvn exec:java 