import { initials, parseDate, relativeDays, toIsoDate } from './format';

describe('format', () => {
  it('parse une date ISO sans décalage de fuseau', () => {
    expect(toIsoDate(parseDate('2026-12-18'))).toBe('2026-12-18');
  });

  it('formate les jours restants', () => {
    expect(relativeDays(null)).toBe('Sans échéance');
    expect(relativeDays(0)).toBe("Échéance aujourd'hui");
    expect(relativeDays(-3)).toBe('Dépassé de 3 j');
    expect(relativeDays(12)).toBe('J-12');
  });

  it('calcule les initiales', () => {
    expect(initials('Léa Dubois')).toBe('LD');
    expect(initials('Camille Anne Martin')).toBe('CA');
  });
});
