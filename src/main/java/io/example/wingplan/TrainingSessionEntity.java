package io.example.wingplan;

import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@EntityKey({ "studentId", "instructorId", "aircraftId", "startTime" })
@EntityType("trainingSession")
@RequestMapping("/trainingSession/{studentId}/{instructorId}/{aircraftId}/{startTime}")
public class TrainingSessionEntity extends EventSourcedEntity<TrainingSessionEntity.State, TrainingSessionEntity.Event> {
  private final Logger log = LoggerFactory.getLogger(TrainingSessionEntity.class);
  private final String entityId;

  public TrainingSessionEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public State emptyState() {
    return State.emptyState();
  }

  @PutMapping("/schedule")
  public Effect<String> schedule(@RequestBody ScheduleCommand command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);
    if (!currentState().isEmpty()) {
      return effects().reply("OK");
    }
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @PutMapping("/complete")
  public Effect<String> complete(@RequestBody CompleteCommand command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);
    if (currentState().completedTime != null) {
      return effects().reply("OK");
    }
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @PutMapping("/cancel")
  public Effect<String> cancel(@RequestBody CancelCommand command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);
    if (currentState().cancelledTime != null) {
      return effects().reply("OK");
    }
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @GetMapping
  public Effect<State> getTrainingSession() {
    log.info("EntityId: {}\n_State: {}", entityId, currentState());
    return effects().reply(currentState());
  }

  @EventHandler
  public State on(ScheduledEvent event) {
    log.info("EntityId: {}\n_State: {}\n_Event: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(CompletedEvent event) {
    log.info("EntityId: {}\n_State: {}\n_Event: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(CancelledEvent event) {
    log.info("EntityId: {}\n_State: {}\n_Event: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  public record State(
      String studentId,
      String instructorId,
      String aircraftId,
      Instant startTime,
      Duration duration,
      Instant completedTime,
      Instant cancelledTime) {
    static State emptyState() {
      return new State(null, null, null, null, null, null, null);
    }

    boolean isEmpty() {
      return studentId == null || studentId.isEmpty();
    }

    Event eventFor(ScheduleCommand command) {
      if (completedTime != null) {
        return new ScheduleRejectedEvent(command.studentId(), command.instructorId(), command.aircraftId(), command.startTime(), command.duration(),
            "Training session already completed");
      }
      if (cancelledTime != null) {
        return new ScheduleRejectedEvent(command.studentId(), command.instructorId(), command.aircraftId(), command.startTime(), command.duration(),
            "Training session has been cancelled");
      }
      return new ScheduledEvent(command.studentId(), command.instructorId(), command.aircraftId(), command.startTime(), command.duration());
    }

    Event eventFor(CompleteCommand command) {
      if (completedTime != null) {
        return new CompleteRejectedEvent(command.studentId(), command.instructorId(), command.aircraftId(), command.completedTime(), command.startTime(),
            "Training session already completed");
      }
      if (cancelledTime != null) {
        return new CompleteRejectedEvent(command.studentId(), command.instructorId(), command.aircraftId(), command.startTime(), command.completedTime(),
            "Training session has been cancelled");
      }
      return new CompletedEvent(command.studentId(), command.instructorId(), command.aircraftId(), command.startTime(), command.completedTime());
    }

    Event eventFor(CancelCommand command) {
      return new CancelledEvent(command.studentId(), command.instructorId(), command.aircraftId(), command.startTime(), command.cancelTime(), command.reason());
    }

    State on(ScheduledEvent event) {
      if (isEmpty()) {
        return new State(event.studentId(), event.instructorId(), event.aircraftId(), event.startTime(), event.duration(), null, null);
      } else {
        return this;
      }
    }

    State on(ScheduleRejectedEvent event) {
      return this;
    }

    State on(CancelledEvent event) {
      return new State(studentId, instructorId, aircraftId, startTime, duration, completedTime, event.cancelledTime());
    }

    State on(CompletedEvent event) {
      return new State(studentId, instructorId, aircraftId, startTime, duration, event.completedTime(), cancelledTime);
    }
  }

  public interface Event {}

  public record ScheduleCommand(String studentId, String instructorId, String aircraftId, Instant startTime, Duration duration) {}

  public record ScheduledEvent(String studentId, String instructorId, String aircraftId, Instant startTime, Duration duration) implements Event {}

  public record ScheduleRejectedEvent(String studentId, String instructorId, String aircraftId, Instant startTime, Duration duration, String reason) implements Event {}

  public record CancelCommand(String studentId, String instructorId, String aircraftId, Instant startTime, Instant cancelTime, String reason) {}

  public record CancelledEvent(String studentId, String instructorId, String aircraftId, Instant startTime, Instant cancelledTime, String reason) implements Event {}

  public record CompleteCommand(String studentId, String instructorId, String aircraftId, Instant startTime, Instant completedTime) {}

  public record CompletedEvent(String studentId, String instructorId, String aircraftId, Instant startTime, Instant completedTime) implements Event {}

  public record CompleteRejectedEvent(String studentId, String instructorId, String aircraftId, Instant startTime, Instant completedTime, String reason) implements Event {}
}
