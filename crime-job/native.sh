#!/usr/bin/env bash 
./mvn -DskipTests -Pnative native:compile 
./target/crime-job
