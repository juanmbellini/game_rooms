package ar.edu.itba.tav.game_rooms.core;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

/**
 * {@link akka.actor.Actor} in charge of representing a game room as an actor in the platform.
 */
public class GameRoomActor extends AbstractActor {

    /**
     * The game room's name.
     */
    private final String gameRoomName;

    // TODO: add capacity

    // TODO: add amount of players

    /**
     * Constructor.
     *
     * @param gameRoomName The game room's name.
     */
    private GameRoomActor(String gameRoomName) {
        this.gameRoomName = gameRoomName;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .build();
    }

    /**
     * Create {@link Props} for an {@link akka.actor.Actor} of this type.
     *
     * @param gameRoomName The name of the game room the {@link GameRoomActor} will contain.
     * @return The created {@link Props}.
     */
    /* package */
    static Props props(String gameRoomName) {
        return Props.create(GameRoomActor.class, () -> new GameRoomActor(gameRoomName));
    }
}
