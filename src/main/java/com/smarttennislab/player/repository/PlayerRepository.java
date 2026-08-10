package com.smarttennislab.player.repository;

import com.smarttennislab.player.model.Player;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// Todos los métodos llevan coachId: es la barrera que impide que un profe toque alumnos de otro.
// No agregar acá un findById pelado.
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    Optional<Player> findByIdAndCoachId(UUID id, UUID coachId);

    List<Player> findByCoachIdAndArchivedAtIsNullOrderByLastNameAscFirstNameAsc(UUID coachId);

    List<Player> findByCoachIdOrderByLastNameAscFirstNameAsc(UUID coachId);
}
