package ar.edu.itba.tav.game_rooms.utils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An {@link akka.actor.Actor} that collects results after sending a request to a bunch of {@link akka.actor.ActorRef}.
 *
 * @param <T> The concrete type of object expected to receive.
 */
public class AggregatorActor<T> extends AbstractActor {

    /**
     * The {@link Class} of the object representing the responses.
     */
    private final Class<T> responseClass;

    /**
     * The request to be sent to the {@link ActorRef}s.
     */
    private final Object request;

    /**
     * A {@link List} of {@link ActorRef} to which the request will be sent.
     */
    private final List<ActorRef> actors;

    /**
     * An {@link ActorRef} to which the aggregation result will be sent.
     */
    private final ActorRef respondTo;

    /**
     * A {@link FiniteDuration} indicating the amount of time the aggregation process will run.
     */
    private final FiniteDuration timeoutDuration;

    /**
     * A {@link Cancellable} which will interrupt the process when the timeout occurs.
     * It's initialized when the process starts (i.e the actor receives the signal to start processing).
     */
    private Cancellable timeoutTask;

    /**
     * A flag indicating the aggregation results are complete.
     */
    private boolean sent;

    /**
     * Object to be auto sent in order to trigger the aggregation process.
     */
    private final Object startAggregationProcessObject;

    /**
     * {@link Map} holding the result each {@link ActorRef} returned.
     */
    private final Map<ActorRef, T> result;

    /**
     * Constructor.
     *
     * @param responseClass The {@link Class} of the object representing the responses.
     * @param request       The request to be sent to the {@link ActorRef}s.
     * @param actors        A {@link List} of {@link ActorRef} to which the request will be sent.
     * @param respondTo     An {@link ActorRef} to which the aggregation result will be sent.
     * @param timeout       The amount of milliseconds to wait till the aggregation process finishes.
     */
    private AggregatorActor(Class<T> responseClass,
                            Object request, List<ActorRef> actors, ActorRef respondTo, long timeout) {
        this.responseClass = responseClass;
        this.request = request;
        this.actors = new LinkedList<>(actors); // Save it in a new private list
        this.respondTo = respondTo;
        this.timeoutDuration = Duration.create(timeout, TimeUnit.MILLISECONDS);
        this.timeoutTask = null;
        this.sent = false;

        this.startAggregationProcessObject = new Object();
        this.result = new HashMap<>();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        // Auto-send a message to start the aggregation process
        this.getSelf().tell(startAggregationProcessObject, this.getSender());
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .matchEquals(startAggregationProcessObject, obj -> handleAggregationRequest())
                .match(responseClass, this::handleResponse)
                .build();
    }

    /**
     * Handles the aggregation request.
     */
    private void handleAggregationRequest() {
        if (this.actors.isEmpty()) {
            terminateSuccessful();
            return;
        }
        // Send the request to each actor
        this.actors.forEach(actorRef -> actorRef.tell(this.request, this.getSelf()));
        this.timeoutTask = this.getContext().getSystem()
                .scheduler()
                .scheduleOnce(timeoutDuration, this::handleTimeout, this.getContext().dispatcher());
    }

    /**
     * Handles the process of receiving a response from an {@link ActorRef} in the {@code actors} {@link List}.
     *
     * @param response The message sent as a response.
     */
    private void handleResponse(T response) {
        if (timeoutTask == null) {
            reportFailure(new IllegalStateException("Process did not started yet!"));
            return;
        }
        final ActorRef sender = this.getSender();
        // Do not handle messages from actors that are not in the actors list
        if (!actors.contains(sender)) {
            return;
        }
        result.put(sender, response);

        if (result.size() == actors.size()) {
            timeoutTask.cancel();
            terminateSuccessful();
        }
    }

    /**
     * Callback method to be executed when the aggregation timeout occurs.
     */
    private void handleTimeout() {
        if (result.size() != actors.size()) {
            reportTimeout();
            return;
        }

        terminateSuccessful();
    }

