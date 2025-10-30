# Chronicle-Wire Follow-up Tasks

Chronicle Software

## Code-Review Profile

- [ ] Chronicle-Wire: SpotBugs suppressions currently mask ≈285 findings (AA assertions, NP hot paths, constructor hygiene); triage and replace with real fixes where feasible.
- [ ] Chronicle-Wire: Current coverage 69.8% lines / 64.1% branches (JaCoCo thresholds set to 0.0/0.0 for code-review); raise gates once new tests land.
- [ ] Chronicle-Wire: Re-enable PMD check (currently skipped due to PMD 7.17 parsing bug on `YamlWireOut.typePrefix`) after upgrading or adding targeted excludes.
