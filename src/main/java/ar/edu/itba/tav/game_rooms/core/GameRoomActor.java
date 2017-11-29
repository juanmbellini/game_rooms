package ar.edu.itba.tav.game_rooms.core;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.GameRoomDataMessage;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.GetGameRoomDataMessage;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link akka.actor.Actor} in charge of representing a game room as an actor in the platform.
 */
public class GameRoomActor extends AbstractActor {

    /**
     * The game room's name.
     */
    private final String gameRoomName;

    /**
     * The game room's capacity.
     */
    private final int capacity;

    /**
     * The players in this game room.
     */
    private final Set<String> players;


    /**
     * Constructor.
     *
     * @param gameRoomName The game room's name.
     * @param capacity     The game room's capacity.
     */
    private GameRoomActor(String gameRoomName, int capacity) {
        this.gameRoomName = gameRoomName;
        this.capacity = capacity;
        this.players = new HashSet<>();
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(GetGameRoomDataMessage.class, msg -> this.reportData())
                .build();
    }

    /**
     * Sends this game room data to the sender.
     */
    private void reportData() {
        getSender().tell(new GameRoomDataMessage(gameRoomName, capacity, players), this.getSelf());
    }

    /**
     * Create {@link Props} for an {@link akka.actor.Actor} of this type.
     *
     * @param gameRoomName The name of the game room the {@link GameRoomActor} will contain.
     * @return The created {@link Props}.
     * @throws IllegalArgumentException If there are invalid arguments.
     */
    /* package */
    static Props props(String gameRoomName, int capacity) throws IllegalArgumentException {
        if (gameRoomName == null || capacity <= 0) {
            throw new IllegalArgumentException("Wrong params");
        }
        return Props.create(GameRoomActor.class, () -> new GameRoomActor(gameRoomName, capacity));
    }
}
