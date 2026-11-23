// Applique le thème avant le rendu pour éviter un flash (fichier externe : compatible avec une CSP stricte)
try {
  const t = localStorage.getItem('ez-theme');
  if (t === 'dark' || (!t && matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.documentElement.classList.add('dark');
  }
} catch {
  // stockage indisponible : thème clair
}
