package com.stevenlothrop.concurrency;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

public class SubscribeListExample {
    public static class PubSubChannels {
        public static final PubSubChannel<Boolean> BOOLEAN_PUB_SUB_CHANNEL = new PubSubChannel<>();
    }

    @Test
    public void simpleTest() throws InterruptedException {
        PubSubThreads pubSubThreads = new PubSubThreads();
        WrappedCountdownLatch wrappedCountdownLatch = new WrappedCountdownLatch(2);

        // Publish
        {
            PubSubThread pubSubThread = pubSubThreads.createAndSoutExceptions();
            pubSubThread.execute(() -> PubSubChannels.BOOLEAN_PUB_SUB_CHANNEL.publish(Arrays.asList(Boolean.TRUE, Boolean.TRUE)));
        }

        // Subscribe
        {
            PubSubThread pubSubThread = pubSubThreads.createAndSoutExceptions();
            pubSubThread.subscribeList(PubSubChannels.BOOLEAN_PUB_SUB_CHANNEL, wrappedCountdownLatch.on());
        }

        pubSubThreads.start();
        wrappedCountdownLatch.c.await();
        assertTrue(true);
    }

    public static class WrappedCountdownLatch {
        public CountDownLatch c;

        public WrappedCountdownLatch(int i) {
            this.c = new CountDownLatch(i);
        }

        public SubscriberList<Boolean> on() {
            return l -> {
                for (Boolean ignored : l) {
                    c.countDown();
                }
            };
        }
    }
}
