-- V10 (PostgreSQL uniquement) : Row-Level Security, défense en profondeur de l'isolation multi-organisation.
-- L'application pose app.org_id à chaque emprunt de connexion (TenantAwareDataSource). Quand elle est vide
-- (connexion, migrations, tâches système), l'accès reste global ; sinon seules les lignes de l'organisation
-- sont visibles et modifiables. FORCE s'applique aussi au propriétaire des tables (compte de l'application).

CREATE OR REPLACE FUNCTION ez_current_org() RETURNS BIGINT
    LANGUAGE sql STABLE
AS $$ SELECT NULLIF(current_setting('app.org_id', true), '')::BIGINT $$;

-- Tables portant directement l'organisation
ALTER TABLE project ENABLE ROW LEVEL SECURITY;
ALTER TABLE project FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON project
    USING (ez_current_org() IS NULL OR organization_id = ez_current_org())
    WITH CHECK (ez_current_org() IS NULL OR organization_id = ez_current_org());

ALTER TABLE app_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_user FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON app_user
    USING (ez_current_org() IS NULL OR organization_id = ez_current_org())
    WITH CHECK (ez_current_org() IS NULL OR organization_id = ez_current_org());

ALTER TABLE team ENABLE ROW LEVEL SECURITY;
ALTER TABLE team FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON team
    USING (ez_current_org() IS NULL OR organization_id = ez_current_org())
    WITH CHECK (ez_current_org() IS NULL OR organization_id = ez_current_org());

ALTER TABLE audit_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_event FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON audit_event
    USING (ez_current_org() IS NULL OR organization_id = ez_current_org())
    WITH CHECK (ez_current_org() IS NULL OR organization_id = ez_current_org());

ALTER TABLE role_permission ENABLE ROW LEVEL SECURITY;
ALTER TABLE role_permission FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON role_permission
    USING (ez_current_org() IS NULL OR organization_id = ez_current_org())
    WITH CHECK (ez_current_org() IS NULL OR organization_id = ez_current_org());

-- Tables rattachées à un projet (le projet est lui-même filtré par sa politique)
ALTER TABLE task ENABLE ROW LEVEL SECURITY;
ALTER TABLE task FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON task
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = task.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = task.project_id));

ALTER TABLE retro_phase ENABLE ROW LEVEL SECURITY;
ALTER TABLE retro_phase FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON retro_phase
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = retro_phase.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = retro_phase.project_id));

ALTER TABLE poker_round ENABLE ROW LEVEL SECURITY;
ALTER TABLE poker_round FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON poker_round
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = poker_round.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = poker_round.project_id));

ALTER TABLE time_entry ENABLE ROW LEVEL SECURITY;
ALTER TABLE time_entry FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON time_entry
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = time_entry.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = time_entry.project_id));

ALTER TABLE risk ENABLE ROW LEVEL SECURITY;
ALTER TABLE risk FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON risk
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = risk.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = risk.project_id));

ALTER TABLE decision ENABLE ROW LEVEL SECURITY;
ALTER TABLE decision FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON decision
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = decision.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = decision.project_id));

ALTER TABLE status_report ENABLE ROW LEVEL SECURITY;
ALTER TABLE status_report FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON status_report
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = status_report.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = status_report.project_id));

ALTER TABLE allocation ENABLE ROW LEVEL SECURITY;
ALTER TABLE allocation FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON allocation
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = allocation.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = allocation.project_id));

ALTER TABLE project_member ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_member FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON project_member
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = project_member.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = project_member.project_id));

ALTER TABLE project_status_change ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_status_change FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON project_status_change
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = project_status_change.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = project_status_change.project_id));

ALTER TABLE project_module ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_module FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON project_module
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = project_module.project_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM project p WHERE p.id = project_module.project_id));

-- Tables rattachées à une tâche
ALTER TABLE task_comment ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_comment FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON task_comment
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM task t WHERE t.id = task_comment.task_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM task t WHERE t.id = task_comment.task_id));

ALTER TABLE task_checklist_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_checklist_item FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON task_checklist_item
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM task t WHERE t.id = task_checklist_item.task_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM task t WHERE t.id = task_checklist_item.task_id));

ALTER TABLE task_label ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_label FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON task_label
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM task t WHERE t.id = task_label.task_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM task t WHERE t.id = task_label.task_id));

ALTER TABLE task_attachment ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_attachment FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON task_attachment
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM task t WHERE t.id = task_attachment.task_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM task t WHERE t.id = task_attachment.task_id));

-- Tables rattachées à un utilisateur
ALTER TABLE absence ENABLE ROW LEVEL SECURITY;
ALTER TABLE absence FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON absence
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM app_user u WHERE u.id = absence.user_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM app_user u WHERE u.id = absence.user_id));

ALTER TABLE mfa_recovery_code ENABLE ROW LEVEL SECURITY;
ALTER TABLE mfa_recovery_code FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON mfa_recovery_code
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM app_user u WHERE u.id = mfa_recovery_code.user_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM app_user u WHERE u.id = mfa_recovery_code.user_id));

-- Cas particuliers
ALTER TABLE team_member ENABLE ROW LEVEL SECURITY;
ALTER TABLE team_member FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON team_member
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM team t WHERE t.id = team_member.team_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM team t WHERE t.id = team_member.team_id));

ALTER TABLE poker_vote ENABLE ROW LEVEL SECURITY;
ALTER TABLE poker_vote FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON poker_vote
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM poker_round r WHERE r.id = poker_vote.round_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM poker_round r WHERE r.id = poker_vote.round_id));

ALTER TABLE todo_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE todo_item FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON todo_item
    USING (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM app_user u WHERE u.id = todo_item.owner_id))
    WITH CHECK (ez_current_org() IS NULL OR EXISTS (SELECT 1 FROM app_user u WHERE u.id = todo_item.owner_id));
