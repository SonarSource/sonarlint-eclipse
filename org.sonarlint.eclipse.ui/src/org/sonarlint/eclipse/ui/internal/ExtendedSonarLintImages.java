package org.sonarlint.eclipse.ui.internal;

import java.util.Locale;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils.FindingMatchingStatus;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ImpactSeverity;
import org.sonarsource.sonarlint.core.rpc.protocol.common.IssueSeverity;
import org.sonarsource.sonarlint.core.rpc.protocol.common.RuleType;

/**
 *  Purpose of this class is to remove the dependencies to "SonarLint Core" from the base "SonarLintImages" class so it
 *  can be imported safely in the "org.sonarlint.eclipse.jdt" bundle.
 */
public final class ExtendedSonarLintImages extends SonarLintImages {
  private ExtendedSonarLintImages() {
    super();
  }

  /**
   *  Mapping of the matching status of an issue to the specific image
   *
   *  @param matchingStatus specific matching status of an issue
   *  @param isResolved resolution status of an issue
   *  @return the corresponding connection mode icon (including standalone)
   */
  private static ImageDescriptor matchingStatusToImageDescriptor(FindingMatchingStatus matchingStatus,
    boolean isResolved) {
    if (matchingStatus == FindingMatchingStatus.NOT_MATCHED) {
      return isResolved ? STANDALONE_RESOLVED_16 : STANDALONE_16;
    } else if (matchingStatus == FindingMatchingStatus.MATCHED_WITH_SC) {
      return isResolved ? SONARCLOUD_RESOLVED_16 : SONARCLOUD_16;
    }
    return isResolved ? SONARQUBE_RESOLVED_16 : SONARQUBE_16;
  }

  /**
   *  Create a composite image with the markers' project connection mode, issue severity and type
   *
   *  @param matchingStatus matching status of an issue (nullable when grouped)
   *  @param issueSeverity issue severity (NOT impact severity)
   *  @param type issue type
   *  @param isResolved issue status
   *  @return composite image if found, null otherwise
   */
  @Nullable
  public static Image getIssueImage(@Nullable FindingMatchingStatus matchingStatus, String issueSeverity,
    @Nullable String type, boolean isResolved) {
    var key = matchingStatus + "/" + issueSeverity + "/" + type + "/" + isResolved;
    var imageRegistry = SonarLintUiPlugin.getDefault().getImageRegistry();
    var image = imageRegistry.get(key);
    if (image == null) {
      ImageDescriptor matchingStatusImage = null;
      if (matchingStatus != null) {
        matchingStatusImage = matchingStatusToImageDescriptor(matchingStatus, isResolved);
      }
      var severityImage = getImpactSeverityImageDescriptor(issueSeverity);
      ImageDescriptor typeImage = null;
      if (type != null) {
        typeImage = createImageDescriptor("type/" + type.toLowerCase(Locale.ENGLISH) + ".png");
      }
      imageRegistry.put(key, new CompositeIssueImage(matchingStatusImage, severityImage, typeImage));
    }
    return imageRegistry.get(key);
  }

  /**
   *  Create a composite image for the new clean code taxonomy with the markers' connection mode and highest impact
   *
   *  @param matchingStatus matching status of an issue (nullable when grouped)
   *  @param impact highest issue impact
   *  @param isResolved issue status
   *  @return composite image if found, null otherwise
   */
  @Nullable
  public static Image getIssueImage(@Nullable FindingMatchingStatus matchingStatus, String impact,
    boolean isResolved) {
    var key = matchingStatus + "/" + impact + "/" + isResolved;
    var imageRegistry = SonarLintUiPlugin.getDefault().getImageRegistry();
    var image = imageRegistry.get(key);
    if (image == null) {
      ImageDescriptor matchingStatusImage = null;
      if (matchingStatus != null) {
        matchingStatusImage = matchingStatusToImageDescriptor(matchingStatus, isResolved);
      }
      var impactImage = createImageDescriptor(IMPACT_FOLDER_PREFIX + impact.toLowerCase(Locale.ENGLISH) + ".png");
      imageRegistry.put(key, new CompositeIssueImage(matchingStatusImage, impactImage, null));
    }
    return imageRegistry.get(key);
  }

  @Nullable
  public static ImageDescriptor getImpactSeverityImageDescriptor(String issueSeverity) {
    ImpactSeverity impactSeverity;
    if (IssueSeverity.BLOCKER.name().equals(issueSeverity)) {
      impactSeverity = ImpactSeverity.BLOCKER;
    } else if (IssueSeverity.CRITICAL.name().equals(issueSeverity)) {
      impactSeverity = ImpactSeverity.HIGH;
    } else if (IssueSeverity.MAJOR.name().equals(issueSeverity)) {
      impactSeverity = ImpactSeverity.MEDIUM;
    } else if (IssueSeverity.MINOR.name().equals(issueSeverity)) {
      impactSeverity = ImpactSeverity.LOW;
    } else {
      impactSeverity = ImpactSeverity.INFO;
    }
    return createImageDescriptor(IMPACT_FOLDER_PREFIX + impactSeverity.name().toLowerCase(Locale.ENGLISH) + ".png");
  }

  @Nullable
  public static Image getSeverityImage(IssueSeverity severity) {
    ImpactSeverity impactSeverity;
    switch (severity) {
      case BLOCKER:
        impactSeverity = ImpactSeverity.BLOCKER;
        break;
      case CRITICAL:
        impactSeverity = ImpactSeverity.HIGH;
        break;
      case MAJOR:
        impactSeverity = ImpactSeverity.MEDIUM;
        break;
      case MINOR:
        impactSeverity = ImpactSeverity.LOW;
        break;
      default:
        impactSeverity = ImpactSeverity.INFO;
    }
    return getImpactImage(impactSeverity);
  }

  @Nullable
  public static Image getTypeImage(RuleType type) {
    return createImage("type/" + type.name().toLowerCase(Locale.ENGLISH) + ".png");
  }

  @Nullable
  public static Image getImpactImage(ImpactSeverity impact) {
    return createImage(IMPACT_FOLDER_PREFIX + impact.name().toLowerCase(Locale.ENGLISH) + ".png");
  }
}
