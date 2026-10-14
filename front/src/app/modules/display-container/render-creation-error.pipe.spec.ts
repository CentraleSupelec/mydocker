import { RenderCreationErrorPipe } from "./render-creation-error.pipe";
import { TestBed } from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { IContainer } from "../shell/interfaces/container";
import { TranslateFrenchTestingModule } from 'src/testing/translate-testing.module';

// The two expectations below follow src/assets/i18n/fr.json rather than the wording this spec was
// written against: the catalogue lost a trailing period and gained "de stockage" while the suite
// was never executed.
describe("RenderCreationErrorPipe", () => {
  let pipe: RenderCreationErrorPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateFrenchTestingModule,
        RouterTestingModule,
      ],
      providers: [RenderCreationErrorPipe],
    });
    pipe = TestBed.inject(RenderCreationErrorPipe);
  });
  const defaultContainer: IContainer = {
    username: "USERNAME",
    password: "PASSWORD",
    ip: "IP_ADDRESS",
    ports: [],
    status: "KO",
    errorParams: {},
  };
  const now = new Date();
  const cloneDate = (prevDate: Date) => new Date(prevDate);

  const testCases = [
    {
      container: null,
      expected: "une erreur inconnue s'est produite",
    },
    {
      container: {
        ...defaultContainer,
        creationError: "student-volume.local-storage",
      },
      expected: "le cours est configuré avec un type de stockage de fichiers non supporté (local).",
    },
    {
      container: {
        ...defaultContainer,
        creationError: "student-volume.existing-rbd-service",
        errorParams: {},
      },
      expected: "le service  démarré récemment utilise actuellement le répertoire personnel. Il est nécessaire de l'éteindre au préalable.",
    },
    {
      container: {
        ...defaultContainer,
        creationError: "student-volume.existing-rbd-service",
        errorParams: {
          createdAt: (new Date(cloneDate(now).setMinutes(now.getMinutes() - 20))).toISOString(),
        },
      },
      expected: /le service <a href=".*"><\/a> démarré à \d{1,2}h\d{1,2}(?: (?:A|P)M)? utilise actuellement le répertoire personnel. Il est nécessaire de l'éteindre au préalable./,
    },
    {
      container: {
        ...defaultContainer,
        creationError: "student-volume.existing-rbd-service",
        errorParams: {
          createdAt: (new Date(cloneDate(now).setMonth(now.getMonth() - 1))).toISOString(),
        },
      },
      expected: /le service <a href=".*"><\/a> démarré le \d{1,2}\/\d{1,2}\/\d{4} à \d{1,2}h\d{1,2}(?: (?:A|P)M)? utilise actuellement le répertoire personnel. Il est nécessaire de l'éteindre au préalable./,
    },
    {
      container: {
        ...defaultContainer,
        creationError: "unknown",
        errorParams: {},
      },
      expected: "le cours a rencontré un problème : \"unknown\"",
    },
  ];
  testCases.forEach(((testCase, index) => {
    it(`should render ${index}`, () => {
      if (typeof testCase.expected === "string") {
        const actual = document.createElement("span");
        actual.innerHTML = pipe.transform(testCase.container) || '';
        expect(actual.innerText).toEqual(testCase.expected);
      } else {
        expect(pipe.transform(testCase.container)).toMatch(testCase.expected);
      }
    });
  }));
});
