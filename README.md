# Bootiful Data Processing 


code for a Spring Batch and distributed data processing talk. 

## spring batch
- will look at Spring Batch, first, but also look at projects like Spring Integration, JobRunr, GraalVM, and Flowable  
- Spring Batch 
 - jobs 
 - step scopes 
 - repositories
 - step flows
 - batch partitioning/chunking? here's my [batch-spring-boot-starter: provides auto configuration for distributed computing idioms with Spring Batch](https://github.com/joshlong/batch-spring-boot-starter)


here's what we could use as a demo: [the NYC taxi trips database](https://www1.nyc.gov/site/tlc/about/tlc-trip-record-data.page)

	•	Open data, CSV, parquet, millions of rows
	•	Super intuitive schema: pickup time, dropoff, trip distance, fare, tip, payment type
	•	Easy insights:
	•	Average fare per borough
	•	Surge trends by hour
	•	Tip percentage distribution
	•	Longest trips
	•	Partitionable by: month, day, or even borough





## graalvm 
* running a spring batch job as a graalvm native image makes a _ton_ of sense if ur using something like autosys or bmc or whatever and wantt he app to start up ane be self-contained

## jobrunr
* can run the batch jobs in response to requests 
* run-only-once 
* show how that can play nicely with the job parameters mechanism 


## spring integration
* show how u can use channels to coordinate between nodes eg, via filesystems or rabbitmq 

## remote chunking 
i buitl a starter that automates away a lot of this. time to update it, convert the build into `pom.xml`, and maybe publish to maven central like i have some of the other projects thanks to andres' help. 

