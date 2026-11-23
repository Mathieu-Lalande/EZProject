package com.ezproject.account;

import com.ezproject.user.AppUser;

/**
 * Point d'extension RGPD : chaque module détenant des données personnelles (temps saisis, absences,
 * commentaires…) les ajoute à l'export et les efface ou les anonymise lors de la suppression d'un compte.
 */
public interface PersonalDataContributor {

    String key();

    Object export(AppUser user);

    void erase(AppUser user);
}
