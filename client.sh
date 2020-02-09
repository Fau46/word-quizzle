#!/bin/bash

cd Client
for ((i=0; i<1; i++)) do
mvn exec:java &
# gnome-terminal -e Home/Documenti/GitHub/word-quizzle ./client.sh
done