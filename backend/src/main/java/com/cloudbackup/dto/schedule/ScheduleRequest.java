package com.cloudbackup.dto.schedule;

import com.cloudbackup.entity.ScheduleFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ScheduleRequest {

    @NotBlank(message = "Schedule name is required")
    @Size(min = 2, max = 150, message = "Schedule name must be between 2 and 150 characters")
    private String scheduleName;

    private String cronExpression;

    private ScheduleFrequency frequency = ScheduleFrequency.DAILY;

    private String targetDirectory = "/";

    public ScheduleRequest() {
    }

    public ScheduleRequest(String scheduleName, String cronExpression, ScheduleFrequency frequency, String targetDirectory) {
        this.scheduleName = scheduleName;
        this.cronExpression = cronExpression;
        this.frequency = frequency != null ? frequency : ScheduleFrequency.DAILY;
        this.targetDirectory = targetDirectory != null ? targetDirectory : "/";
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public ScheduleFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(ScheduleFrequency frequency) {
        this.frequency = frequency;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }
}
