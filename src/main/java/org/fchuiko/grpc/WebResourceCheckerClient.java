package org.fchuiko.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//This is just a small test client. Main tests are in WebResourceCheckerServerTest
public class WebResourceCheckerClient {
  private static final Logger logger = Logger.getLogger(WebResourceCheckerClient.class.getName());

  private final ManagedChannel channel;
  private final CheckerGrpc.CheckerBlockingStub blockingStub;

  public WebResourceCheckerClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .build());
  }

  WebResourceCheckerClient(ManagedChannel channel) {
    this.channel = channel;
    blockingStub = CheckerGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }


  public void checkResource(String address) {
    logger.info("Requesting  " + address + " ...");
    WebResource request = WebResource.newBuilder().setAddress(address).build();
    CheckResult response;
    try {
      response = blockingStub.checkResource(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Response code: " + response.getResponseCode());
    logger.info("Response delay (in milliseconds): " + response.getResponseDelay());
  }


  public static void main(String[] args) throws Exception {
    WebResourceCheckerClient client = new WebResourceCheckerClient("localhost", 50051);
    try {
      String resource = "http://google.com";
      if (args.length > 0) {
        resource = args[0];
      }
      client.checkResource(resource);
    } finally {
      client.shutdown();
    }
  }
}
