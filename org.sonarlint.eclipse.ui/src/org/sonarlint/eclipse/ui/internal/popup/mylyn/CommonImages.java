/*******************************************************************************
 * Copyright (c) 2004, 2015 Tasktop Technologies and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.sonarlint.eclipse.ui.internal.popup.mylyn;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

/**
 * @author Mik Kersten
 * @since 3.7
 */
public class CommonImages {

    private static final URL baseUrl;

    static {
        Bundle bundle = Platform.getBundle(CommonsUiConstants.ID_PLUGIN);
        if (bundle != null) {
            baseUrl = bundle.getEntry("/icons/"); //$NON-NLS-1$
        } else {
            URL iconsUrl = null;
            try {
                // lookup location of CommonImages class on disk
                iconsUrl = new URL(CommonImages.class.getResource("CommonImages.class"), "../../../../../../icons/"); //$NON-NLS-1$ //$NON-NLS-2$
            } catch (MalformedURLException e) {
                // ignore
            }
            baseUrl = iconsUrl;
        }
    }

    private static ImageRegistry imageRegistry;

    private static final String T_ELCL = "elcl16"; //$NON-NLS-1$

    private static final String T_EVIEW = "eview16"; //$NON-NLS-1$

    private static final String T_TOOL = "etool16"; //$NON-NLS-1$

    private static final String T_OBJ = "obj16"; //$NON-NLS-1$

    private static final String T_OBJ_32 = "obj32"; //$NON-NLS-1$

    private static final String T_OBJ48 = "obj48"; //$NON-NLS-1$

    private static final String T_WIZBAN = "wizban"; //$NON-NLS-1$

    private static final String T_OVR = "ovr16"; //$NON-NLS-1$

    // Priorities

    public static final ImageDescriptor PRIORITY_1 = create(T_OBJ, "priority-1.gif"); //$NON-NLS-1$

    public static final ImageDescriptor PRIORITY_2 = create(T_OBJ, "priority-2.gif"); //$NON-NLS-1$

    public static final ImageDescriptor PRIORITY_3 = create(T_OBJ, "priority-3.gif"); //$NON-NLS-1$

    public static final ImageDescriptor PRIORITY_4 = create(T_OBJ, "priority-4.gif"); //$NON-NLS-1$

    public static final ImageDescriptor PRIORITY_5 = create(T_OBJ, "priority-5.gif"); //$NON-NLS-1$

    public static final ImageDescriptor PRIORITY_1_LARGE = create(T_OBJ_32, "priority-critical.png"); //$NON-NLS-1$

    public static final ImageDescriptor PRIORITY_2_LARGE = create(T_OBJ_32, "priority-high.png"); //$NON-NLS-1$

    public static final ImageDescriptor PRIORITY_3_LARGE = create(T_OBJ_32, "priority-normal.png"); //$NON-NLS-1$

    public static final ImageDescriptor PRIORITY_4_LARGE = create(T_OBJ_32, "priority-low.png"); //$NON-NLS-1$

    public static final ImageDescriptor PRIORITY_5_LARGE = create(T_OBJ_32, "priority-very-low.png"); //$NON-NLS-1$

    //  Calendars, people and notifications

    public static final ImageDescriptor CALENDAR = create(T_TOOL, "calendar.gif"); //$NON-NLS-1$

    public static final ImageDescriptor CALENDAR_SMALL = create(T_OBJ, "calendar-small.gif"); //$NON-NLS-1$

    public static final ImageDescriptor SCHEDULE = create(T_TOOL, "schedule.png"); //$NON-NLS-1$

    public static final ImageDescriptor SCHEDULE_DAY = create(T_TOOL, "schedule-day.png"); //$NON-NLS-1$

    public static final ImageDescriptor SCHEDULE_WEEK = create(T_TOOL, "schedule-week.png"); //$NON-NLS-1$

    public static final ImageDescriptor PERSON = create(T_TOOL, "person.gif"); //$NON-NLS-1$

    public static final ImageDescriptor PERSON_NARROW = create(T_TOOL, "person-narrow.gif"); //$NON-NLS-1$

    public static final ImageDescriptor PERSON_ME = create(T_TOOL, "person-me.gif"); //$NON-NLS-1$

    public static final ImageDescriptor PERSON_ME_SMALL = create(T_TOOL, "person-me-small.png"); //$NON-NLS-1$

    public static final ImageDescriptor PERSON_ME_NARROW = create(T_TOOL, "person-me-narrow.gif"); //$NON-NLS-1$

