import { TestBed } from "@angular/core/testing";

import { RenderStringService } from "./render-string.service";
import { ConnectionType } from "../ports-form/interfaces/port";

describe("RenderStringService", () => {
  let service: RenderStringService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
    });
    service = TestBed.inject(RenderStringService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
  const ports = [{
    description: "http",
    mapPort: 8888,
    portMapTo: 8888,
    connectionType: ConnectionType.HTTP,
    requiredToAccessContainer: false,
    hostname: "hostname.com",
  }, {
    description: "http",
    mapPort: 8890,
    portMapTo: 8890,
    connectionType: ConnectionType.HTTP,
    requiredToAccessContainer: false,
  }, {
    description: "http",
    mapPort: 8889,
    portMapTo: 8889,
    connectionType: ConnectionType.OTHER,
    requiredToAccessContainer: false,
  }];

  it("should be created", () => {
    expect(
      service.renderString(
        "http://{{IP}}:{{PORT['8888']}}/?token={{PASSWORD}}",
        ports, "USERNAME", "PASSWORD", "IP_ADDRESS"),
    ).toEqual("http://IP_ADDRESS:8888/?token=PASSWORD");
    expect(
      service.renderString(
        "http://{{HOST['8889']}}/?token={{PASSWORD}}",
        ports, "USERNAME", "PASSWORD", "IP_ADDRESS"),
    ).toEqual("HOST est uniquement pour les ports de type HTTP");
    expect(
      service.renderString(
        "http://{{HOST['8890']}}/?token={{PASSWORD}}",
        ports, "USERNAME", "PASSWORD", "IP_ADDRESS"),
    ).toEqual("https://generated-hostname.mydocker.com/?token=PASSWORD");
    expect(
      service.renderString(
        "http://{{HOST['8888']}}/?token={{PASSWORD}}",
        ports, "USERNAME", "PASSWORD", "IP_ADDRESS"),
    ).toEqual("https://hostname.com/?token=PASSWORD");
  });
});
