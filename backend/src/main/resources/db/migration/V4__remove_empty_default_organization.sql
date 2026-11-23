-- Sur une installation neuve, V2 crée une organisation "default" qui ne contient rien :
-- on la supprime pour que la première organisation soit celle créée par l'inscription ou ADMIN_*.
-- Sur une base existante (V1 avec données), l'organisation par défaut contient les utilisateurs et est conservée.
DELETE FROM organization
WHERE slug = 'default'
  AND id NOT IN (SELECT organization_id FROM app_user)
  AND id NOT IN (SELECT organization_id FROM project);
