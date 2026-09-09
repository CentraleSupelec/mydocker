const REGISTRY_HOST = /[.:]/;

/**
 * Hides the registry host in an image reference, leaving the namespace and name.
 *
 * The rule is Docker's own: the first path segment is a registry only if it contains a dot or a
 * colon. So `harbor.example.org/ns/img` loses its host while `ns/img` and `library/ubuntu` keep
 * their namespace. The full reference stays on the wire; this is display only, because the host is
 * the first thing you want to know when an image turns out to be the wrong one.
 */
export function stripRegistryHost(image: string | undefined | null): string {
  if (!image) {
    return '';
  }

  const separator = image.indexOf('/');
  if (separator < 0) {
    return image;
  }

  const firstSegment = image.slice(0, separator);
  return REGISTRY_HOST.test(firstSegment) ? image.slice(separator + 1) : image;
}
