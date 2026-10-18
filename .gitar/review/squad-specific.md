# Eclipse review focus

- For bundle changes, check OSGi imports/exports, version ranges, and resolution on supported runtimes; a successful build does not establish runtime availability.
- Trace UI calls to the SWT display thread and expensive work away from it.
- Check delayed callbacks against widget, shell, and display disposal using existing dispatch conventions.
- Report concrete runtime or lifecycle failures with an affected class, supported scenario, and source location.
