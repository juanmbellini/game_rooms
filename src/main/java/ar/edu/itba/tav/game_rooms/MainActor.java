package ar.edu.itba.tav.game_rooms;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import ar.edu.itba.tav.game_rooms.core.GameRoomsManagerActor;
import ar.edu.itba.tav.game_rooms.http.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link akka.actor.Actor} in charge of creating the other platform managers.
 * It's a convenient way of supervising all managers.
 */
public class MainActor extends AbstractActor {

    /**
     * The {@link Logger} object.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(MainActor.class);

    /**
     * Indicates whether the system is started.
     */
    private boolean started;

    /**
     * Private constructor.
     */
    private MainActor() {
        this.started = false;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(StartSystemMessage.class, msg -> !started, this::startSystem)
                .build();
    }

    /**
     * Starts the system.
     *
     * @param message The start system message containing data used for starting the system.
     */
    private void startSystem(StartSystemMessage message) {
        // Create game manager actor
        final ActorRef gameRoomsManagerRef = getContext()
                .actorOf(GameRoomsManagerActor.getProps(), "game_rooms_manager");

        // Start http server
        HttpServer.createServer(getContext().getSystem(), gameRoomsManagerRef.path())
                .start(message.getHttpServerHostname(), message.getHttpServerPort());

        this.started = true;
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return MainActorSupervisionStrategy.getInstance();
    }

    /**
     * Create {@link Props} for an {@link akka.actor.Actor} of this type.
     *
     * @return The created {@link Props}.
     */
    /* package */
    static Props getProps() {
        return Props.create(MainActor.class, MainActor::new);
    }

    /**
     * Message class to be sent when the system must be started.
     */
    /* package */ static final class StartSystemMessage {

        /**
         * The hostname for the http server.
         */
        private final String httpServerHostname;

        /**
         * The port for the http server.
         */
        private final int httpServerPort;

        /**
         * Private constructor.
         *
         * @param httpServerHostname The hostname for the http server.
         * @param httpServerPort     The port for the http server.
         */
        private StartSystemMessage(String httpServerHostname, int httpServerPort) {
            this.httpServerHostname = httpServerHostname;
            this.httpServerPort = httpServerPort;
        }

        /**
         * @return The hostname for the http server.
         */
        private int getHttpServerPort() {
            return httpServerPort;
        }

        /**
         * @return The port for the http server.
         */
        private String getHttpServerHostname() {
            return httpServerHostname;
        }

        /**
         * Creates a message of this type.
         *
         * @param httpServerHostname The hostname for the http server.
         * @param httpServerPort     The port for the http server.
         * @return The created message.
         */
        /* package */
        static StartSystemMessage createMessage(String httpServerHostname, int httpServerPort) {
            return new StartSystemMessage(httpServerHostname, httpServerPort);
        }
    }

    /**
     * A custom {@link OneForOneStrategy} for this {@link Actor}.
     */
    private final static class MainActorSupervisionStrategy extends OneForOneStrategy {

        /**
         * The single instance of this {@link OneForOneStrategy}.
         */
        private final static MainActorSupervisionStrategy SINGLETON = new MainActorSupervisionStrategy();

        /**
         * Private constructor.
         */
        private MainActorSupervisionStrategy() {
            super(false,
                    DeciderBuilder.match(Throwable.class, e -> {
                        LOGGER.warn("An actor has been stopped because it throw an exception");
                        return stop();
                    }).build());
        }

        /**
         * @return The single instance of this {@link OneForOneStrategy}.
         */
        private static MainActorSupervisionStrategy getInstance() {
            return SINGLETON;
        }
    }
}
