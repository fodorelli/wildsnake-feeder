package tech.allegro.io.twitter.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import tech.allegro.io.twitter.config.TwitterAccessProperties;
import tech.allegro.io.twitter.domain.Twitt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TwitterStreamItemReader implements ItemStreamReader<Twitt> {
    private static final Logger log = LoggerFactory.getLogger(TwitterStreamItemReader.class);
    private BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);
    private Client client;
    private final TwitterAccessProperties accessProperties;

    @Autowired
    public TwitterStreamItemReader(TwitterAccessProperties accessProperties) {
        this.accessProperties = accessProperties;
    }

    @Override
    public Twitt read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        ObjectMapper objectMapper = new ObjectMapper();
        Twitt twitt = objectMapper.readValue(queue.take(), Twitt.class);
        log.info("RECEIVED TWITT: " + twitt);
        return twitt;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        endpoint.trackTerms(Lists.newArrayList("snake USD"));
        Authentication auth = new OAuth1(accessProperties.getConsumerKey(),
                accessProperties.getConsumerSecret(),
                accessProperties.getAccessToken(),
                accessProperties.getAccessTokenSecret());

        client = new ClientBuilder()
                .hosts(Constants.STREAM_HOST)
                .endpoint(endpoint)
                .authentication(auth)
                .processor(new StringDelimitedProcessor(queue))
                .build();

        client.connect();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
        client.stop();
    }
}
