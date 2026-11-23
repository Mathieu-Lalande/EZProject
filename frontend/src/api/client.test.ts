import { api, ApiError } from './client';

describe('api client', () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
    document.cookie = 'XSRF-TOKEN=abc123';
  });

  afterEach(() => vi.unstubAllGlobals());

  it("envoie l'en-tête CSRF sur les requêtes mutantes uniquement", async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response('{}', { status: 200 })));

    await api.get('/api/projects');
    await api.post('/api/todos', { title: 'x' });

    expect(fetchMock.mock.calls[0]![1].headers).not.toHaveProperty('X-XSRF-TOKEN');
    expect(fetchMock.mock.calls[1]![1].headers).toMatchObject({ 'X-XSRF-TOKEN': 'abc123', 'Content-Type': 'application/json' });
  });

  it('transforme un Problem Detail en ApiError avec erreurs de champ', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ detail: 'Requête invalide', errors: { name: 'obligatoire' } }), { status: 400 }),
    );

    const error = await api.post('/api/projects', {}).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(400);
    expect((error as ApiError).fieldErrors).toEqual({ name: 'obligatoire' });
  });

  it('gère les réponses 204 sans corps', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    await expect(api.delete('/api/tasks/1')).resolves.toBeUndefined();
  });
});
