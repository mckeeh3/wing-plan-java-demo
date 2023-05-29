package io.example.wingplan;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import kalix.javasdk.testkit.EventSourcedTestKit;

public class TrainingSessionEntityTest {
  @Test
  public void scheduleTest() {
    var testKit = EventSourcedTestKit.of(TrainingSessionEntity::new);

    var startTime = Instant.now();
    var duration = Duration.ofHours(1);
    var command = new TrainingSessionEntity.ScheduleCommand("student1", "instructor1", "aircraft1", startTime, duration);

    {
      var result = testKit.call(e -> e.schedule(command));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals("student1", state.studentId());
      assertEquals("instructor1", state.instructorId());
      assertEquals("aircraft1", state.aircraftId());
      assertEquals(startTime, state.startTime());
      assertEquals(duration, state.duration());

      var event = result.getNextEventOfType(TrainingSessionEntity.ScheduledEvent.class);
      assertEquals("student1", event.studentId());
      assertEquals("instructor1", event.instructorId());
      assertEquals("aircraft1", event.aircraftId());
      assertEquals(startTime, event.startTime());
      assertEquals(duration, event.duration());
    }

    { // idempotent test
      var result = testKit.call(e -> e.schedule(command));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals("student1", state.studentId());
      assertEquals("instructor1", state.instructorId());
      assertEquals("aircraft1", state.aircraftId());
      assertEquals(startTime, state.startTime());
      assertEquals(duration, state.duration());

      var events = result.getAllEvents();
      assertEquals(0, events.size());
    }
  }

  @Test
  public void completeTest() {
    var testKit = EventSourcedTestKit.of(TrainingSessionEntity::new);

    var startTime = Instant.now();
    var duration = Duration.ofHours(1);
    var completedTime = startTime.plus(duration);
    var scheduleCommand = new TrainingSessionEntity.ScheduleCommand("student1", "instructor1", "aircraft1", startTime, duration);
    var completeCommand = new TrainingSessionEntity.CompleteCommand("student1", "instructor1", "aircraft1", startTime, completedTime);

    {
      var result = testKit.call(e -> e.schedule(scheduleCommand));
      assertEquals("OK", result.getReply());
    }

    {
      var result = testKit.call(e -> e.complete(completeCommand));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals("student1", state.studentId());
      assertEquals("instructor1", state.instructorId());
      assertEquals("aircraft1", state.aircraftId());
      assertEquals(startTime, state.startTime());
      assertEquals(duration, state.duration());
      assertEquals(completedTime, state.completedTime());

      var event = result.getNextEventOfType(TrainingSessionEntity.CompletedEvent.class);
      assertEquals("student1", event.studentId());
      assertEquals("instructor1", event.instructorId());
      assertEquals("aircraft1", event.aircraftId());
      assertEquals(startTime, event.startTime());
      assertEquals(completedTime, event.completedTime());
    }

    { // idempotent test
      var result = testKit.call(e -> e.complete(completeCommand));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals("student1", state.studentId());
      assertEquals("instructor1", state.instructorId());
      assertEquals("aircraft1", state.aircraftId());
      assertEquals(startTime, state.startTime());
      assertEquals(duration, state.duration());
      assertEquals(completedTime, state.completedTime());

      var events = result.getAllEvents();
      assertEquals(0, events.size());
    }
  }

  @Test
  public void cancelledTest() {
    var testKit = EventSourcedTestKit.of(TrainingSessionEntity::new);

    var startTime = Instant.now();
    var duration = Duration.ofHours(1);
    var cancelledTime = startTime.plus(duration);
    var reason = "cancelled reason";
    var scheduleCommand = new TrainingSessionEntity.ScheduleCommand("student1", "instructor1", "aircraft1", startTime, duration);
    var cancelCommand = new TrainingSessionEntity.CancelCommand("student1", "instructor1", "aircraft1", startTime, cancelledTime, reason);

    {
      var result = testKit.call(e -> e.schedule(scheduleCommand));
      assertEquals("OK", result.getReply());
    }

    {
      var result = testKit.call(e -> e.cancel(cancelCommand));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals("student1", state.studentId());
      assertEquals("instructor1", state.instructorId());
      assertEquals("aircraft1", state.aircraftId());
      assertEquals(startTime, state.startTime());
      assertEquals(duration, state.duration());
      assertEquals(cancelledTime, state.cancelledTime());

      var event = result.getNextEventOfType(TrainingSessionEntity.CancelledEvent.class);
      assertEquals("student1", event.studentId());
      assertEquals("instructor1", event.instructorId());
      assertEquals("aircraft1", event.aircraftId());
      assertEquals(startTime, event.startTime());
      assertEquals(cancelledTime, event.cancelledTime());
    }

    { // idempotent test
      var result = testKit.call(e -> e.cancel(cancelCommand));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals("student1", state.studentId());
      assertEquals("instructor1", state.instructorId());
      assertEquals("aircraft1", state.aircraftId());
      assertEquals(startTime, state.startTime());
      assertEquals(duration, state.duration());
      assertEquals(cancelledTime, state.cancelledTime());

      var events = result.getAllEvents();
      assertEquals(0, events.size());
    }
  }
}
