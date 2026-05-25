package com.radar.prospection.domain;

public enum MissionStatus {
    NEW,        // détectée, pas encore analysée
    ANALYZED,   // analysée par Claude
    SHORTLISTED,// retenue pour contact
    CONTACTED,  // message envoyé
    REPLIED,    // réponse reçue
    IN_DISCUSSION,
    ARCHIVED    // hors scope ou expirée
}
