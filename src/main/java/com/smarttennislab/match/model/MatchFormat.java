package com.smarttennislab.match.model;

// Formato pactado antes de empezar. Solo cambia cómo se lee el marcador en la app: el super
// tiebreak reemplaza al tercer set y se juega a 10 puntos con 2 de diferencia.
public enum MatchFormat {
    BEST_OF_3_SETS,
    TWO_SETS_SUPER_TIEBREAK
}
