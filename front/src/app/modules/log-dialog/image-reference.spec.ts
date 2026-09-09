import { stripRegistryHost } from "./image-reference";

describe('stripRegistryHost', () => {
  it('removes a host with a dot', () => {
    expect(stripRegistryHost('harbor.centralesupelec.fr/mydocker-vd-pp/test-mailhog-2'))
      .toBe('mydocker-vd-pp/test-mailhog-2');
  });

  it('removes a host with a port', () => {
    expect(stripRegistryHost('registry:5000/ns/img:1.0')).toBe('ns/img:1.0');
  });

  it('keeps a namespace that is not a host', () => {
    expect(stripRegistryHost('library/ubuntu:22.04')).toBe('library/ubuntu:22.04');
    expect(stripRegistryHost('mydocker-prod/lab')).toBe('mydocker-prod/lab');
  });

  it('keeps an image with no path at all', () => {
    expect(stripRegistryHost('ubuntu:22.04')).toBe('ubuntu:22.04');
  });

  it('removes only the host, not deeper segments', () => {
    expect(stripRegistryHost('harbor.example.org/a/b/c')).toBe('a/b/c');
  });

  it('returns an empty string for a missing image', () => {
    expect(stripRegistryHost(undefined)).toBe('');
    expect(stripRegistryHost(null)).toBe('');
    expect(stripRegistryHost('')).toBe('');
  });
});
