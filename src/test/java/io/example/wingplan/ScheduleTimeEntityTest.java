package io.example.wingplan;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import kalix.javasdk.testkit.EventSourcedTestKit;

public class ScheduleTimeEntityTest {
  @Test
  public void scheduleTest() {
    var testKit = EventSourcedTestKit.of(ScheduleTimeEntity::new);

    var year = Integer.valueOf(2023);
    var month = Integer.valueOf(1);
    var day = Integer.valueOf(1);
    var minuteOfDay = Integer.valueOf(1);
    var type = "student";
    var id = "student1";
    var command = new ScheduleTimeEntity.ScheduleCommand(year, month, day, minuteOfDay, type, id);

    {
      var result = testKit.call(e -> e.schedule(command));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals(year, state.year());
      assertEquals(month, state.month());
      assertEquals(day, state.day());
      assertEquals(minuteOfDay, state.minuteOfDay());
      assertEquals(type, state.type());
      assertEquals(id, state.id());

      var event = result.getNextEventOfType(ScheduleTimeEntity.ScheduledEvent.class);
      assertEquals(year, event.year());
      assertEquals(month, event.month());
      assertEquals(day, event.day());
      assertEquals(minuteOfDay, event.minuteOfDay());
      assertEquals(type, event.type());
      assertEquals(id, event.id());
    }

    { // idempotent test
      var result = testKit.call(e -> e.schedule(command));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals(year, state.year());
      assertEquals(month, state.month());
      assertEquals(day, state.day());
      assertEquals(minuteOfDay, state.minuteOfDay());
      assertEquals(type, state.type());
      assertEquals(id, state.id());

      var events = result.getAllEvents();
      assertEquals(0, events.size());
    }
  }

  @Test
  public void rejectedTest() {
    var testKit = EventSourcedTestKit.of(ScheduleTimeEntity::new);

    var year = Integer.valueOf(2023);
    var month = Integer.valueOf(1);
    var day = Integer.valueOf(1);
    var minuteOfDay = Integer.valueOf(1);
    var type = "student";
    var id = "student1";
    var command = new ScheduleTimeEntity.ScheduleCommand(year, month, day, minuteOfDay, type, id);

    {
      var result = testKit.call(e -> e.schedule(command));
      assertEquals("OK", result.getReply());
    }

    {
      var id2 = "student2";
      var command2 = new ScheduleTimeEntity.ScheduleCommand(year, month, day, minuteOfDay, type, id2);

      var result = testKit.call(e -> e.schedule(command2));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals(year, state.year());
      assertEquals(month, state.month());
      assertEquals(day, state.day());
      assertEquals(minuteOfDay, state.minuteOfDay());
      assertEquals(type, state.type());
      assertEquals(id, state.id());

      var event = result.getNextEventOfType(ScheduleTimeEntity.ScheduleRejectedEvent.class);
      assertEquals(year, event.year());
      assertEquals(month, event.month());
      assertEquals(day, event.day());
      assertEquals(minuteOfDay, event.minuteOfDay());
      assertEquals(type, event.type());
      assertEquals(id2, event.id());
    }
  }

  @Test
  public void releaseSameTypeAndId() {
    var testKit = EventSourcedTestKit.of(ScheduleTimeEntity::new);

    var year = Integer.valueOf(2023);
    var month = Integer.valueOf(1);
    var day = Integer.valueOf(1);
    var minuteOfDay = Integer.valueOf(1);
    var type = "student";
    var id = "student1";

    {
      var command = new ScheduleTimeEntity.ScheduleCommand(year, month, day, minuteOfDay, type, id);
      var result = testKit.call(e -> e.schedule(command));
      assertEquals("OK", result.getReply());
    }

    {
      var command = new ScheduleTimeEntity.ReleaseCommand(year, month, day, minuteOfDay, type, id);
      var result = testKit.call(e -> e.release(command));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals(year, state.year());
      assertEquals(month, state.month());
      assertEquals(day, state.day());
      assertEquals(minuteOfDay, state.minuteOfDay());
      assertEquals(null, state.type());
      assertEquals(null, state.id());

      var event = result.getNextEventOfType(ScheduleTimeEntity.ReleasedEvent.class);
      assertEquals(year, event.year());
      assertEquals(month, event.month());
      assertEquals(day, event.day());
      assertEquals(minuteOfDay, event.minuteOfDay());
      assertEquals(type, event.type());
      assertEquals(id, event.id());
    }
  }

  @Test
  public void releaseDifferentTypeAndId() {
    var testKit = EventSourcedTestKit.of(ScheduleTimeEntity::new);

    var year = Integer.valueOf(2023);
    var month = Integer.valueOf(1);
    var day = Integer.valueOf(1);
    var minuteOfDay = Integer.valueOf(1);
    var type = "student";
    var id = "student1";

    {
      var command = new ScheduleTimeEntity.ScheduleCommand(year, month, day, minuteOfDay, type, id);
      var result = testKit.call(e -> e.schedule(command));
      assertEquals("OK", result.getReply());
    }

    {
      var type2 = "student";
      var id2 = "student2";
      var command = new ScheduleTimeEntity.ReleaseCommand(year, month, day, minuteOfDay, type2, id2);
      var result = testKit.call(e -> e.release(command));
      assertEquals("OK", result.getReply());

      var state = testKit.getState();
      assertEquals(year, state.year());
      assertEquals(month, state.month());
      assertEquals(day, state.day());
      assertEquals(minuteOfDay, state.minuteOfDay());
      assertEquals(type, state.type());
      assertEquals(id, state.id());

      var events = result.getAllEvents();
      assertEquals(0, events.size());
    }
  }
}
