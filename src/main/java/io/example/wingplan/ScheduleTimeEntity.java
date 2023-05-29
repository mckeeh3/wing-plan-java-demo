package io.example.wingplan;

import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@EntityKey("{year,month,day,minuteOfDay}")
@EntityType("scheduleTime")
@RequestMapping("/scheduleTime/{year}/{month}/{day}/{minuteOfDay}")
public class ScheduleTimeEntity extends EventSourcedEntity<ScheduleTimeEntity.State, ScheduleTimeEntity.Event> {
  private final Logger log = LoggerFactory.getLogger(ScheduleTimeEntity.class);
  private final String entityId;

  public ScheduleTimeEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public State emptyState() {
    return State.emptyState();
  }

  @PutMapping("/schedule")
  public Effect<String> schedule(@RequestBody ScheduleCommand command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);
    if (!currentState().isEmpty() && currentState().isSameTypeAndId(command.type(), command.id())) {
      return effects().reply("OK");
    }
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @PutMapping("/release")
  public Effect<String> release(@RequestBody ReleaseCommand command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);
    if (currentState().isEmpty() || !currentState().isSameTypeAndId(command.type(), command.id())) {
      return effects().reply("OK");
    }
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @GetMapping
  public Effect<State> getScheduleTime() {
    log.info("EntityId: {}\n_State: {}", entityId, currentState());
    return effects().reply(currentState());
  }

  @EventHandler
  public State on(ScheduledEvent event) {
    log.info("EntityId: {}\n_State: {}\n_Event: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(ScheduleRejectedEvent event) {
    log.info("EntityId: {}\n_State: {}\n_Event: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  @EventHandler
  public State on(ReleasedEvent event) {
    log.info("EntityId: {}\n_State: {}\n_Event: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  public record State(Integer year, Integer month, Integer day, Integer minuteOfDay, String type, String id) {
    static State emptyState() {
      return new State(null, null, null, null, null, null);
    }

    boolean isEmpty() {
      return year == null || year.equals(0);
    }

    boolean isSameTypeAndId(String type, String id) {
      return this.type != null && this.id != null && this.type.equals(type) && this.id.equals(id);
    }

    Event eventFor(ScheduleCommand command) {
      if (isEmpty() || type == null && id == null) {
        return new ScheduledEvent(command.year(), command.month(), command.day(), command.minuteOfDay(), command.type(), command.id());
      }
      return new ScheduleRejectedEvent(command.year(), command.month(), command.day(), command.minuteOfDay(), command.type(), command.id());
    }

    Event eventFor(ReleaseCommand command) {
      if (isSameTypeAndId(command.type(), command.id())) {
        return new ReleasedEvent(command.year(), command.month(), command.day(), command.minuteOfDay(), command.type(), command.id());
      }
      return new ScheduleRejectedEvent(command.year(), command.month(), command.day(), command.minuteOfDay(), command.type(), command.id());
    }

    State on(ScheduledEvent event) {
      if (isEmpty()) {
        return new State(event.year(), event.month(), event.day(), event.minuteOfDay(), event.type(), event.id);
      } else {
        return this;
      }
    }

    State on(ScheduleRejectedEvent event) {
      return this;
    }

    State on(ReleasedEvent event) {
      if (isSameTypeAndId(event.type(), event.id())) {
        return new State(year, month, day, minuteOfDay, null, null);
      }
      return this;
    }
  }

  public interface Event {}

  public record ScheduleCommand(Integer year, Integer month, Integer day, Integer minuteOfDay, String type, String id) {}

  public record ScheduledEvent(Integer year, Integer month, Integer day, Integer minuteOfDay, String type, String id) implements Event {}

  public record ScheduleRejectedEvent(Integer year, Integer month, Integer day, Integer minuteOfDay, String type, String id) implements Event {}

  public record ReleaseCommand(Integer year, Integer month, Integer day, Integer minuteOfDay, String type, String id) {}

  public record ReleasedEvent(Integer year, Integer month, Integer day, Integer minuteOfDay, String type, String id) implements Event {}
}
