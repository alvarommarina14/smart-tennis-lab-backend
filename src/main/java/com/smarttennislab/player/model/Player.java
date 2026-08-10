package com.smarttennislab.player.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Se guarda el id y no la entidad User: el alumno nunca necesita navegar hasta el profe, y así
    // no hay lazy loading que se escape a la vista.
    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "dominant_hand")
    private DominantHand dominantHand;

    @Column(name = "notes")
    private String notes;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Player(UUID coachId, String firstName, String lastName) {
        this.coachId = coachId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public void archive() {
        this.archivedAt = Instant.now();
    }

    public void restore() {
        this.archivedAt = null;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
