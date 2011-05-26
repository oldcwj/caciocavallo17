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
package net.java.openjdk.awt.peer.web;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

import sun.java2d.SurfaceData;
import sun.java2d.loops.SurfaceType;

/**
 * SurfaceData implementation based on libSDL.
 * 
 * @author Mario Torre <neugens.limasoftware@gmail.com>
 */
public class WebSurfaceData extends SurfaceData {

    static SurfaceType typeDefault = SurfaceType.IntRgb.deriveSubType("Cacio SDL default");

    static {
	initIDs();
    }

    //TODO: Nicht mehr static!!!
    public ArrayList<Rectangle> dirtyRects = new ArrayList<Rectangle>();
    public BufferedImage imgBuffer;
    
    private Rectangle bounds;
    private GraphicsConfiguration configuration;
    private Object destination;

    protected WebSurfaceData(SurfaceType surfaceType, ColorModel cm, Rectangle b, GraphicsConfiguration gc, Object dest) {

	super(surfaceType, cm);

	bounds = b;
	configuration = gc;
	destination = dest;

	imgBuffer = new BufferedImage(b.width, b.height, BufferedImage.TYPE_INT_RGB);
	Graphics g = imgBuffer.getGraphics();
	g.setColor(Color.WHITE);
	g.fillRect(0, 0, b.width, b.height);
	int[] data = ((DataBufferInt) imgBuffer.getRaster().getDataBuffer()).getData();

	initOps(data, b.width, b.height, 0);
    }

    @Override
    public SurfaceData getReplacement() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
	return configuration;
    }

    @Override
    public Raster getRaster(int arg0, int arg1, int arg2, int arg3) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Rectangle getBounds() {
	return new Rectangle(bounds);
    }

    @Override
    public Object getDestination() {
	return destination;
    }

    private native final void initOps(int[] data, int width, int height, int stride);

    private static final native void initIDs();

    public void addDirtyRect(int x1, int x2, int y1, int y2) {
	synchronized (dirtyRects) {
	    Rectangle dirtyRect = new Rectangle(x1, y1, (x2 - x1), (y2 - y1));
	    dirtyRects.add(dirtyRect);
	}
    }

    // if(dirtyRects.size() >= 100) {
    // Rectangle unionRect = dirtyRects.get(0);
    // for(Rectangle rect : dirtyRects) {
    // unionRect = unionRect.union(rect);
    // }
    //
    // dirtyRects.clear();
    // System.out.println("Union-Dirty-Rect was: "+unionRect);
    // }
}
