/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.github.caciocavallosilano.cacio.ctc;

import com.github.caciocavallosilano.cacio.ctc.junit.CTCInterceptor;
import com.github.caciocavallosilano.cacio.peer.PlatformWindowFactory;
import com.github.caciocavallosilano.cacio.peer.managed.FullScreenWindowFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.agent.ByteBuddyAgent;

import javax.swing.plaf.metal.MetalLookAndFeel;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.*;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

public class CTCPreloadClassLoader extends URLClassLoader {
    // https://stackoverflow.com/a/56043252/1050369
    private static final VarHandle MODIFIERS;

    static {
        try {
            ByteBuddyAgent.install();

            var lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
            MODIFIERS = lookup.findVarHandle(Field.class, "modifiers", int.class);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        }
    }

    static {
        try {
            injectCTCGraphicsEnvironment();

            Field toolkit = Toolkit.class.getDeclaredField("toolkit");
            toolkit.setAccessible(true);
            toolkit.set(null, new CTCToolkit());

            Field defaultHeadlessField = java.awt.GraphicsEnvironment.class.getDeclaredField("defaultHeadless");
            defaultHeadlessField.setAccessible(true);
            defaultHeadlessField.set(null,Boolean.FALSE);
            Field headlessField = java.awt.GraphicsEnvironment.class.getDeclaredField("headless");
            headlessField.setAccessible(true);
            headlessField.set(null,Boolean.FALSE);

//            Class<?> geCls = Class.forName("java.awt.GraphicsEnvironment$LocalGE");
//            Field ge = geCls.getDeclaredField("INSTANCE");
//            ge.setAccessible(true);
//            defaultHeadlessField.set(null, Boolean.FALSE);
//            headlessField.set(null,Boolean.FALSE);

            //makeNonFinal(ge);

            Class<?> smfCls = Class.forName("sun.java2d.SurfaceManagerFactory");
            Field smf = smfCls.getDeclaredField("instance");
            smf.setAccessible(true);
            smf.set(null, null);

//            ge.set(null, new CTCGraphicsEnvironment());

            String propertyFontManager = System.getProperty("cacio.font.fontmanager");
            if (propertyFontManager != null) {
                FontManagerUtil.setFontManager(propertyFontManager);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.setProperty("swing.defaultlaf", MetalLookAndFeel.class.getName());
    }

    public CTCPreloadClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public static void injectCTCGraphicsEnvironment() throws ClassNotFoundException, IOException {
        /*
         * ByteBuddy is used to intercept the methods that return the graphics environment in use
         * (java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment() and
         *  sun.awt.PlatformGraphicsInfo.createGE())
         *
         * Since java.awt.GraphicsEnvironment is loaded by the bootstrap class loader,
         * all classes used by CTCGraphicsEnvironment also need to be available to the bootstrap class loader,
         * as that class loader also loads the CTCInterceptor class, which will instantiate CTCGraphicsEnvironment.
         */
        injectClassIntoBootstrapClassLoader(
                CTCInterceptor.class,
                CTCGraphicsEnvironment.class,
                CTCSurfaceManagerFactory.class,
                CTCGraphicsConfiguration.class,
                PlatformWindowFactory.class,
                FullScreenWindowFactory.class,
                CTCGraphicsDevice.class,
                CTCVolatileSurfaceManager.class);

        ByteBuddy byteBuddy = new ByteBuddy();

        byteBuddy
                .redefine(
                        TypePool.Default.ofSystemLoader().describe("java.awt.GraphicsEnvironment").resolve(),
                        ClassFileLocator.ForClassLoader.ofSystemLoader())
                .method(ElementMatchers.named("getLocalGraphicsEnvironment"))
                .intercept(
                        MethodDelegation.to(CTCInterceptor.class))
                .make()
                .load(
                        Object.class.getClassLoader(),
                        ClassReloadingStrategy.fromInstalledAgent());

        TypeDescription platformGraphicInfosType;
        platformGraphicInfosType = TypePool.Default.ofSystemLoader().describe("sun.awt.PlatformGraphicsInfo").resolve();
        ClassFileLocator locator = ClassFileLocator.ForClassLoader.ofSystemLoader();

        byteBuddy
                .redefine(
                        platformGraphicInfosType,
                        locator)
                .method(
                        nameStartsWith("createGE"))
                .intercept(
                        MethodDelegation.to(GraphicsEnvironmentInterceptor.class))
                .make()
                .load(
                        Thread.currentThread().getContextClassLoader(),
                        ClassReloadingStrategy.fromInstalledAgent());

    }

    private static void injectClassIntoBootstrapClassLoader(Class... classes) throws IOException {
        for (Class<?> clazz: classes) {
            final byte[] buffer = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/').concat(".class")).readAllBytes();
            ClassInjector.UsingUnsafe injector = new ClassInjector.UsingUnsafe(null);
            injector.injectRaw(Map.of(clazz.getName(), buffer));
        }
    }

    public static class GraphicsEnvironmentInterceptor {
        @RuntimeType
        public static Object intercept(@Origin Method method, @AllArguments final Object[] args) throws Exception {
            return CTCGraphicsEnvironment.getInstance();
        }
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
        try {
            System.setProperty("java.class.path", System.getProperty("java.class.path") + ":" + new File(url.toURI()).getAbsolutePath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    static URL getFileURL(File file) {
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {}

        try {
            return file.toURL();
        } catch (MalformedURLException e) {
            // Should never happen since we specify the protocol...
            throw new InternalError(e);
        }
    }

    private void appendToClassPathForInstrumentation(String path) {
        assert(Thread.holdsLock(this));
        super.addURL(getFileURL(new File(path)));
    }

    public static void makeNonFinal(Field field) {
        int mods = field.getModifiers();
        if (Modifier.isFinal(mods)) {
            MODIFIERS.set(field, mods & ~Modifier.FINAL);
        }
    }
}
