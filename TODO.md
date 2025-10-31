# Chronicle-Wire Follow-up Tasks

Chronicle Software

## Code-Review Profile

- [ ] Chronicle-Wire: SpotBugs suppressions currently mask ≈285 findings (AA assertions, NP hot paths, constructor hygiene); triage and replace with real fixes where feasible.
- [ ] Chronicle-Wire: Current coverage 69.8% lines / 64.1% branches (JaCoCo thresholds set to 0.0/0.0 for code-review); raise gates once new tests land.
- [ ] Chronicle-Wire: Monitor PMD-4578 (nested generic parsing crash); remove `pom.xml` PMD excludes for TextWire/YamlWireOut once upstream ships a fix.
