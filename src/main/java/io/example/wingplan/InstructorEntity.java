package io.example.wingplan;

import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@EntityKey("studentId")
@EntityType("student")
@RequestMapping("/student/{studentId}")
public class InstructorEntity extends EventSourcedEntity<InstructorEntity.State, InstructorEntity.Event> {
  private final Logger log = LoggerFactory.getLogger(InstructorEntity.class);
  private final String entityId;

  public InstructorEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public State emptyState() {
    return State.emptyState();
  }

  @PutMapping("/create")
  public Effect<String> createInstructor(@RequestBody CreateInstructorCommand command) {
    log.info("EntityId: {}\n_State: {}\n_Command: {}", entityId, currentState(), command);
    if (!currentState().isEmpty()) {
      return effects().reply("OK");
    }
    return effects()
        .emitEvent(currentState().eventFor(command))
        .thenReply(__ -> "OK");
  }

  @GetMapping
  public Effect<State> getInstructor() {
    log.info("EntityId: {}\n_State: {}", entityId, currentState());
    return effects().reply(currentState());
  }

  @EventHandler
  public State on(CreatedInstructorEvent event) {
    log.info("EntityId: {}\n_State: {}\n_Event: {}", entityId, currentState(), event);
    return currentState().on(event);
  }

  public record State(String studentId, String name, String courseId) {
    static State emptyState() {
      return new State(null, null, null);
    }

    boolean isEmpty() {
      return studentId == null || studentId.isEmpty();
    }

    CreatedInstructorEvent eventFor(CreateInstructorCommand command) {
      return new CreatedInstructorEvent(command.studentId(), command.name(), command.courseId());
    }

    State on(CreatedInstructorEvent event) {
      if (isEmpty()) {
        return new State(event.studentId(), event.name(), event.courseId());
      } else {
        return this;
      }
    }
  }

  public interface Event {}

  public record CreateInstructorCommand(String studentId, String name, String courseId) {}

  public record CreatedInstructorEvent(String studentId, String name, String courseId) implements Event {}
}
