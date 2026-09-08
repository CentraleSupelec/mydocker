import { TranslatedPipe } from './translated.pipe';
import { Language } from '../../app-config';

describe('TranslatedPipe', () => {
  let pipe: TranslatedPipe;

  beforeEach(() => {
    pipe = new TranslatedPipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('returns the requested language', () => {
    const title = { [Language.FR]: 'Documentation', [Language.EN]: 'Docs' };
    expect(pipe.transform(title, Language.EN)).toBe('Docs');
    expect(pipe.transform(title, Language.FR)).toBe('Documentation');
  });

  it('falls back to another language when the requested one is missing', () => {
    const frenchOnly = { [Language.FR]: 'Connexion universitaire' } as any;
    expect(pipe.transform(frenchOnly, Language.EN)).toBe('Connexion universitaire');

    const englishOnly = { [Language.EN]: 'University login' } as any;
    expect(pipe.transform(englishOnly, Language.FR)).toBe('University login');
  });

  it('accepts a plain string, as declared by a platform provisioned before the i18n migration', () => {
    expect(pipe.transform('CONNEXION AVEC CENTRALESUPELEC', Language.EN))
      .toBe('CONNEXION AVEC CENTRALESUPELEC');
  });

  it('returns an empty string for a missing or empty value', () => {
    expect(pipe.transform(undefined, Language.FR)).toBe('');
    expect(pipe.transform(null, Language.FR)).toBe('');
    expect(pipe.transform({} as any, Language.FR)).toBe('');
  });
});
