import { useRef, useState } from 'react';
import { AlertCircle, CheckCircle2, Download, FileSpreadsheet, Upload } from 'lucide-react';
import { useImport, useMe } from '../../api/hooks';
import type { ImportReport } from '../../api/types';
import { ApiError } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { useToast } from '../../components/ui/Toast';
import { isManager } from '../../lib/permissions';

/**
 * Reprise des données existantes (portefeuille Excel, export d'un autre outil) :
 * 1) vérification à blanc avec rapport ligne par ligne, 2) import réel, tout ou rien.
 */
export function ImportExportTab() {
  const { data: me } = useMe();
  return (
    <div className="space-y-6">
      <div className="grid gap-6 lg:grid-cols-2">
        {isManager(me) && (
          <ImportCard kind="projects" title="Importer des projets"
            description="Création ou mise à jour par code projet. Les équipes inconnues sont créées. Une colonne vide conserve la valeur existante." />
        )}
        <ImportCard kind="tasks" title="Importer des tâches"
          description="Ajout de tâches dans des projets existants, selon vos droits sur chaque projet." />
      </div>
      <section className="card p-5">
        <h3 className="text-sm font-semibold">Exporter</h3>
        <p className="mb-4 text-xs text-ink-3">CSV compatible Excel (séparateur « ; », UTF-8), limité aux projets que vous pouvez voir.</p>
        <div className="flex flex-wrap gap-2">
          <a className="inline-flex h-9 items-center gap-2 rounded-lg border border-line bg-surface px-3.5 text-sm font-medium hover:bg-surface-2" href="/api/export/portfolio.csv" download>
            <Download className="size-4" />Portefeuille (synthèse par projet)
          </a>
          <a className="inline-flex h-9 items-center gap-2 rounded-lg border border-line bg-surface px-3.5 text-sm font-medium hover:bg-surface-2" href="/api/export/tasks.csv" download>
            <Download className="size-4" />Toutes les tâches
          </a>
        </div>
      </section>
    </div>
  );
}

function ImportCard({ kind, title, description }: { kind: 'projects' | 'tasks'; title: string; description: string }) {
  const importer = useImport(kind);
  const toast = useToast();
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [report, setReport] = useState<ImportReport | null>(null);

  const run = (dryRun: boolean) => {
    if (!file) return;
    importer.mutate({ file, dryRun }, {
      onSuccess: (r) => {
        setReport(r);
        if (r.applied) toast('success', `Import terminé : ${r.created} créé(s), ${r.updated} mis à jour`);
      },
      onError: (e) => toast('error', e instanceof ApiError ? e.message : 'Import impossible'),
    });
  };

  return (
    <section className="card flex flex-col p-5">
      <div className="mb-1 flex items-center gap-2 text-sm font-semibold"><FileSpreadsheet className="size-4 text-brand-500" />{title}</div>
      <p className="mb-4 text-xs text-ink-3">{description}</p>
      <a className="mb-4 inline-flex items-center gap-1.5 text-xs font-medium text-brand-600 hover:underline" href={`/api/import/templates/${kind}.csv`} download>
        <Download className="size-3.5" />Télécharger le modèle CSV
      </a>
      <input ref={input} type="file" accept=".csv,text/csv" className="hidden"
        onChange={(e) => { setFile(e.target.files?.[0] ?? null); setReport(null); }} />
      <div className="flex flex-wrap items-center gap-2">
        <Button variant="secondary" icon={<Upload className="size-4" />} onClick={() => input.current?.click()}>
          {file ? file.name : 'Choisir un fichier'}
        </Button>
        <Button variant="secondary" disabled={!file} loading={importer.isPending && importer.variables?.dryRun} onClick={() => run(true)}>Vérifier</Button>
        <Button disabled={!file || !report || !report.dryRun || report.errors.length > 0} loading={importer.isPending && !importer.variables?.dryRun} onClick={() => run(false)}>
          Importer
        </Button>
      </div>
      {report && <ReportView report={report} />}
    </section>
  );
}

function ReportView({ report }: { report: ImportReport }) {
  const ok = report.errors.length === 0;
  return (
    <div className="mt-4 rounded-lg border border-line bg-surface-2/50 p-3 text-sm">
      <div className={`mb-2 flex items-center gap-2 font-medium ${ok ? 'text-emerald-600' : 'text-red-600'}`}>
        {ok ? <CheckCircle2 className="size-4" /> : <AlertCircle className="size-4" />}
        {report.applied ? 'Import appliqué' : ok ? 'Fichier valide — prêt à importer' : `${report.errors.length} erreur(s) : rien n'a été importé`}
      </div>
      <p className="text-xs text-ink-3">
        {report.rows} ligne(s) · {report.created} création(s) · {report.updated} mise(s) à jour{report.teamsCreated ? ` · ${report.teamsCreated} équipe(s) créée(s)` : ''}
      </p>
      {report.errors.length > 0 && (
        <ul className="mt-2 max-h-40 space-y-1 overflow-y-auto text-xs text-red-700 dark:text-red-300">
          {report.errors.map((e) => <li key={`${e.line}-${e.message}`}>Ligne {e.line} : {e.message}</li>)}
        </ul>
      )}
      {ok && report.preview.length > 0 && (
        <ul className="mt-2 max-h-40 space-y-0.5 overflow-y-auto text-xs text-ink-2">
          {report.preview.map((p) => <li key={p}>{p}</li>)}
        </ul>
      )}
    </div>
  );
}
