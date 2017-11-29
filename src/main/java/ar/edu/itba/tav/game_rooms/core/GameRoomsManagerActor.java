package ar.edu.itba.tav.game_rooms.core;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.*;
import ar.edu.itba.tav.game_rooms.utils.AggregatorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;


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
     * A {@link Set} containing those {@link ActorRef} that represent game room {@link Actor},
     * children of this game room manager.
     */
    private final List<ActorRef> gameRoomActors;

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
        this.gameRoomActors = new LinkedList<>();
        this.terminatedActors = new HashMap<>();
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(GetAllGameRoomsMessage.class, msg -> this.getAllGameRooms())
                .match(CreateGameRoomMessage.class, this::startGameRoom)
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
        final GetGameRoomDataMessage request = GetGameRoomDataMessage.getMessage();
        final ActorRef respondTo = this.getContext()
                .actorOf(GetAllGameRoomsResponseHandler.getProps(this.getSelf(), this.getSender()));
        final long timeout = 2000;
        this.getContext()
                .actorOf(AggregatorActor.props(GameRoomDataMessage.class, request, gameRoomActors, respondTo, timeout));
    }

    /**
     * Starts a new game room with the given {@code gameRoomName} (i.e starts a new {@link GameRoomActor}).
     *
     * @param message the {@link CreateGameRoomMessage} containing data for game room creation.
     */
    private void startGameRoom(CreateGameRoomMessage message) {
        Objects.requireNonNull(message, "The message must not be null!");

        final String gameRoomName = message.getGameRoomName();
        final int capacity = message.getCapacity();

        final ActorRef requester = this.getSender();
        try {
            LOGGER.debug("Trying to create a new game room with name {}", gameRoomName);
            final String urlEncodedName = URLEncoder.encode(gameRoomName, UTF8_ENCODING);
            try {
                final ActorRef actorRef = this.getContext()
                        .actorOf(GameRoomActor.props(gameRoomName, capacity), urlEncodedName);
                this.getContext().watch(actorRef);  // Monitor child life
                this.gameRoomActors.add(actorRef);
            } catch (IllegalArgumentException e) {
                reportToActor(requester, GameRoomCreationResult.INVALID);
                return;
            }
            LOGGER.debug("Game room with name \"{}\" successfully created. Name is url encoded as \"{}\"",
                    gameRoomName, urlEncodedName);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
            LOGGER.debug("Stacktrace: ", e);
            reportToActor(requester, GameRoomCreationResult.FAILURE);
            throw new RuntimeException("Could not url encode game room name", e);
        } catch (InvalidActorNameException e) {
            LOGGER.debug("Name \"{}\" is invalid, or already used", gameRoomName);
            reportToActor(requester, GameRoomCreationResult.NAME_REPEATED);
            return;
        }
        reportToActor(requester, GameRoomCreationResult.CREATED);
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
                reportToActor(requester, GameRoomRemovalResult.NO_SUCH_GAME_ROOM);
                return;
            }
            final ActorRef actorRef = actorRefOptional.get();
            this.getContext().stop(actorRef);
            this.gameRoomActors.remove(actorRef);
            this.terminatedActors.put(actorRef, requester);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
            LOGGER.debug("Stacktrace: ", e);
            reportToActor(requester, GameRoomRemovalResult.FAILURE);
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
            reportToActor(requester, GameRoomRemovalResult.REMOVED);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
            LOGGER.debug("Stacktrace: ", e);
            reportToActor(requester, GameRoomRemovalResult.FAILURE);
            throw new RuntimeException("Could not url decode game room name", e);
        }
    }

    // TODO: join a game room (game room id + player id?)


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
     * Inner {@link akka.actor.Actor} in charge of handling a get all game rooms aggregation response.
     */
    private static final class GetAllGameRoomsResponseHandler extends AbstractActor {

        /**
         * {@link ActorRef} who must send the response.
         */
        private final ActorRef from;
        /**
         * {@link ActorRef} who must receive the response.
         */
        private final ActorRef respondTo;

        /**
         * Private constructor.
         *
         * @param from      {@link ActorRef} who must send the response.
         * @param respondTo {@link ActorRef} who must receive the response.
         */
        private GetAllGameRoomsResponseHandler(ActorRef from, ActorRef respondTo) {
            this.from = from;
            this.respondTo = respondTo;
        }

        @Override
        public Receive createReceive() {
            return ReceiveBuilder.create()
                    .match(AggregatorActor.SuccessfulResultMessage.class, this::handleSuccessfulResponse)
                    .match(AggregatorActor.TimeoutMessage.class, msg -> this.handleTimeout())
                    .match(AggregatorActor.FailMessage.class, this::handleFailure)
                    .build();
        }

        /**
         * Handles the successful aggregation response message.
         *
         * @param msg The {@link AggregatorActor.SuccessfulResultMessage}
         *            containing the aggregation result.
         */
        private void handleSuccessfulResponse(AggregatorActor.SuccessfulResultMessage<GameRoomDataMessage> msg) {
            respondTo.tell(new LinkedList<>(msg.getResult().values()), from);
        }

        /**
         * Handles aggregation process timeout (i.e do nothing, and expect the requester eventually timeouts by itself).
         */
        private void handleTimeout() {
            terminate();
            // Do nothing, as if there is a timeout, the requester will eventually timeout by itself
        }

        /**
         * Handles aggregation process failure (i.e TODO: complete)
         *
         * @param msg The {@link ar.edu.itba.tav.game_rooms.utils.AggregatorActor.FailMessage}
         *            containing the {@link Throwable} that caused the failure.
         */
        private void handleFailure(AggregatorActor.FailMessage msg) {
            // TODO: define what do we do here...
            terminate();
        }

        private void terminate() {
            this.getContext().stop(this.getSelf());
        }

        /**
         * Create {@link Props} for an {@link akka.actor.Actor} of this type.
         *
         * @param from      {@link ActorRef} who must send the response.
         * @param respondTo {@link ActorRef} who must receive the response.
         * @return The created {@link Props}.
         */
        private static Props getProps(ActorRef from, ActorRef respondTo) {
            return Props.create(GetAllGameRoomsResponseHandler.class,
                    () -> new GetAllGameRoomsResponseHandler(from, respondTo));
        }
    }
}
