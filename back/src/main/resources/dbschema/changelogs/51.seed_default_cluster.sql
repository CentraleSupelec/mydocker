-- Changeset 51: the "default" cluster of a platform upgrading from one Go API.
-- In its own file so the recorded checksum covers this text, not the substituted values:
-- the seed settings may change or disappear after the seed has run.
INSERT INTO clusters (name, go_address, go_port, terraform_state_url, deployment_enabled)
SELECT 'default',
       '${defaultClusterGoAddress}',
       ${defaultClusterGoPort},
       NULLIF('${defaultClusterTerraformStateUrl}', ''),
       ${defaultClusterDeploymentEnabled} AND '${defaultClusterTerraformStateUrl}' <> ''
WHERE NOT EXISTS (SELECT 1 FROM clusters)
  AND EXISTS (SELECT 1 FROM courses)
