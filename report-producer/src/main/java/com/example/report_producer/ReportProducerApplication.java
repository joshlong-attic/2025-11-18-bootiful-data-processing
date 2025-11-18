package com.example.report_producer;

import com.joshlong.batch.remotechunking.worker.WorkerInboundChunkChannel;
import com.joshlong.batch.remotechunking.worker.WorkerItemProcessor;
import com.joshlong.batch.remotechunking.worker.WorkerItemWriter;
import com.joshlong.batch.remotechunking.worker.WorkerOutboundChunkChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

@SpringBootApplication
public class ReportProducerApplication {

    public static void main(String[] args) {
        System.setProperty("spring.amqp.deserialization.trust.all", "true");
        SpringApplication.run(ReportProducerApplication.class, args);
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Bean
    @WorkerItemProcessor
    ItemProcessor<Object, Object> itemProcessor() {
        return item -> item;
    }

    @Bean
    @WorkerItemWriter
    ItemWriter<Object> itemWriter() {
        return chunk -> {

            this.log.info("doing the long-running writing thing");
            var items = chunk.getItems();
//            this.log.info("writing {} items", items.size());
            for (var i : items)
                this.log.info("item={}", i + "");
        };
    }

    @Bean
    IntegrationFlow inboundAmqpIntegrationFlow(@WorkerInboundChunkChannel MessageChannel workerRequestsMessageChannel,
                                               ConnectionFactory connectionFactory) {
        return IntegrationFlow//
                .from(Amqp.inboundAdapter(connectionFactory, "requests"))//
                .channel(workerRequestsMessageChannel)//
                .get();
    }

    @Bean
    IntegrationFlow outboundAmqpIntegrationFlow(
            @WorkerOutboundChunkChannel MessageChannel workerRepliesMessageChannel,
            AmqpTemplate template) {
        return IntegrationFlow //
                .from(workerRepliesMessageChannel)//
                .handle(Amqp.outboundAdapter(template).routingKey("replies"))//
                .get();
    }

}
