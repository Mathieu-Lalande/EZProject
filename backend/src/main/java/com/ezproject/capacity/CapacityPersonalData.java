package com.ezproject.capacity;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.ezproject.account.PersonalDataContributor;
import com.ezproject.user.AppUser;

/** RGPD : les absences sont des données personnelles (parfois de santé) : exportées, puis effacées. */
@Component
class CapacityPersonalData implements PersonalDataContributor {

    private final AbsenceRepository absences;

    CapacityPersonalData(AbsenceRepository absences) {
        this.absences = absences;
    }

    @Override
    public String key() {
        return "absences";
    }

    @Override
    public Object export(AppUser user) {
        return absences.findByUserIdOrderByStartDateDesc(user.getId()).stream()
                .map(a -> Map.of("start", a.getStartDate().toString(), "end", a.getEndDate().toString(),
                        "kind", a.getKind(), "comment", a.getComment() == null ? "" : a.getComment()))
                .toList();
    }

    @Override
    public void erase(AppUser user) {
        absences.deleteAll(absences.findByUserIdOrderByStartDateDesc(user.getId()));
    }
}
