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
    jasmine.clock().install();
    jasmine.clock().mockDate(now);
  });

  afterEach(() => {
    jasmine.clock().uninstall();
  });
  const defaultContainer: IContainer = {
    username: "USERNAME",
    password: "PASSWORD",
    ip: "IP_ADDRESS",
    ports: [],
    status: "KO",
    errorParams: {},
  };
  // Midday, and the same instant the pipe reads: with the real clock, a start 20 minutes earlier
  // falls on the previous day between 00:00 and 00:20 and the pipe prefixes the date.
  const now = new Date(2026, 5, 15, 12, 0, 0);
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

  it("renders the technical detail of a start that left no container", () => {
    // Called loosely so that this spec still compiles, and fails alone, against a pipe without the detail.
    const transform = pipe.transform.bind(pipe) as (container: IContainer | null, detail?: string) => string;
    const actual = document.createElement("span");
    actual.innerHTML = transform(null, "Http failure response: 500 Internal Server Error") || '';
    expect(actual.innerText).toEqual("le cours a rencontré un problème : \"Http failure response: 500 Internal Server Error\"");
  });

  it("renders the detail of a failed start rather than its error key", () => {
    const actual = document.createElement("span");
    actual.innerHTML = pipe.transform({
      ...defaultContainer,
      creationError: "container.start-failed",
      errorParams: { detail: "invalid memory value 1048576: Must be at least 4MiB" },
    }) || '';
    expect(actual.innerText).toContain("invalid memory value 1048576: Must be at least 4MiB");
    expect(actual.innerText).not.toContain("container.start-failed");
  });

  it("renders an unknown error for a failed start without detail", () => {
    const actual = document.createElement("span");
    actual.innerHTML = pipe.transform({
      ...defaultContainer,
      creationError: "container.start-failed",
      errorParams: {},
    }) || '';
    expect(actual.innerText).toEqual("une erreur inconnue s'est produite");
  });

  it("still renders an unknown error key as is", () => {
    const actual = document.createElement("span");
    actual.innerHTML = pipe.transform({
      ...defaultContainer,
      creationError: "boom",
      errorParams: {},
    }) || '';
    expect(actual.innerText).toEqual("le cours a rencontré un problème : \"boom\"");
  });
});
