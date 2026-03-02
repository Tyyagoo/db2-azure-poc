package model.task;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "task_audit")
public class TaskAuditEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Transient
    private String actor = "system";

    @Column(name = "changed_at", nullable = false)
    private Instant at;

    @Column(name = "before_snapshot")
    private String beforeSnapshot;

    @Column(name = "after_snapshot")
    private String afterSnapshot;

    public Long getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }

    public String getBeforeSnapshot() {
        return beforeSnapshot;
    }

    public void setBeforeSnapshot(String beforeSnapshot) {
        this.beforeSnapshot = beforeSnapshot;
    }

    public String getAfterSnapshot() {
        return afterSnapshot;
    }

    public void setAfterSnapshot(String afterSnapshot) {
        this.afterSnapshot = afterSnapshot;
    }
}
