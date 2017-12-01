package ar.edu.itba.tav.game_rooms.core;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.*;
import ar.edu.itba.tav.game_rooms.utils.AggregatorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

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
    private final Map<String, ActorRef> gameRoomActors;

    /**
     * A {@link Map} of {@link ActorRef} holding as keys those actors whose termination process was triggered.
     * This set allows this {@link Actor} to know which children started their termination process,
     * in order to handle their post termination process (i.e execution of {@link #removeGameRoom(ActorRef)}).
     * The values are the {@link ActorRef} that requested the shutdown of the game room.
     */
    private final Map<ActorRef, ActorRef> terminatedActorsAndRequesters;

    /**
     * A {@link Map} of {@link ActorRef} and {@link String},
     * holding for each terminated {@link akka.actor.Actor}, their name (i.e the game room name).
     * This helps the termination process by saving the game room name to be removed from the {@code gameRoomActors},
     * without having to url decode the {@link Actor} name.
     */
    private final Map<ActorRef, String> terminatedActorsAndNames;

    /**
     * Private constructor.
     */
    private GameRoomsManagerActor() {
        this.gameRoomActors = new HashMap<>();
        this.terminatedActorsAndRequesters = new HashMap<>();
        this.terminatedActorsAndNames = new HashMap<>();
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(GetAllGameRoomsMessage.class, msg -> this.getAllGameRooms())
                .match(GetSpecificGameRoomMessage.class, this::getSpecificGameRoom)
                .match(CreateGameRoomMessage.class, this::startGameRoom)
                .match(RemoveGameRoomMessage.class, msg -> this.stopGameRoom(msg.getGameRoomName()))
                .match(Terminated.class,
                        terminated -> this.terminatedActorsAndRequesters.containsKey(terminated.getActor()),
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
        final List<ActorRef> actors = new LinkedList<>(gameRoomActors.values());
        final long timeout = 2000;
        this.getContext()
                .actorOf(AggregatorActor.props(GameRoomDataMessage.class, request, actors, respondTo, timeout));
    }

    /**
     * Replies with the data of the specified game room.
     *
     * @param msg The {@link GetSpecificGameRoomMessage} with containing the name of the game room to be retrieved.
     */
    private void getSpecificGameRoom(GetSpecificGameRoomMessage msg) {
        final ActorRef gameRoomActor = gameRoomActors.get(msg.getGameRoomName());

        if (gameRoomActor == null) {
            this.getSender().tell(Optional.empty(), this.getSelf());
            return;
        }
        final Future<Object> future = Patterns.ask(gameRoomActor, GetGameRoomDataMessage.getMessage(), 100);
        final ExecutionContext executionContext = getContext().dispatcher();
        Patterns.pipe(future.map(Optional::ofNullable, executionContext), executionContext).to(getSender());
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
        if (gameRoomActors.containsKey(gameRoomName)) {
            LOGGER.debug("Name \"{}\" is invalid, or already used", gameRoomName);
            reportToActor(requester, GameRoomCreationResult.NAME_REPEATED);
            return;
        }
        try {
            LOGGER.debug("Trying to create a new game room with name {}", gameRoomName);
            final String urlEncodedName = URLEncoder.encode(gameRoomName, UTF8_ENCODING);
            try {
                final ActorRef actorRef = this.getContext()
                        .actorOf(GameRoomActor.props(gameRoomName, capacity), urlEncodedName);
                this.getContext().watch(actorRef);  // Monitor child life
                this.gameRoomActors.put(gameRoomName, actorRef);
            } catch (IllegalArgumentException e) {
                reportToActor(requester, GameRoomCreationResult.INVALID);
                return;
            }
            reportToActor(requester, GameRoomCreationResult.CREATED);
            LOGGER.debug("Game room with name \"{}\" successfully created. Name is url encoded as \"{}\"",
                    gameRoomName, urlEncodedName);
        } catch (UnsupportedEncodingException | InvalidActorNameException e) {
            LOGGER.error("Some unexpected thing happened. Exception message: {}", e.getMessage());
            LOGGER.debug("Stacktrace: ", e);
            reportToActor(requester, GameRoomCreationResult.FAILURE);
            throw new RuntimeException("Could not url encode game room name", e);
        }
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
        final ActorRef child = gameRoomActors.get(gameRoomName);
        if (child == null) {
            LOGGER.debug("No game room with name \"{}\"", gameRoomName);
            reportToActor(requester, GameRoomRemovalResult.NO_SUCH_GAME_ROOM);
            return;
        }
        this.getContext().stop(child);
        this.terminatedActorsAndRequesters.put(child, requester);
        this.terminatedActorsAndNames.put(child, gameRoomName);
    }

    /**
     * Removes a game room from the system (i.e removes the used name of the game room).
     *
     * @param terminatedActorRef The {@link ActorRef} of a terminated {@link GameRoomActor}.
     */
    private void removeGameRoom(ActorRef terminatedActorRef) {
        Objects.requireNonNull(terminatedActorRef, "The terminatedActorRef must not be null!");
        final ActorRef requester = terminatedActorsAndRequesters.remove(terminatedActorRef);
        final String gameRoomName = this.terminatedActorsAndNames.remove(terminatedActorRef);
        if (requester == null || gameRoomName == null) {
            reportToActor(requester, GameRoomRemovalResult.FAILURE);
            throw new IllegalStateException("Some unexpected thing happened");
        }
        gameRoomActors.remove(gameRoomName);
        reportToActor(requester, GameRoomRemovalResult.REMOVED);
        LOGGER.debug("Successfully stopped game with name \"{}\"", gameRoomName);
        LOGGER.debug("Name \"{}\" is again available for a game room", gameRoomName);
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
