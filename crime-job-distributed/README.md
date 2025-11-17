# README

this will start simple and eventually turn into a larger effort 

## act 1.

a simple spring batch  `Job` that will demonstrate how to read in data from a .csv file into a SQL db table.

this will show: 

 * will show how to use a `Tasklet` to first reset the DB before the first `Step` kicks in
 * `Job`, `Step` creation. 
 * show using `@Configuration` classes to separate the individual stages of processing.
 * show using the job / step builders and how to use Spring's built-in support to initialize schema 
 
## act 2. 

let's make this work go faster. let's use concurrency to parallelize the work. 

## act 3.
when do we run this thing? we could do in one of two ways, and we'll show them both.

* spring integration, to listen for events 
* jobrunr, to run on a cluster at a fixed time, with durable tracking and so on

## act 4.
we want to generate a .pdf report for each district, but that takes time. let's use remote chunking so that the work could be divided across a number of worker nodes. we'll bring in my [easy-spring-batch-remotechunking](https://github.com/joshlong/easy-spring-batch-remotechunking) library. See [the examples here](https://github.com/joshlong/batch-spring-boot-starter).

