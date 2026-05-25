package com.radar.prospection.domain;

public enum SignalType {
    FUNDING,            // levée de fonds → recrutement à venir
    CTO_NOMINATION,     // nouveau CTO/VP Eng → réorganisation
    TECH_HIRING_SPREE,  // 3+ offres dev publiées → projet en démarrage
    TECH_BLOG_POST,     // article tech récent → équipe active
    CONFERENCE_SPEAKER, // speaker d'une conf → contact warm
    JOB_STILL_OPEN,     // offre dev ouverte > 30j → besoin difficile à combler
    FORMER_COLLEAGUE,   // ancien collègue promu décideur
    APPEL_OFFRES        // marché public BOAMP
}
