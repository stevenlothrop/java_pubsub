package com.stevenlothrop.concurrency;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

public class SubscribeLastExample {
    public static class PubSubChannels {
        public static final PubSubChannel<Boolean> BOOLEAN_PUB_SUB_CHANNEL = new PubSubChannel<>();
    }

    @Test
    public void simpleTest() throws InterruptedException {
        PubSubThreads pubSubThreads = new PubSubThreads();
        WrappedCountdownLatch wrappedCountdownLatch = new WrappedCountdownLatch(1);

        // Publish
        {
            PubSubThread pubSubThread = pubSubThreads.createAndSoutExceptions();
            pubSubThread.execute(() -> PubSubChannels.BOOLEAN_PUB_SUB_CHANNEL.publish(Arrays.asList(Boolean.FALSE, Boolean.TRUE)));
        }

        // Subscribe
        {
            PubSubThread pubSubThread = pubSubThreads.createAndSoutExceptions();
            pubSubThread.subscribeLast(PubSubChannels.BOOLEAN_PUB_SUB_CHANNEL, wrappedCountdownLatch.on());
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

        public Subscriber<Boolean> on() {
            return aBoolean -> {
                if (aBoolean) {
                    c.countDown();
                } else{
                    assertTrue(false);
                }
            };
        }
    }
}
