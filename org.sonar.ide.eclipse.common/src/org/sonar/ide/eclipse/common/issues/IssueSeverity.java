package org.sonar.ide.eclipse.common.issues;

import java.util.EnumSet;

public enum IssueSeverity {
	INFO, MINOR, MAJOR, CRITICAL, BLOCKER;

	public EnumSet<IssueSeverity> getEqualOrGreaterSeverities() {
		return EnumSet.range(this, BLOCKER);
	}

	public String[] getEqualOrGreaterSeveritiesAsStringArray() {
		EnumSet<IssueSeverity> severities = getEqualOrGreaterSeverities();
		String[] result = new String[severities.size()];
		int i = 0;
		for (IssueSeverity severity : severities) {
			result[i++] = severity.name();
		}
		return result;
	}
}
