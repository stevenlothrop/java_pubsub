package com.stevenlothrop.concurrency;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

public class SubscribeSetExample {
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
            pubSubThread.execute(() -> PubSubChannels.BOOLEAN_PUB_SUB_CHANNEL.publish(Arrays.asList(Boolean.TRUE, Boolean.FALSE)));
        }

        // Subscribe
        {
            PubSubThread pubSubThread = pubSubThreads.createAndSoutExceptions();
            pubSubThread.subscribeSet(PubSubChannels.BOOLEAN_PUB_SUB_CHANNEL, wrappedCountdownLatch.on(), aBoolean -> aBoolean.toString());
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

        public SubscriberSet<Boolean> on() {
            return l -> {
                for (Boolean ignored : l) {
                    c.countDown();
                }
            };
        }
    }
}
