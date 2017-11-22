package io.github.AndiA92.aws.lambda.restapi.websockets;

import io.github.AndiA92.aws.lambda.restapi.exceptions.ChannelNotFoundException;
import io.github.AndiA92.aws.lambda.restapi.websockets.WebSocketHandler;


import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
//
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

import static java.util.Arrays.stream;
import static spark.Spark.*;

@Slf4j
public class WebSocketBroadcaster {

    private static Map<String, WebSocketHandler> wsRegistry = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        stream(args)
                .forEach(arg -> {
                    log.info("Create web socket at route: " + arg);
                    WebSocketHandler handler = new WebSocketHandler();
                    webSocket(String.format("/%s", arg), handler);
                    wsRegistry.put(arg, handler);
                });

        init();

        post("/broadcast/:channel", (req, resp) -> {
            String route = req.params("channel");
            log.info("Message received from " + req.ip() + " on channel " + route);
            Optional.ofNullable(wsRegistry.get(route))
                    .orElseThrow(() -> new ChannelNotFoundException("Could not find handler for route: " + route))
                    .dispatch(req.body());

            return new JSONObject("{status: 'success'}");
        });

        exception(ChannelNotFoundException.class, (exception, request, response) -> {
            log.error("Bad request: ", exception);
            halt(HttpServletResponse.SC_BAD_REQUEST);
        });
    }
}
