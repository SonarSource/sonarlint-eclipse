# Eclipse review focus

Apply these checks only when the diff touches the relevant behavior.
Leave an advisory finding only for a concrete failure supported by the changed
code and its callers; name the failing scenario and source location.
Update an existing thread for the same concern, preserve unresolved evidence,
and remain quiet when the checks pass. Do not approve, block, or modify the PR.

## OSGi runtime compatibility

- For dependency or bundle changes, distinguish build-time availability from
  runtime bundle resolution. Check imports, exports, Require-Bundle ranges,
  optional resolution, and the supported target platform only where affected.
- Start with `org.sonarlint.eclipse.core/META-INF/MANIFEST.MF` and
  `org.sonarlint.eclipse.ui/META-INF/MANIFEST.MF`, then follow the changed bundle.
- Report a missing package, incompatible range, or runtime linkage failure only
  with the affected class and a supported installation scenario. A successful
  Maven/Tycho build alone does not establish OSGi runtime availability.

## SWT execution and lifecycle

- For changed UI calls, trace whether execution reaches the SWT display thread;
  for analysis, backend waits, or I/O, check whether the display thread can block.
- Check delayed callbacks against disposal of widgets, shells, and displays.
  Identify a reachable disposed-object access instead of requesting blanket guards.
- Existing dispatch helpers are in
  `org.sonarlint.eclipse.ui/src/org/sonarlint/eclipse/ui/internal/util/DisplayUtils.java`;
  lifecycle handling also appears in
  `org.sonarlint.eclipse.ui/src/org/sonarlint/eclipse/ui/internal/util/SonarLintWebView.java`.
- Follow the established job and display conventions. Avoid unrelated rewrites
  or warnings without a runtime failure supported by the diff.
