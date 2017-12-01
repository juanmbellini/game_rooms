package ar.edu.itba.tav.game_rooms.core;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import ar.edu.itba.tav.game_rooms.messages.SystemMonitorMessages;
import ar.edu.itba.tav.game_rooms.messages.SystemMonitorMessages.GetDataMessage;
import ar.edu.itba.tav.game_rooms.messages.SystemMonitorMessages.SystemMonitorData;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * {@link akka.actor.Actor} in charge of monitoring the system.
 */
public class SystemMonitorActor extends AbstractActor {

    /**
     * Object to be auto-sent to start the monitoring process.
     */
    private final Object startingMessage;

    /**
     * The {@link Cancellable} that makes it possible to measure data with a given frequency.
     */
    private Cancellable measureCancellable;

    /**
     * Percentage of cpu usage.
     */
    private Double cpuUsage;

    /**
     * The amount of available memory.
     */
    private long freeMemory;

    /**
     * The max. amount of memory available for the process.
     */
    private long maxMemory;

    /**
     * The actual amount of memory the jvm has.
     */
    private long jvmMemory;

    /**
     * Private constructor.
     */
    private SystemMonitorActor() {
        this.startingMessage = new Object();
        this.measureCancellable = null;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .matchEquals(startingMessage, msg -> handleStart())
                .match(GetDataMessage.class, msg -> handleGetData())
                .build();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        // Auto-send a message to start the monitoring process
        this.getSelf().tell(startingMessage, this.getSender());
    }


    /**
     * Performs the starting monitor process (i.e schedules the process of monitoring the system).
     */
    private void handleStart() {
        if (measureCancellable != null) {
            return; // Already started
        }

        final FiniteDuration zero = Duration.Zero();
        final FiniteDuration interval = Duration.create(1000, TimeUnit.MILLISECONDS);
        this.measureCancellable = this.getContext().getSystem()
                .scheduler()
                .schedule(zero, interval, this::measure, this.getContext().dispatcher());
    }

    /**
     * Handler for reporting measured data.
     */
    private void handleGetData() {
        Object message = Optional.ofNullable(measureCancellable)
                .<Object>map(ignored -> SystemMonitorData.getMessage(cpuUsage, freeMemory, maxMemory, jvmMemory))
                .orElse(SystemMonitorMessages.SystemMonitorNotStartedMessage.getMessage());
        getSender().tell(message, this.getSelf());
    }

    /**
     * Performs the measurement process
     *
     * @throws SystemMonitorException In case there is any error while performing the process.
     */
    private void measure() throws SystemMonitorException {
        try {
            measureCpuUsage();
            measureMemory();
        } catch (Throwable e) {
            throw new SystemMonitorException(e);
        }
    }

    /**
     * Performs the measurement of cpu usage (only process usage).
     *
     * @throws MalformedObjectNameException In exceptional cases (in fact, it should not throw it).
     * @throws InstanceNotFoundException    In exceptional cases (in fact, it should not throw it).
     * @throws ReflectionException          In exceptional cases (in fact, it should not throw it).
     */
    private void measureCpuUsage()
            throws MalformedObjectNameException, InstanceNotFoundException, ReflectionException {

        final ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final AttributeList list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});

        if (list.isEmpty()) {
            this.cpuUsage = Double.NaN;
            return;
        }

        final Attribute att = (Attribute) list.get(0);
        final Double value = (Double) att.getValue();

        // Usually takes a couple of seconds before we get real values,
        // If not, return percentage with 2 decimal point precision
        this.cpuUsage = (value == -1.0) ? Double.NaN : ((int) (value * 10000) / 100.0);
    }

    /**
     * Performs the measurement of memory usage (only process usage).
     */
    private void measureMemory() {
        this.freeMemory = Runtime.getRuntime().freeMemory();
        this.maxMemory = Runtime.getRuntime().maxMemory();
        this.jvmMemory = Runtime.getRuntime().totalMemory();
    }

    /**
     * Create {@link Props} for an {@link akka.actor.Actor} of this type.
     *
     * @return The created {@link Props}.
     */
    public static Props getProps() {
        return Props.create(SystemMonitorActor.class, SystemMonitorActor::new);
    }


    /**
     * Exception thrown when there is any error with the system monitoring process.
     */
    public final static class SystemMonitorException extends RuntimeException {
        private SystemMonitorException(Throwable e) {
            super(e);
        }
    }
}
