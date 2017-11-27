package ar.edu.itba.tav.game_rooms;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor;
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
        final ActorRef gameManagerActorRef = system.actorOf(GameRoomsManagerActor.getProps(), "game_rooms_manager");
        gameManagerActorRef
                .tell(GameRoomsManagerActor.CreateGameRoomMessage.getMessage("The game room"), ActorRef.noSender());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        gameManagerActorRef
                .tell(GameRoomsManagerActor.RemoveGameRoomMessage.getMessage("The game room"), ActorRef.noSender());
    }
}
