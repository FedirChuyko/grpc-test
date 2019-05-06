package org.fchuiko.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server that manages startup/shutdown of a server.
 */
public class WebResourceCheckerServer {
    private static final Logger logger = Logger.getLogger(WebResourceCheckerServer.class.getName());

    private Server server;

    public WebResourceCheckerServer(int port) {
        server = ServerBuilder.forPort(port).addService(new CheckerImpl())
                .build();
    }

    public WebResourceCheckerServer(ServerBuilder<?> serverBuilder) {
        server = serverBuilder.addService(new CheckerImpl())
                .build();
    }


    public void start() throws IOException {

        server.start();
        if (server.getPort() > 0) {
            logger.info("Server started, listening on " + server.getPort());
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                WebResourceCheckerServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final WebResourceCheckerServer server = new WebResourceCheckerServer(50051);
        server.start();
        server.blockUntilShutdown();
    }

    static class CheckerImpl extends CheckerGrpc.CheckerImplBase {
        @Override
        public void checkResource(WebResource resource, StreamObserver<CheckResult> responseObserver) {

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(resource.getAddress());
                long startTime = System.currentTimeMillis();
                HttpResponse response = client.execute(request);
                long elapsedTime = System.currentTimeMillis() - startTime;

                CheckResult reply = CheckResult.newBuilder().setResponseCode(response.getStatusLine().getStatusCode()).setResponseDelay(elapsedTime).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (IOException e) {
                logger.info("IOException: " + e.getMessage());
                responseObserver.onNext(CheckResult.newBuilder().setResponseCode(0).build());
                responseObserver.onCompleted();
            }
        }
    }
}