    /**
     * Terminates the aggregation process successfully (i.e sends results to the requester),
     * and stops this {@link akka.actor.Actor}.
     */
    private void terminateSuccessful() {
        if (this.sent) {
            return;
        }
        this.respondTo.tell(SuccessfulResultMessage.getMessage(result), this.getSelf());
        this.sent = true;
        stopActor();
    }

    /**
     * Terminates the aggregation process sending a timeout message.
     */
    private void reportTimeout() {
        this.respondTo.tell(TimeoutMessage.getMessage(), this.getSelf());
        stopActor();
    }

    /**
     * Terminates the aggregation process sending a failure message.
     *
     * @param e The {@link Throwable} that caused failure.
     */
    private void reportFailure(Throwable e) {
        this.respondTo.tell(FailMessage.getMessage(e), this.getSelf());
        stopActor();
    }

    /**
     * Stops this {@link akka.actor.Actor}.
     */
    private void stopActor() {
        this.getContext().stop(this.getSelf()); // Stop this actor after sending the results
    }

    /**
     * Create {@link Props} for an {@link akka.actor.Actor} of this type.
     *
     * @param responseClass The {@link Class} of the object representing the responses.
     * @param request       The request to be sent to the {@link ActorRef}s.
     * @param actors        A {@link List} of {@link ActorRef} to which the request will be sent.
     * @param respondTo     An {@link ActorRef} to which the aggregation result will be sent.
     * @param timeout       The amount of milliseconds to wait till the aggregation process finishes.
     * @param <E>           The concrete type of object expected to receive.
     * @return The created {@link Props}.
     */
    public static <E> Props props(Class<E> responseClass,
                                  Object request, List<ActorRef> actors, ActorRef respondTo, long timeout) {

        return Props.create(AggregatorActor.class,
                () -> new AggregatorActor<>(responseClass, request, actors, respondTo, timeout));
    }


    /**
     * A message indicating the process finished successfully, including the results.
     *
     * @param <E> The concrete type of the results returned by each {@link ActorRef}.
     */
    public static final class SuccessfulResultMessage<E> {

        /**
         * {@link Map} holding the result each {@link ActorRef} returned.
         */
        private final Map<ActorRef, E> result;

        /**
         * Private constructor.
         *
         * @param result {@link Map} holding the result each {@link ActorRef} returned.
         */
        private SuccessfulResultMessage(Map<ActorRef, E> result) {
            this.result = new HashMap<>(result);
        }

        /**
         * @return {@link Map} holding the result each {@link ActorRef} returned.
         */
        public Map<ActorRef, E> getResult() {
            return result;
        }

        /**
         * Creates a message of this type.
         *
         * @param result {@link Map} holding the result each {@link ActorRef} returned.
         * @param <R>    The concrete type of the results returned by each {@link ActorRef}.
         * @return The created message.
         */
        private static <R> SuccessfulResultMessage getMessage(Map<ActorRef, R> result) {
            return new SuccessfulResultMessage<>(result);
        }
    }

    /**
     * A message indicating the process was interrupted because of timeout.
     */
    public static final class TimeoutMessage {

        /**
         * Private constructor.
         */
        private TimeoutMessage() {
        }

        /**
         * @return A message of this type.
         */
        private static <R> TimeoutMessage getMessage() {
            return new TimeoutMessage();
        }
    }

    /**
     * A message indicating the process was interrupted because of an exception..
     */
    public static final class FailMessage {

        /**
         * The error that caused the interruption.
         */
        private final Throwable error;


        /**
         * Private constructor.
         *
         * @param error The error that caused the interruption.
         */
        private FailMessage(Throwable error) {
            this.error = error;
        }

        /**
         * @return The error that caused the interruption.
         */
        public Throwable getError() {
            return error;
        }

        /**
         * Creates a message of this type.
         *
         * @param error The error that caused the interruption.
         * @return The created message.
         */
        private static FailMessage getMessage(Throwable error) {
            return new FailMessage(error);
        }
    }
}
