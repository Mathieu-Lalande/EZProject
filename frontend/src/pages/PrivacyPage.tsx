import { Link } from 'react-router-dom';

/**
 * Politique de confidentialité : MODÈLE à compléter par l'éditeur du service (responsable de traitement,
 * hébergeur, DPO…). Les éléments techniques décrits correspondent au fonctionnement réel de l'application.
 */
export function PrivacyPage() {
  return (
    <div className="mx-auto max-w-3xl px-6 py-12 leading-relaxed">
      <Link to="/login" className="text-sm text-brand-600 hover:underline">← Retour</Link>
      <h1 className="mt-4 mb-2 text-3xl font-semibold tracking-tight">Politique de confidentialité</h1>
      <p className="mb-8 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200">
        Modèle à compléter par l'éditeur du service avant mise en production (éléments entre crochets).
      </p>
      <div className="space-y-6 text-sm text-ink-2">
        <Section title="Responsable du traitement">
          [Raison sociale], [adresse], [SIREN]. Contact : [email du DPO ou du responsable].
        </Section>
        <Section title="Données traitées">
          Compte (nom, email, fonction, rôle), contenu saisi dans les projets (tâches, commentaires, temps passés, absences,
          risques), journal d'audit des actions et des connexions. Aucun traceur publicitaire n'est utilisé ; seuls des
          cookies strictement nécessaires (session et protection CSRF) sont déposés.
        </Section>
        <Section title="Finalités et base légale">
          Fourniture du service de gestion de projets (exécution du contrat) et sécurité du service (intérêt légitime :
          journal d'audit, limitation des tentatives de connexion).
        </Section>
        <Section title="Durées de conservation">
          Pendant la durée de l'abonnement de l'organisation. Journal d'audit : [12 mois]. Sauvegardes : [30 jours].
          À la suppression de l'organisation, l'ensemble de ses données est effacé.
        </Section>
        <Section title="Hébergement">[Hébergeur, pays / région d'hébergement].</Section>
        <Section title="Vos droits">
          Accès et portabilité : « Mon compte » → « Exporter mes données ». Effacement : « Mon compte » → « Supprimer mon compte »
          (vos données personnelles sont effacées et votre nom est anonymisé dans l'historique des projets).
          Rectification : « Mon compte » → « Profil ». Pour toute autre demande : [contact]. Vous pouvez saisir la CNIL.
        </Section>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section>
      <h2 className="mb-1 text-base font-semibold text-ink">{title}</h2>
      <p>{children}</p>
    </section>
  );
}
