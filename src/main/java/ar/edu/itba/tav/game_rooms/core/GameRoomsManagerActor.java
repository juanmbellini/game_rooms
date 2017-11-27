package ar.edu.itba.tav.game_rooms.core;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import ar.edu.itba.tav.game_rooms.exceptions.NameAlreadyInUseException;
import ar.edu.itba.tav.game_rooms.exceptions.NoSuchGameRoomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * {@link akka.actor.Actor} in charge of managing game rooms.
 */
public class GameRoomsManagerActor extends AbstractActor {

    /**
     * The {@link Logger} object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GameRoomsManagerActor.class);

    /**
     * UTF-8 Encoding.
     */
    private static final String UTF8_ENCODING = "UTF-8";

    /**
     * A {@link Set} of {@link ActorRef} holding those actors whose termination process was triggered.
     * This set allows this {@link Actor} to know which children started their termination process,
     * in order to handle their post termination process (i.e execution of {@link #removeGameRoom(ActorRef)}).
     */
    private final Set<ActorRef> terminatedActors;

    /**
     * Private constructor.
     */
    private GameRoomsManagerActor() {
        terminatedActors = new HashSet<>();
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(CreateGameRoomMessage.class, msg -> this.startGameRoom(msg.getGameRoomName()))
                .match(RemoveGameRoomMessage.class, msg -> this.stopGameRoom(msg.getGameRoomName()))
                .match(Terminated.class,
                        terminated -> this.terminatedActors.contains(terminated.getActor()),
                        terminated -> this.removeGameRoom(terminated.getActor()))
                .build();
    }

    /**
     * Starts a new game room with the given {@code gameRoomName} (i.e starts a new {@link GameRoomActor}).
     *
     * @param gameRoomName The name for the new game room.
     * @throws NameAlreadyInUseException If the given name is already in use
     *                                   (i.e there is another game room with the given name).
     */
    private void startGameRoom(String gameRoomName) throws NameAlreadyInUseException {
        Objects.requireNonNull(gameRoomName, "The gameRoomName must not be null!");
        try {
            LOGGER.info("Trying to create a new game room with name {}", gameRoomName);
            final String urlEncodedName = URLEncoder.encode(gameRoomName, UTF8_ENCODING);
            final ActorRef actorRef = this.getContext().actorOf(GameRoomActor.props(gameRoomName), urlEncodedName);
            this.getContext().watch(actorRef);  // Monitor child life
            LOGGER.info("Game room with name \"{}\" successfully created. Name is url encoded as \"{}\"",
                    gameRoomName, urlEncodedName);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
            LOGGER.debug("Stacktrace: ", e);
            throw new RuntimeException("Could not url encode game room name", e);
        } catch (InvalidActorNameException e) {
            LOGGER.debug("Name \"{}\" is invalid, or already used", gameRoomName);
            throw new NameAlreadyInUseException("There is another game room with the name \"" + gameRoomName + "\".");
        }
    }

    /**
     * Stops a game room with the given {@code gameRoomName}
     * (i.e starts the termination process of the {@link GameRoomActor}
     * representing the game room with the given {@code gameRoomName}).
     *
     * @param gameRoomName The name of the game room to be stopped.
     * @throws NoSuchGameRoomException If there is no game room with the given {@code gameRoomName} to be stopped.
     */
    private void stopGameRoom(String gameRoomName) throws NoSuchGameRoomException {
        Objects.requireNonNull(gameRoomName, "The gameRoomName must not be null!");
        LOGGER.info("Trying to Stop game room with name \"{}\"", gameRoomName);
        try {
            final String urlEncodedName = URLEncoder.encode(gameRoomName, UTF8_ENCODING);
            final ActorRef actorRef = this.getContext().findChild(urlEncodedName)
                    .orElseThrow(() -> {
                        LOGGER.debug("No game room with name \"{}\"", gameRoomName);
                        return new NoSuchGameRoomException("There is no game room with the name " + gameRoomName);
                    });
            this.getContext().stop(actorRef);
            this.terminatedActors.add(actorRef);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
            LOGGER.debug("Stacktrace: ", e);
            throw new RuntimeException("Could not url encode game room name", e);
        }
    }

    /**
     * Removes a game room from the system (i.e removes the used name of the game room).
     *
     * @param terminatedActorRef The {@link ActorRef} of a terminated {@link GameRoomActor}.
     */
    private void removeGameRoom(ActorRef terminatedActorRef) {
        Objects.requireNonNull(terminatedActorRef, "The terminatedActorRef must not be null!");
        try {
            final String gameRoomName = URLDecoder.decode(terminatedActorRef.path().name(), UTF8_ENCODING);
            LOGGER.info("Successfully stopped game with name \"{}\"", gameRoomName);
            LOGGER.info("Name \"{}\" is again available for a game room", gameRoomName);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
            LOGGER.debug("Stacktrace: ", e);
            throw new RuntimeException("Could not url decode game room name", e);
        }
    }

    // TODO: join a game room (game room id + player id?)


    /**
     * Create {@link Props} for an {@link akka.actor.Actor} of this type.
     *
     * @return The created {@link Props}.
     */
    public static Props getProps() {
        return Props.create(GameRoomsManagerActor.class, GameRoomsManagerActor::new);
    }

    /**
     * Abstract class representing a game room message (i.e a message that a {@link GameRoomsManagerActor}
     * can understand in order to operate over a game room.
     */
    private abstract static class GameRoomMessage {

        /**
         * The game room's name in which the operation must be done.
         */
        private final String gameRoomName;

        /**
         * Private constructor.
         *
         * @param gameRoomName The game room's name in which the operation must be done.
         */
        private GameRoomMessage(String gameRoomName) {
            this.gameRoomName = gameRoomName;
        }

        /**
         * @return The game room's name in which the operation must be done.
         */
        /* package */ String getGameRoomName() {
            return gameRoomName;
        }
    }

    /**
     * A {@link GameRoomMessage} that is used to create a new game room.
     */
    public final static class CreateGameRoomMessage extends GameRoomMessage {

        /**
         * Private constructor.
         *
         * @param gameRoomName The name for the new game room.
         */
        private CreateGameRoomMessage(String gameRoomName) {
            super(gameRoomName);
        }

        /**
         * Static method to create a {@link CreateGameRoomMessage}.
         *
         * @param gameRoomName The name for the new game room.
         * @return The new {@link CreateGameRoomMessage}.
         */
        public static CreateGameRoomMessage getMessage(String gameRoomName) {
            return new CreateGameRoomMessage(gameRoomName);
        }
    }

    /**
     * A {@link GameRoomMessage} that is used remove game rooms.
     */
    public final static class RemoveGameRoomMessage extends GameRoomMessage {

        /**
         * Private constructor.
         *
         * @param gameRoomName The name of the game room to be removed.
         */
        private RemoveGameRoomMessage(String gameRoomName) {
            super(gameRoomName);
        }

        /**
         * Static method to create a {@link RemoveGameRoomMessage}.
         *
         * @param gameRoomName The name of the game room to be removed.
         * @return The new {@link RemoveGameRoomMessage}.
         */
        public static RemoveGameRoomMessage getMessage(String gameRoomName) {
            return new RemoveGameRoomMessage(gameRoomName);
        }
    }
}
