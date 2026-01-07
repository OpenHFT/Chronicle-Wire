# AGENTS.md

## Scope
- Chronicle Wire is a Java Maven project; source docs live under `src/main/docs/` in AsciiDoc.

## Build and test
- Preferred full check:
  - `mkdir -p logs`
  - `mvn verify -l logs/mvn-verify.log`
- Clean build when needed:
  - `mvn clean verify -l logs/mvn-clean-verify.log`
- Test example:
  - `mvn -Dtest=ClassName test -l logs/mvn-test.log`
- Review logs:
  - `rg -n '^\[(WARNING|ERROR)\]|SLF4J\(W\)|\bWARNING:|\bwarning:' logs/mvn-verify.log`
- Do not commit logs/.

## Constraints
- Java baseline: 8 (avoid newer language features).
- Source files must stay ISO-8859-1 (code points 0-255). Prefer ASCII; avoid smart quotes and non-breaking spaces.
- Preserve public APIs unless explicitly requested.
- Treat warnings as defects; keep logs clean.
- Never commit secrets or credentials; document security trade-offs in Javadoc or `.adoc` files.

## Docs and review checklist
- Keep AsciiDoc, tests, and code in sync; update `.adoc` files when behaviour changes.
- Javadoc must add behavioural contracts, edge cases, thread safety, units, or performance notes.
- For large mechanical changes, declare the transformation rule and keep it consistent.

## References
- `src/main/docs/decision-log.adoc` and `src/main/docs/project-requirements.adoc`.
- `OpenHFT/docs/Company-Wide-Tagging.adoc` for tagging, decision records, and AsciiDoc conventions.
