package com.example.crime_job;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class CrimeJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrimeJobApplication.class, args);
    }
}

@Configuration
class IngestJobConfiguration {

    @Bean
    TaskExecutor taskExecutor() {
        return new VirtualThreadTaskExecutor();
    }

    @Bean
    Job ingestJob(JobRepository repository,
                  ResetDbStepConfiguration resetDbStepConfiguration,
                  LoadCsvStepConfiguration s1,
                  SummarizationStepConfiguration s2) {
        return new JobBuilder("ingestJob", repository)
                .start(resetDbStepConfiguration.resetDbStep(null, null, null))
                .next(s1.loadCsvStep(null, null, null))
                .next(s2.summarizationStep(null, null, null, null))
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @EventListener
    void jobExecuted(JobExecutionEvent event) {
        var jobExecution = event.getJobExecution();
        var startTime = jobExecution.getCreateTime();
        var endTime = jobExecution.getEndTime();
        IO.println("started the batch job @" + startTime + " and finished @" + endTime +
                " and job execution duration: " + Duration.between(startTime, endTime).toMillis());
    }
}

@Configuration
class ResetDbStepConfiguration {

    @Bean
    Step resetDbStep(JdbcClient db, PlatformTransactionManager tx, JobRepository jobRepository) {
        return new StepBuilder("resetDbStep", jobRepository)
                .tasklet((_, _) -> {
                    db.sql("delete from crime_data ").update();
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }
}

@Configuration
@ImportRuntimeHints(LoadCsvStepConfiguration.Hints.class)
class LoadCsvStepConfiguration {

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection().registerType(Crime.class, MemberCategory.values());
            hints.resources().registerResource(DATA_CSV_RESOURCE);
        }
    }

    static final Resource DATA_CSV_RESOURCE = new ClassPathResource("data.csv");

    @Bean
    JdbcBatchItemWriter<Crime> crimesCsvItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Crime>()
                .sql("""
                        insert into crime_data (
                            district, year, month, fbi_code
                        )
                        values (
                            ?, ?, ?, ?
                        )
                        """)
                .dataSource(dataSource)
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setInt(1, item.district());
                    ps.setInt(2, item.year());
                    ps.setInt(3, item.month());
                    ps.setString(4, item.fbiCode());
                })
                .build();
    }

    @Bean
    FlatFileItemReader<Crime> crimeCsvFlatFileItemReader() {
        return new FlatFileItemReaderBuilder<Crime>()
                .name("crimeCsvFlatFileItemReader")
                .resource(DATA_CSV_RESOURCE)
                .linesToSkip(1)
                .fieldSetMapper(fieldSet -> new Crime(fieldSet.readInt("ARR_DISTRICT", -1),
                        fieldSet.readInt("ARR_YEAR", -1), fieldSet.readInt("ARR_MONTH", -1), fieldSet.readString("FBI_CODE")))
                .delimited().names("ARR_DISTRICT,ARR_BEAT,ARR_YEAR,ARR_MONTH,RACE_CODE_CD,FBI_CODE,STATUTE,STAT_DESCR,CHARGE_CLASS_CD,CHARGE_TYPE_CD".split(","))
                .build();
    }

    @Bean
    Step loadCsvStep(TaskExecutor taskExecutor, PlatformTransactionManager tx, JobRepository jobRepository) {
        return new StepBuilder("csvToTableStep", jobRepository)
                .<Crime, Crime>chunk(1000, tx)
                .reader(this.crimeCsvFlatFileItemReader())
                .writer(this.crimesCsvItemWriter(null))
                .taskExecutor(taskExecutor)
                .build();
    }
}


@Configuration
class SummarizationStepConfiguration {

    record CrimeBreakdown(int district, Map<Integer, Integer> yearToCrimes) {
    }

    @Component
    static class CrimeBreakdownRowMapper
            implements RowMapper<CrimeBreakdown> {

        private final Map<Integer, Map<Integer, Integer>> map = new ConcurrentHashMap<>();

        @Override
        public CrimeBreakdown mapRow(ResultSet rs, int rowNum) throws SQLException {
            var district = rs.getInt("district");
            var cityToStat = this.map.computeIfAbsent(district, _ -> new ConcurrentHashMap<>());
            cityToStat.put(rs.getInt("year"), rs.getInt("total_crimes"));
            var cb = new CrimeBreakdown(district, map.get(district));
            IO.println(cb);
            return cb;
        }
    }

    @Bean
    ItemWriter<CrimeBreakdown> crimeBreakdownItemWriter() {
        return chunk -> chunk.forEach(IO::println);
    }

    @Bean
    JdbcCursorItemReader<CrimeBreakdown> crimeBreakdownJdbcBatchItemWriter(
            CrimeBreakdownRowMapper rowMapper,
            DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<CrimeBreakdown>()
                .name("crimeBreakdownJdbcBatchItemWriter")
                .dataSource(dataSource)
                .sql("select * from crime_breakdown")
                .rowMapper(rowMapper)
                .build();
    }

    @Bean
    Step summarizationStep(JobRepository jobRepository,
                           JdbcCursorItemReader<CrimeBreakdown> crimeBreakdownJdbcCursorItemReader,
                           ItemWriter<CrimeBreakdown> crimeBreakdownItemWriter,
                           PlatformTransactionManager tx) {
        return new StepBuilder("summarizationStep", jobRepository)
                .<CrimeBreakdown, CrimeBreakdown>chunk(1000, tx)
                .reader(crimeBreakdownJdbcCursorItemReader)
                .writer(crimeBreakdownItemWriter)
                .build();
    }

}

record Crime(int district, int year, int month, String fbiCode) {
}