    public static final ImageDescriptor NOTIFICATION_CLOSE = create(T_EVIEW, "notification-close.gif"); //$NON-NLS-1$

    public static final ImageDescriptor NOTIFICATION_CLOSE_HOVER = create(T_EVIEW, "notification-close-active.gif"); //$NON-NLS-1$

    public static final ImageDescriptor NOTIFICATION_PREFERENCES_HOVER = create(T_EVIEW,
            "notification-preferences-active.png"); //$NON-NLS-1$

    public static final ImageDescriptor NOTIFICATION_PREFERENCES = create(T_EVIEW,
            "notification-preferences-inactive.png"); //$NON-NLS-1$

    // Date and synchronization overlays

    public static final ImageDescriptor OVERLAY_DATE_DUE = create(T_EVIEW, "overlay-has-due.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_DATE_OVERDUE = create(T_EVIEW, "overlay-overdue.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SYNC_IN_PROGRESS = create(T_EVIEW, "overlay-synchronizing.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SYNC_INCOMMING = create(T_EVIEW, "overlay-incoming.gif"); //$NON-NLS-1$

    /**
     * @since 3.18
     */
    public static final ImageDescriptor OVERLAY_SYNC_INCOMING_REVIEWS = create(T_EVIEW, "overlay-incoming-reviews.png"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SYNC_INCOMMING_NEW = create(T_EVIEW, "overlay-incoming-new.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SYNC_OUTGOING = create(T_EVIEW, "overlay-outgoing.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SYNC_OUTGOING_NEW = create(T_EVIEW, "overlay-outgoing-new.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SYNC_CONFLICT = create(T_EVIEW, "overlay-conflict.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SYNC_WARNING = create(T_OVR, "overlay-sync-warning.gif"); //$NON-NLS-1$

    /**
     * @since 3.9
     */
    public static final ImageDescriptor OVERLAY_SYNC_ERROR = create(T_OVR, "overlay-sync-error.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_WARNING = create(T_OVR, "warning_ovr.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SUCCESS = create(T_OVR, "success_ovr.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_ERROR = create(T_OVR, "error_ovr.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_FAILED = create(T_OVR, "failed_ovr.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SYNC_OLD_INCOMMING = create(T_EVIEW, "overlay-synch-incoming.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SYNC_OLD_INCOMMING_NEW = create(T_EVIEW,
            "overlay-synch-incoming-new.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_SYNC_OLD_OUTGOING = create(T_EVIEW, "overlay-synch-outgoing.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_CLEAR = create(T_OVR, "overlay-blank.gif"); //$NON-NLS-1$

    public static final ImageDescriptor OVERLAY_WHITE = create(T_OVR, "solid-white.gif"); //$NON-NLS-1$

    // Wizard banners

    public static final ImageDescriptor BANNER_SCREENSHOT = create(T_WIZBAN, "banner-screenshot.png"); //$NON-NLS-1$

    public static final ImageDescriptor BANNER_IMPORT = create(T_WIZBAN, "banner-import.gif"); //$NON-NLS-1$

    public static final ImageDescriptor BANNER_EXPORT = create(T_WIZBAN, "banner-export.gif"); //$NON-NLS-1$

    public static final ImageDescriptor BANNER_SECURE_ROLE = create(T_WIZBAN, "secur_role_wiz.gif"); //$NON-NLS-1$

    // Discovery

    public static final ImageDescriptor DISCOVERY = create(T_TOOL, "discovery.png"); //$NON-NLS-1$

    public static final ImageDescriptor BANNER_DISOVERY = create(T_WIZBAN, "banner-discovery.png"); //$NON-NLS-1$

    // Miscellaneous
    // TODO: some of the common images below come from the workbench

    public static final ImageDescriptor COMPLETE = create(T_OBJ, "complete.gif"); //$NON-NLS-1$

    public static final ImageDescriptor CHECKED = create(T_OBJ, "checked.gif"); //$NON-NLS-1$

    public static final ImageDescriptor REMOVE = create(T_ELCL, "remove.gif"); //$NON-NLS-1$

    public static final ImageDescriptor DELETE = create(T_ELCL, "delete.gif"); //$NON-NLS-1$

    /**
     * @since 3.9
     */
    public static final ImageDescriptor ERROR = create(T_ELCL, "error.gif"); //$NON-NLS-1$

    public static final ImageDescriptor WARNING = create(T_ELCL, "warning.gif"); //$NON-NLS-1$

    public static final ImageDescriptor FILTER_COMPLETE = create(T_ELCL, "filter-complete.gif"); //$NON-NLS-1$

    /**
     * @since 3.11
     */
    public static final ImageDescriptor FILTER_MY_TASKS = create(T_ELCL, "filter-mytasks.png"); //$NON-NLS-1$

    public static final ImageDescriptor FILTER_ARCHIVE = create(T_ELCL, "filter-archive.gif"); //$NON-NLS-1$

    public static final ImageDescriptor FILTER_PRIORITY = create(T_ELCL, "filter-priority.gif"); //$NON-NLS-1$

    public static final ImageDescriptor COLOR_PALETTE = create(T_ELCL, "color-palette.gif"); //$NON-NLS-1$

    public static final ImageDescriptor FILTER = create(T_TOOL, "view-filter.gif"); //$NON-NLS-1$

    public static final ImageDescriptor FIND_CLEAR = create(T_TOOL, "find-clear.gif"); //$NON-NLS-1$

    public static final ImageDescriptor FIND_CLEAR_DISABLED = create(T_TOOL, "find-clear-disabled.gif"); //$NON-NLS-1$

    public static final ImageDescriptor BROWSER_SMALL = create(T_OBJ, "browser-small.gif"); //$NON-NLS-1$

    public static final ImageDescriptor BROWSER_OPEN_TASK = create(T_TOOL, "open-browser.gif"); //$NON-NLS-1$

    public static final ImageDescriptor TOOLBAR_ARROW_RIGHT = create(T_TOOL, "toolbar-arrow-right.gif"); //$NON-NLS-1$

    public static final ImageDescriptor TOOLBAR_ARROW_DOWN = create(T_TOOL, "toolbar-arrow-down.gif"); //$NON-NLS-1$

    public static final ImageDescriptor LINK_EDITOR = create(T_TOOL, "link-editor.gif"); //$NON-NLS-1$

    public static final ImageDescriptor CLEAR = create(T_TOOL, "clear.gif"); //$NON-NLS-1$

    public static final ImageDescriptor EDIT = create(T_TOOL, "edit.gif"); //$NON-NLS-1$

    public static final ImageDescriptor EDIT_SMALL = create(T_TOOL, "edit-small.png"); //$NON-NLS-1$

    public static final ImageDescriptor CUT = create(T_TOOL, "cut.gif"); //$NON-NLS-1$

    public static final ImageDescriptor UNDO = create(T_TOOL, "undo_edit.gif"); //$NON-NLS-1$

    public static final ImageDescriptor REDO = create(T_TOOL, "redo_edit.gif"); //$NON-NLS-1$

    public static final ImageDescriptor STATUS_NORMAL = create(T_EVIEW, "status-normal.gif"); //$NON-NLS-1$

    public static final ImageDescriptor STATUS_CONTEXT = create(T_EVIEW, "status-server-context.gif"); //$NON-NLS-1$

    public static final ImageDescriptor PRESENTATION = create(T_TOOL, "presentation.gif"); //$NON-NLS-1$

    public static final ImageDescriptor GROUPING = create(T_TOOL, "grouping.gif"); //$NON-NLS-1$

    public static final ImageDescriptor COPY = create(T_TOOL, "copy.png"); //$NON-NLS-1$

    public static final ImageDescriptor GO_UP = create(T_TOOL, "go-up.gif"); //$NON-NLS-1$

    public static final ImageDescriptor GO_INTO = create(T_TOOL, "go-into.gif"); //$NON-NLS-1$

    public static final ImageDescriptor REFRESH = create(T_ELCL, "refresh.gif"); //$NON-NLS-1$

    public static final ImageDescriptor REFRESH_SMALL = create(T_ELCL, "refresh-small.gif"); //$NON-NLS-1$

    public static final ImageDescriptor COLLAPSE_ALL = create(T_ELCL, "collapseall.png"); //$NON-NLS-1$

    public static final ImageDescriptor COLLAPSE_ALL_SMALL = create(T_ELCL, "collapseall-small.png"); //$NON-NLS-1$

    public static final ImageDescriptor EXPAND_ALL = create(T_ELCL, "expandall.gif"); //$NON-NLS-1$

    public static final ImageDescriptor EXPAND = create(T_ELCL, "expand.gif"); //$NON-NLS-1$

    public static final ImageDescriptor EXPAND_ALL_SMALL = create(T_ELCL, "expandall-small.png"); //$NON-NLS-1$

    public static final ImageDescriptor BLANK = create(T_ELCL, "blank.gif"); //$NON-NLS-1$

    public static final ImageDescriptor IMAGE_CAPTURE = create(T_TOOL, "capture-screen.png"); //$NON-NLS-1$

    public static final ImageDescriptor IMAGE_FIT = create(T_TOOL, "capture-fit.png"); //$NON-NLS-1$

    public static final ImageDescriptor IMAGE_FILE = create(T_OBJ, "file-image.gif"); //$NON-NLS-1$

    public static final ImageDescriptor FILE_PLAIN = create(T_OBJ, "file-plain.png"); //$NON-NLS-1$

    public static final ImageDescriptor FILE_PLAIN_SMALL = create(T_OBJ, "file-small.png"); //$NON-NLS-1$

    public static final ImageDescriptor NOTES_SMALL = create(T_OBJ, "notes-small.png"); //$NON-NLS-1$

    public static final ImageDescriptor QUESTION = create(T_OBJ, "question.gif"); //$NON-NLS-1$

    public static final ImageDescriptor INFORMATION = create(T_OBJ, "message_info.gif"); //$NON-NLS-1$

    public static final ImageDescriptor SEPARATOR_LIST = create(T_TOOL, "content-assist-separator.gif"); //$NON-NLS-1$

    public static final ImageDescriptor PART_MAXIMIZE = create(T_TOOL, "maximize.png"); //$NON-NLS-1$

    public static final ImageDescriptor PREVIEW_WEB = create(T_TOOL, "preview-web.png"); //$NON-NLS-1$

    public static final ImageDescriptor WEB = create(T_TOOL, "web.png"); //$NON-NLS-1$

    public static final ImageDescriptor FIND = create(T_TOOL, "find.gif"); //$NON-NLS-1$

    public static final ImageDescriptor SAVE = create(T_TOOL, "save.gif"); //$NON-NLS-1$;

    public static final ImageDescriptor VALIDATE = create(T_OBJ, "resource_obj.gif"); //$NON-NLS-1$

    public static final ImageDescriptor NOTIFICATION_CONFIGURE = create(T_TOOL, "notification-configure.gif"); //$NON-NLS-1$;

    public static final ImageDescriptor NOTIFICATION_CONFIGURE_HOVER = create(T_TOOL,
            "notification-configure-active.gif"); //$NON-NLS-1$;

    public static final ImageDescriptor CHECKBOX_CLEARED = create(T_ELCL, "checkboxcleared.gif"); //$NON-NLS-1$;

    public static final ImageDescriptor CHECKBOX_SELECTED = create(T_ELCL, "checkboxselected.gif"); //$NON-NLS-1$;

    public static final ImageDescriptor CHECKBOX_UNSELECTED = create(T_ELCL, "checkboxunselected.gif"); //$NON-NLS-1$;

    public static final ImageDescriptor CHECKBOX_DISABLED = create(T_ELCL, "checkboxgreyed.gif"); //$NON-NLS-1$;

    public static final ImageDescriptor PERSON_LARGE = create(T_OBJ48, "person.gif"); //$NON-NLS-1$;

    private static ImageDescriptor create(String prefix, String name) {
        try {
            return ImageDescriptor.createFromURL(makeIconFileURL(prefix, name));
        } catch (MalformedURLException e) {
            return ImageDescriptor.getMissingImageDescriptor();
        }
    }

    /**
     * Lazily initializes image map.
     *
     * @param imageDescriptor
     * @return Image
     */
    public static Image getImage(ImageDescriptor imageDescriptor) {
        ImageRegistry imageRegistry = getImageRegistry();
        Image image = imageRegistry.get("" + imageDescriptor.hashCode()); //$NON-NLS-1$
        if (image == null) {
            image = imageDescriptor.createImage(true);
            imageRegistry.put("" + imageDescriptor.hashCode(), image); //$NON-NLS-1$
        }
        return image;
    }

    private static ImageRegistry getImageRegistry() {
        if (imageRegistry == null) {
            imageRegistry = new ImageRegistry();
        }

        return imageRegistry;
    }

    private static URL makeIconFileURL(String prefix, String name) throws MalformedURLException {
        if (baseUrl == null) {
            throw new MalformedURLException();
        }

        StringBuffer buffer = new StringBuffer(prefix);
        buffer.append('/');
        buffer.append(name);
        return new URL(baseUrl, buffer.toString());
    }

}
