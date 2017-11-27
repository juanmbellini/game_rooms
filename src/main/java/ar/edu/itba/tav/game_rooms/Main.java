package ar.edu.itba.tav.game_rooms;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor;
import ar.edu.itba.tav.game_rooms.http.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point.
 */
public class Main {

    /**
     * The {@link Logger} object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        LOGGER.info("Starting application...");

        final ActorSystem system = ActorSystem.create("game_rooms");
        final ActorRef gameRoomsManagerRef = system
                .actorOf(GameRoomsManagerActor.getProps(), "game_rooms_manager");

        HttpServer.createServer(system, gameRoomsManagerRef.path()).start(9000);
    }
}
