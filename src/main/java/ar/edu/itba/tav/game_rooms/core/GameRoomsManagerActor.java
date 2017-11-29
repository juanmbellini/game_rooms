package ar.edu.itba.tav.game_rooms.core;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import ar.edu.itba.tav.game_rooms.messages.GameRoomOperationMessages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.ScalaStreamSupport;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;


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


}
