/*
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.github.caciocavallosilano.cacio.peer;

// import java.awt.Dimension;
// import java.awt.Event;
// import java.awt.PopupMenu;
import java.awt.peer.PopupMenuPeer;
// import javax.swing.JMenu;
// import javax.swing.JPopupMenu;
// import javax.swing.event.*;
// import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

class CacioPopupMenuPeer extends CacioMenuPeer implements PopupMenuPeer {

    CacioPopupMenuPeer(PopupMenu m) {
        super(m);
    }

    public void show(Event e) {
        JMenu m = (JMenu) getSwingMenu();
        JPopupMenu pm = m.getPopupMenu();
        Dimension d = pm.getPreferredSize();
        // TODO: Fix location relative to target.
        pm.setLocation(e.x, e.y);
        pm.setSize(d.width, d.height);
        pm.setVisible(true);
        // TODO: Add listener for closing the popup menu.

        //addGlobalMouseListener(pm);
        // Add listener for closing the popup menu when clicking elsewhere
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (!pm.getBounds().contains(mouseEvent.getPoint())) {
                    pm.setVisible(false);
                    pm.removeMouseListener(this); 
                }
            }
        };
        pm.addMouseListener(mouseAdapter);
    }

    private void addGlobalMouseListener(JPopupMenu popupMenu) {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event instanceof MouseEvent) {
                    MouseEvent mouseEvent = (MouseEvent) event;
                    if (mouseEvent.getID() == MouseEvent.MOUSE_CLICKED) {
                        if (!isClickInsidePopupMenu(popupMenu, mouseEvent)) {
                            popupMenu.setVisible(false);
                            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
                        }
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private boolean isClickInsidePopupMenu(JPopupMenu popupMenu, MouseEvent e) {
        Point point = e.getPoint();
        Point menuLocation = popupMenu.getLocationOnScreen();
        Rectangle menuBounds = new Rectangle(menuLocation.x, menuLocation.y,
                popupMenu.getWidth(), popupMenu.getHeight());
        return menuBounds.contains(point);
    }

}
