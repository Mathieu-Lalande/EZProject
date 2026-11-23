package com.ezproject.account;

import com.ezproject.organization.Organization;

/** Point d'extension : chaque module ajoute ses données à l'export complet d'une organisation. */
public interface OrganizationDataContributor {

    String key();

    Object export(Organization organization);
}
