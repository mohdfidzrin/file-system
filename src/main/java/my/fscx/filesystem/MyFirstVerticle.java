package my.fscx.filesystem;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.vertx.core.http.HttpMethod.GET;

public class MyFirstVerticle extends AbstractVerticle {
    private String filePath = "C:/Users/Work/java-workspace/vertx-workspace/filesystem/uploads/c0bda282-88bb-4ee8-9837-91ca5e66ffcc";
    private Map<String, JsonObject> files = new HashMap<>();
    @Override
    public void start(Future<Void> future) {
        Router router = Router.router(vertx);
        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("accept");
        allowedHeaders.add("X-PINGARUNER");

        Set<HttpMethod> allowedMethods = new HashSet<>();
        allowedMethods.add(HttpMethod.GET);
        allowedMethods.add(HttpMethod.POST);
        allowedMethods.add(HttpMethod.OPTIONS);
        /*
         * these methods aren't necessary for this sample,
         * but you may need them for your projects
         */
        allowedMethods.add(HttpMethod.DELETE);
        allowedMethods.add(HttpMethod.PATCH);
        allowedMethods.add(HttpMethod.PUT);

        router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
        // Enable multipart form data parsing
        router.route().handler(BodyHandler.create().setUploadsDirectory("uploads"));

        // handle the form
        router.post("/upload").handler(ctx -> {
            ctx.response().putHeader("Content-Type", "application/json");
            ctx.response().setChunked(true);
            JsonArray arr = new JsonArray();
            for (FileUpload f : ctx.fileUploads()) {
                arr.add(new JsonObject()
                        .put("id", f.uploadedFileName().split("\\\\")[1])
                        .put("name", f.fileName())
                        .put("type",f.contentType())
                        .put("path",f.uploadedFileName())
                );
            }

            ctx.response().end(arr.encodePrettily());
        });

        router.route(GET,"/download").handler(ctx -> {
           vertx
                .fileSystem()
                .open(new File("").getAbsolutePath().concat("\\").concat(ctx.request().getParam("path")), new OpenOptions(), readEvent -> {
                    if(readEvent.failed()) {
                        ctx.response().setStatusCode(500).end();
                        return;
                    }

                    AsyncFile asyncFile = readEvent.result();
                    ctx.response().setChunked(true);
                    ctx.response().putHeader("Content-Disposition", "attachment; filename="+ctx.request().getParam("name"));
                    Pump pump = Pump.pump(asyncFile, ctx.response());
                    pump.start();

                    asyncFile.endHandler(aVoid -> {
                        asyncFile.close();
                        ctx.response().end();
                    });
                });
        });

        // Bind "/" to our hello message - so we are still compatible.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Hello from my first Vert.x 3 application</h1>");
        });

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        config().getInteger("http.port",8080),
                        httpServerAsyncResult -> {
                            if(httpServerAsyncResult.succeeded()) {
                                future.complete();
                            } else {
                                future.fail(httpServerAsyncResult.cause());
                            }
                        }
                );
    }
}
