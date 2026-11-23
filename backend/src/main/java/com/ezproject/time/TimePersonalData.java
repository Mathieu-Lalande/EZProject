package com.ezproject.time;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.ezproject.account.PersonalDataContributor;
import com.ezproject.user.AppUser;

/**
 * RGPD : les temps saisis figurent dans l'export personnel. À l'effacement du compte, ils sont conservés
 * (consommation des budgets) mais rattachés au compte anonymisé.
 */
@Component
class TimePersonalData implements PersonalDataContributor {

    private final TimeEntryRepository repository;

    TimePersonalData(TimeEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public String key() {
        return "timeEntries";
    }

    @Override
    public Object export(AppUser user) {
        return repository.findByUserIdOrderByWorkDateDesc(user.getId()).stream()
                .map(e -> Map.of("project", e.getProject().getCode(), "date", e.getWorkDate().toString(),
                        "days", e.getDays(), "comment", e.getComment() == null ? "" : e.getComment()))
                .toList();
    }

    @Override
    public void erase(AppUser user) {
        // Données de gestion conservées, l'auteur est anonymisé avec le compte
    }
}
