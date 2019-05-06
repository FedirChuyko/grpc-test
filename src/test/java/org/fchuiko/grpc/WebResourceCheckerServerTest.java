package org.fchuiko.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class WebResourceCheckerServerTest {
    private final static int NUMBER_OF_THREADS = 3;
    private List<String> addressList = Arrays.asList(
            "http://google.com",
            "http://Youtube.com",
            "http://Facebook.com",
            "http://Baidu.com",
            "http://github.com",
            "http://Yahoo.com",
            "http://Reddit.com",
            "http://Google.co.in",
            "http://Qq.com",
            "http://Taobao.com",
            "http://Amazon.com",
            "http://Tmall.com",
            "http://stackoverflow.com",
            "http://Sohu.com",
            "http://Instagram.com",
            "http://Live.com",
            "http://Google.co.jp",
            "http://Weibo.com",
            "http://Sina.com.cn",
            "http://Jd.com");
    private WebResourceCheckerServer server;
    private ManagedChannel channel;

    private static final Logger logger = Logger.getLogger(WebResourceCheckerServerTest.class.getName());

    @BeforeEach
    public void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        server = new WebResourceCheckerServer(InProcessServerBuilder.forName(serverName));

        server.start();
        // Here should be a registration for automatic shutdown but JUnit 5 is not supported by grpc-testing,
        // thatswhy we use tearDown() method instead
        channel = InProcessChannelBuilder.forName(serverName).build();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        if (null != channel) {
            channel.shutdown();
            try {
                channel.awaitTermination(50, TimeUnit.SECONDS);
            } finally {
                channel.shutdownNow();
            }
        }
        if (null != server) {
            server.stop();
            server.blockUntilShutdown();
        }
    }

    @Test
    public void testBlockingSingleThread() {
        CheckerGrpc.CheckerBlockingStub blockingStub = CheckerGrpc.newBlockingStub(channel);
        for (String address : addressList) {
            CheckResult result = blockingStub.checkResource(WebResource.newBuilder().setAddress(address).build());
            System.out.println(address + " responseCode:" + result.getResponseCode() + " responseDelay:" + result.getResponseDelay());
            assertTrue(result.getResponseCode() > 0, "request failed");
        }
    }

    @Test
    public void testAsyncSingleThread() {
        CheckerGrpc.CheckerFutureStub futureStub = CheckerGrpc.newFutureStub(channel);
        CountDownLatch latch = new CountDownLatch(addressList.size());
        for (String address : addressList) {
            ListenableFuture<CheckResult> future = futureStub.checkResource(WebResource.newBuilder().setAddress(address).build());
            Futures.addCallback(future, new FutureCallback<CheckResult>() {
                @Override
                public void onSuccess(@Nullable CheckResult result) {
                    if (null!=result) {
                        System.out.println(address + " responseCode:" + result.getResponseCode() + " responseDelay:" + result.getResponseDelay());
                        assertTrue(result.getResponseCode() > 0, "request failed");
                    }
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable t) {
                    fail("Request failed :" + t.getMessage());
                }
            }, MoreExecutors.directExecutor());
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            fail("test Async Singlethread failed :" + e.getMessage());
        }

    }

    @Test
    public void testBlockingMultithread() {
        CheckerGrpc.CheckerBlockingStub blockingStub = CheckerGrpc.newBlockingStub(channel);
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>(addressList);

        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    String address;
                    while ((address = queue.poll()) != null) {
                        CheckResult result = blockingStub.checkResource(WebResource.newBuilder().setAddress(address).build());
                        System.out.println(address + " responseCode:" + result.getResponseCode() + " responseDelay:" + result.getResponseDelay());
                        assertTrue(result.getResponseCode() > 0, "request failed");
                    }
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            fail("test Blocking Multithread failed :" + e.getMessage());
        }
    }

    @Test
    public void testAsyncMultithread() {
        CheckerGrpc.CheckerFutureStub futureStub = CheckerGrpc.newFutureStub(channel);
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>(addressList);

        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        CountDownLatch latch = new CountDownLatch(queue.size());
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    String address;
                    while ((address = queue.poll()) != null) {
                        String add = address;
                        ListenableFuture<CheckResult> future = futureStub.checkResource(WebResource.newBuilder().setAddress(address).build());
                        Futures.addCallback(future, new FutureCallback<CheckResult>() {
                            @Override
                            public void onSuccess(@Nullable CheckResult result) {
                                if (null!=result) {
                                    System.out.println(add + " responseCode:" + result.getResponseCode() + " responseDelay:" + result.getResponseDelay());
                                    assertTrue(result.getResponseCode() > 0, "request failed");
                                }
                                latch.countDown();
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                fail("Request failed :" + t.getMessage());
                            }
                        }, MoreExecutors.directExecutor());
                    }
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            fail("test Async Multithread failed :" + e.getMessage());
        }

    }

}
