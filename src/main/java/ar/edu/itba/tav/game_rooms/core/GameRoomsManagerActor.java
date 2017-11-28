package ar.edu.itba.tav.game_rooms.core;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.immutable.Stream;
import scala.compat.java8.ScalaStreamSupport;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

//import ar.edu.itba.tav.game_rooms.exceptions.NoSuchGameRoomException;

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
     * A {@link Map} of {@link ActorRef} holding as keys those actors whose termination process was triggered.
     * This set allows this {@link Actor} to know which children started their termination process,
     * in order to handle their post termination process (i.e execution of {@link #removeGameRoom(ActorRef)}).
     * The values are the {@link ActorRef} that requested the shutdown of the game room.
     */
    private final Map<ActorRef, ActorRef> terminatedActors;

    /**
     * Private constructor.
     */
    private GameRoomsManagerActor() {
        terminatedActors = new HashMap<>();
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(GetAllGameRoomsMessage.class, msg -> this.getAllGameRooms())
                .match(CreateGameRoomMessage.class, msg -> this.startGameRoom(msg.getGameRoomName()))
                .match(RemoveGameRoomMessage.class, msg -> this.stopGameRoom(msg.getGameRoomName()))
                .match(Terminated.class,
                        terminated -> this.terminatedActors.containsKey(terminated.getActor()),
                        terminated -> this.removeGameRoom(terminated.getActor()))
                .build();
    }

    /**
     * Replies with all the existing game rooms (i.e the children name).
     */
    private void getAllGameRooms() {
        final ActorRef requester = this.getSender();
        final List<String> gameRooms = ScalaStreamSupport.stream(this.context().children())
                .map(ActorRef::path)
                .map(ActorPath::name)
                .map(name -> {
                    try {
                        return URLDecoder.decode(name, UTF8_ENCODING);
                    } catch (UnsupportedEncodingException e) {
                        // This can't happen, but we must catch this exception.
                        LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
                        LOGGER.debug("Stacktrace: ", e);
                        throw new RuntimeException("Could not url decode game room name", e);
                    }
                })
                .collect(Collectors.toList());
        requester.tell(gameRooms, this.getSelf());
    }

    /**
     * Starts a new game room with the given {@code gameRoomName} (i.e starts a new {@link GameRoomActor}).
     *
     * @param gameRoomName The name for the new game room.
     */
    private void startGameRoom(String gameRoomName) {
        Objects.requireNonNull(gameRoomName, "The gameRoomName must not be null!");
        final ActorRef requester = this.getSender();
        try {
            LOGGER.debug("Trying to create a new game room with name {}", gameRoomName);
            final String urlEncodedName = URLEncoder.encode(gameRoomName, UTF8_ENCODING);
            final ActorRef actorRef = this.getContext().actorOf(GameRoomActor.props(gameRoomName), urlEncodedName);
            this.getContext().watch(actorRef);  // Monitor child life
            LOGGER.debug("Game room with name \"{}\" successfully created. Name is url encoded as \"{}\"",
                    gameRoomName, urlEncodedName);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
            LOGGER.debug("Stacktrace: ", e);
            reportGameRoomCreationFailure(requester);
            throw new RuntimeException("Could not url encode game room name", e);
        } catch (InvalidActorNameException e) {
            LOGGER.debug("Name \"{}\" is invalid, or already used", gameRoomName);
            reportGameRoomNameRepeated(requester);
            return;
        }
        reportGameRoomCreationSuccessToSender(requester);
    }

    /**
     * Stops a game room with the given {@code gameRoomName}
     * (i.e starts the termination process of the {@link GameRoomActor}
     * representing the game room with the given {@code gameRoomName}).
     *
     * @param gameRoomName The name of the game room to be stopped.
     */
    private void stopGameRoom(String gameRoomName) {
        Objects.requireNonNull(gameRoomName, "The gameRoomName must not be null!");
        LOGGER.debug("Trying to Stop game room with name \"{}\"", gameRoomName);
        final ActorRef requester = this.getSender();
        try {
            final String urlEncodedName = URLEncoder.encode(gameRoomName, UTF8_ENCODING);
            final Optional<ActorRef> actorRefOptional = this.getContext().findChild(urlEncodedName);

            if (!actorRefOptional.isPresent()) {
                LOGGER.debug("No game room with name \"{}\"", gameRoomName);
                reportGameRoomNotExists(requester);
                return;
            }
            final ActorRef actorRef = actorRefOptional.get();
            this.getContext().stop(actorRef);
            this.terminatedActors.put(actorRef, requester);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
            LOGGER.debug("Stacktrace: ", e);
            reportGameRoomRemovalFailure(requester);
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
        final ActorRef requester = terminatedActors.get(terminatedActorRef);
        try {
            final String gameRoomName = URLDecoder.decode(terminatedActorRef.path().name(), UTF8_ENCODING);
            LOGGER.debug("Successfully stopped game with name \"{}\"", gameRoomName);
            LOGGER.debug("Name \"{}\" is again available for a game room", gameRoomName);
            reportGameRoomRemovalSuccess(requester);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
            LOGGER.debug("Stacktrace: ", e);
            reportGameRoomRemovalFailure(requester);
            throw new RuntimeException("Could not url decode game room name", e);
        }
    }

    // TODO: join a game room (game room id + player id?)

    /**
     * Reports success to the sender (i.e send to it a {@code true} value).
     */
    private void reportGameRoomCreationSuccessToSender(ActorRef requester) {
        reportToActor(requester, GameRoomCreationResult.CREATED);
    }

    /**
     * Reports failure to the sender (i.e send to it a {@code false} value).
     */
    private void reportGameRoomNameRepeated(ActorRef requester) {
        reportToActor(requester, GameRoomCreationResult.NAME_REPEATED);
    }

    /**
     * Reports failure to the sender (i.e send to it a {@code false} value).
     */
    private void reportGameRoomCreationFailure(ActorRef requester) {
        reportToActor(requester, GameRoomCreationResult.FAILURE);
    }

    /**
     * Reports success to the sender (i.e send to it a {@code true} value).
     */
    private void reportGameRoomRemovalSuccess(ActorRef requester) {
        reportToActor(requester, GameRoomRemovalResult.REMOVED);
    }

    /**
     * Reports success to the sender (i.e send to it a {@code true} value).
     */
    private void reportGameRoomNotExists(ActorRef requester) {
        reportToActor(requester, GameRoomRemovalResult.NO_SUCH_GAME_ROOM);
    }

    /**
     * Reports success to the sender (i.e send to it a {@code true} value).
     */
    private void reportGameRoomRemovalFailure(ActorRef requester) {
        reportToActor(requester, GameRoomRemovalResult.FAILURE);
    }

    /**
     * Replies the given {@link ActorRef} with the given {@code result} value.
     *
     * @param actorRef The actor to which the result must be sent.
     * @param result   The value to send as a reply to the sender.
     */
    private <T extends ResultEnum> void reportToActor(ActorRef actorRef, T result) {
        actorRef.tell(result, this.getSelf());
    }

    /**
     * Create {@link Props} for an {@link akka.actor.Actor} of this type.
     *
     * @return The created {@link Props}.
     */
    public static Props getProps() {
        return Props.create(GameRoomsManagerActor.class, GameRoomsManagerActor::new);
    }


    /**
     * A {@link GameRoomMessage} that is used to create a new game room.
     */
    public final static class GetAllGameRoomsMessage {

        /**
         * Private constructor.
         */
        private GetAllGameRoomsMessage() {
        }

        /**
         * Static method to create a {@link GetAllGameRoomsMessage}.
         *
         * @return The new {@link GetAllGameRoomsMessage}.
         */
        public static GetAllGameRoomsMessage getMessage() {
            return new GetAllGameRoomsMessage();
        }
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

    /**
     * Marking interface for enums that can be returned as responses to senders.
     */
    private interface ResultEnum {
    }

    /**
     * Enum containing the possible values to be replied to the sender when creating a new game room.
     */
    public enum GameRoomCreationResult implements ResultEnum {
        /**
         * The game room was created successfully.
         */
        CREATED,
        /**
         * There is another game room with the given name.
         */
        NAME_REPEATED,
        /**
         * There was an unexpected failure when trying to create the game room.
         */
        FAILURE,
    }

    /**
     * Enum containing the possible values to be replied to the sender when creating a new game room.
     */
    public enum GameRoomRemovalResult implements ResultEnum {
        /**
         * The game room was removed successfully.
         */
        REMOVED,
        /**
         * There is no game room with the given name.
         */
        NO_SUCH_GAME_ROOM,
        /**
         * There was an unexpected failure when trying to create the game room.
         */
        FAILURE,
    }
}
