package uk.me.steev.java.heating.controller;

import java.time.LocalDateTime;

public class TemperatureEvent implements Comparable<TemperatureEvent> {
  private LocalDateTime timeDueOn;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  float temperature;

  public TemperatureEvent (LocalDateTime timeDueOn, LocalDateTime startTime,
      LocalDateTime endTime, float temperature) {
    this.timeDueOn = timeDueOn;
    this.startTime = startTime;
    this.endTime = endTime;
    this.temperature = temperature;
  }

  public LocalDateTime getTimeDueOn() {
    return timeDueOn;
  }

  public void setTimeDueOn(LocalDateTime timeDueOn) {
    this.timeDueOn = timeDueOn;
  }

  public float getTemperature() {
    return temperature;
  }

  public void setTemperature(float temperature) {
    this.temperature = temperature;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime realTimeDueOn) {
    this.startTime = realTimeDueOn;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public int compareTo(TemperatureEvent otherEvent) {
    boolean isAfter = this.timeDueOn.isAfter(otherEvent.getTimeDueOn()) ||
        (this.timeDueOn.equals(otherEvent.getTimeDueOn()) &&
         this.temperature > otherEvent.getTemperature());
    boolean isBefore = this.timeDueOn.isBefore(otherEvent.getTimeDueOn()) ||
        (this.timeDueOn.equals(otherEvent.getTimeDueOn()) &&
         this.temperature < otherEvent.getTemperature());

    if (isAfter)
      return 1;
    else if (isBefore)
      return -1;
    else
      return 0;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("TemperatureEvent [timeDueOn=").append(timeDueOn).append(", startTime=").append(startTime)
        .append(", endTime=").append(endTime).append(", temperature=").append(temperature).append("]");
    return builder.toString();
  }
}
