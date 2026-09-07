# COMMIT 010 — Raw / SHA-256 / Gate R2

COMMIT: 010 — Raw / SHA-256 / Gate R2
STATUS: PASS
GIT HASH: dae308e
TESTS EXECUTED: `gradle test`, `assembleOperationalDebug`, `assembleLabDebug`, GitHub Actions run `34171555877`
TEST RESULTS: PASS
APKS GENERATED: operationalDebug, labDebug
KNOWN LIMITATIONS: end-to-end physical raw import and process-death validation remain to be exercised on the device.
DECISIONS: raw files are copied through `.part`, hashed in streaming, validated by size, content-addressed and immutable.
BLOCKERS: none for the raw storage baseline
NEXT COMMIT: Gate R2 end-to-end validation
