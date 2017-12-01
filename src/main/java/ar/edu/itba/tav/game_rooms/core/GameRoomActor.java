package ar.edu.itba.tav.game_rooms.core;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.*;

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
    private final Set<Long> players;


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
                .match(AddPlayerMessage.class, msg -> this.addPlayer(msg.getPlayerId()))
                .match(RemovePlayerMessage.class, msg -> this.removePlayer(msg.getPlayerId()))
                .build();
    }

    /**
     * Sends this game room data to the sender.
     */
    private void reportData() {
        getSender().tell(new GameRoomDataMessage(gameRoomName, capacity, players), this.getSelf());
    }

    /**
     * Adds a player with the given {@code playerId} into the game room.
     *
     * @param playerId The id of the player being added.
     */
    private void addPlayer(long playerId) {
        if (this.players.size() == this.capacity && !this.players.contains(playerId)) {
            getSender().tell(PlayerOperationResult.FULL_GAME_ROOM, getSelf());
            return;
        }
        // In case players.size != capacity, and the set already contains the id,
        // The following call will not affect, as it can't hold repeated elements.
        // It will just ignore it.
        this.players.add(playerId);
        getSender().tell(PlayerOperationResult.SUCCESSFUL, getSelf());
    }

    /**
     * Removes a player with the given {@code playerId} from the game room.
     * This is an idempotent action.
     *
     * @param playerId The id of the player being removed.
     */
    private void removePlayer(long playerId) {
        this.players.remove(playerId); // Nothing happens if the set does not contain the id. This is idempotent.
        getSender().tell(PlayerOperationResult.SUCCESSFUL, getSelf());
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
