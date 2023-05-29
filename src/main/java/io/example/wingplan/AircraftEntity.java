package io.example.wingplan;

import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@EntityKey("aircraftId")
@EntityType("aircraft")
@RequestMapping("/aircraft/{aircraftId}")
public class AircraftEntity extends EventSourcedEntity<AircraftEntity.State, AircraftEntity.Event> {
  private final Logger log = LoggerFactory.getLogger(AircraftEntity.class);
  private final String entityId;

  public AircraftEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public State emptyState() {
    return State.emptyState();
  }

  @PutMapping("/create")
  public Effect<String> createAircraft(@RequestBody CreateAircraftCommand command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);
    if (!currentState().isEmpty()) {
      return effects().reply("OK");
    }
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @GetMapping
  public Effect<State> getAircraft() {
    log.info("EntityId: {}\n_State: {}", entityId, currentState());
    return effects().reply(currentState());
  }

  @EventHandler
  public State on(CreatedAircraftEvent event) {
    log.info("EntityId: {}\n_State: {}\n_Event: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  public record State(String aircraftId, String name, String courseId) {
    static State emptyState() {
      return new State(null, null, null);
    }

    boolean isEmpty() {
      return aircraftId == null || aircraftId.isEmpty();
    }

    CreatedAircraftEvent eventFor(CreateAircraftCommand command) {
      return new CreatedAircraftEvent(command.aircraftId(), command.name(), command.courseId());
    }

    State on(CreatedAircraftEvent event) {
      if (isEmpty()) {
        return new State(event.aircraftId(), event.name(), event.courseId());
      } else {
        return this;
      }
    }
  }

  public interface Event {}

  public record CreateAircraftCommand(String aircraftId, String name, String courseId) {}

  public record CreatedAircraftEvent(String aircraftId, String name, String courseId) implements Event {}
}
