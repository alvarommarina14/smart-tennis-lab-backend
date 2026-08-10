package com.smarttennislab.auth.model;

import java.util.UUID;

// Lo que queda en el SecurityContext. Los controllers lo reciben con @AuthenticationPrincipal y
// todas las consultas filtran por este id: es lo que aísla a un profe de otro.
public record CoachPrincipal(UUID id, String email) {}
