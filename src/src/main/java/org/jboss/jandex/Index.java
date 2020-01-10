/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.jandex;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An index useful for quickly processing annotations. The index is read-only and supports
 * concurrent access. Also the index is optimized for memory efficiency by using componentized
 * DotName values.
 *
 * <p>It contains the following information:
 * <ol>
 * <li>All annotations and a collection of targets they refer to </li>
 * <li>All classes (including interfaces) scanned during the indexing process (typical all classes in a jar)</li>
 * <li>All subclasses indexed by super class known to this index</li>
 < </ol>
 *
 * @author Jason T. Greene
 *
 */
public final class Index {
    private static final List<AnnotationInstance> EMPTY_ANNOTATION_LIST = Collections.emptyList();
    private static final List<ClassInfo> EMPTY_CLASSINFO_LIST = Collections.emptyList();

    final Map<DotName, List<AnnotationInstance>> annotations;
    final Map<DotName, List<ClassInfo>> subclasses;
    final Map<DotName, List<ClassInfo>> implementors;
    final Map<DotName, ClassInfo> classes;

    Index(Map<DotName, List<AnnotationInstance>> annotations, Map<DotName, List<ClassInfo>> subclasses, Map<DotName, List<ClassInfo>> implementors, Map<DotName, ClassInfo> classes) {
        this.annotations = Collections.unmodifiableMap(annotations);
        this.classes = Collections.unmodifiableMap(classes);
        this.subclasses = Collections.unmodifiableMap(subclasses);
        this.implementors = Collections.unmodifiableMap(implementors);
    }


    /**
     * Constructs a "mock" Index using the passed values. All passed values MUST NOT BE MODIFIED AFTER THIS CALL.
     * Otherwise the resulting object would not conform to the contract outlined above. Also, to conform to the
     * memory efficiency contract this method should be passed componentized DotNames, which all share common root
     * instances. Of course for testing code this doesn't really matter.
     *
     * @param annotations A map to lookup annotation instances by class name
     * @param subclasses A map to lookup subclasses by super class name
     * @param implementors A map to lookup implementing classes by interface name
     * @param classes A map to lookup classes by class name
     * @return the index
     */
    public static Index create(Map<DotName, List<AnnotationInstance>> annotations, Map<DotName, List<ClassInfo>> subclasses, Map<DotName, List<ClassInfo>> implementors, Map<DotName, ClassInfo> classes) {
        return new Index(annotations, subclasses, implementors, classes);
    }


    /**
     * Obtains a list of instances for the specified annotation.
     * This is done using an O(1) lookup. Valid instance targets include
     * field, method, parameter, and class.
     *
     * @param annotationName the name of the annotation to look for
     * @return a non-null list of annotation instances
     */
    public List<AnnotationInstance> getAnnotations(DotName annotationName) {
        List<AnnotationInstance> list = annotations.get(annotationName);
        return list == null ? EMPTY_ANNOTATION_LIST: Collections.unmodifiableList(list);
    }

    /**
     * Gets all known direct subclasses of the specified class name. A known direct
     * subclass is one which was found during the scanning process; however, this is
     * often not the complete universe of subclasses, since typically indexes are
     * constructed per jar. It is expected that several indexes will need to be searched
     * when analyzing a jar that is a part of a complex multi-module/classloader
     * environment (like an EE application server).
     * <p/>
     * Note that this will only pick up direct subclasses of the class. It will not
     * pick up subclasses of subclasses.
     * @param className the super class of the desired subclasses
     * @return a non-null list of all known subclasses of className
     */
    public List<ClassInfo> getKnownDirectSubclasses(DotName className) {
        List<ClassInfo> list = subclasses.get(className);
        return list == null ? EMPTY_CLASSINFO_LIST : Collections.unmodifiableList(list);
    }
    /**
     * Gets all known direct implementors of the specified interface name. A known
     * direct implementor is one which was found during the scanning process; however,
     * this is often not the complete universe of implementors, since typically indexes
     * are constructed per jar. It is expected that several indexes will need to
     * be searched when analyzing a jar that is a part of a complex
     * multi-module/classloader environment (like an EE application server).
     * <p/>
     * The list of implementors may also include other interfaces, in order to get a complete
     * list of all classes that are assignable to a given interface it is necessary to
     * recursively call {@link #getKnownDirectImplementors(DotName)} for every implementing
     * interface found.
     *
     * @param className the super class of the desired subclasses
     * @return a non-null list of all known subclasses of className
     */
    public List<ClassInfo> getKnownDirectImplementors(DotName className) {
        List<ClassInfo> list = implementors.get(className);
        return list == null ? EMPTY_CLASSINFO_LIST : Collections.unmodifiableList(list);
    }

    /**
     * Gets the class (or interface, or annotation) that was scanned during the
     * indexing phase.
     *
     * @param className the name of the class
     * @return information about the class or null if it is not known
     */
    public ClassInfo getClassByName(DotName className) {
        return classes.get(className);
    }

    /**
     * Gets all known classes by this index (those which were scanned).
     *
     * @return a collection of known classes
     */
    public Collection<ClassInfo> getKnownClasses() {
        return classes.values();
    }

    /**
     * Print all annotations known by this index to stdout.
     */
    public void printAnnotations()
    {
        System.out.println("Annotations:");
        for (Map.Entry<DotName, List<AnnotationInstance>> e : annotations.entrySet()) {
            System.out.println(e.getKey() + ":");
            for (AnnotationInstance instance : e.getValue()) {
                AnnotationTarget target = instance.target();


                if (target instanceof ClassInfo) {
                    System.out.println("    Class: " + target);
                } else if (target instanceof FieldInfo) {
                    System.out.println("    Field: " + target);
                } else if (target instanceof MethodInfo) {
                    System.out.println("    Method: " + target);
                } else if (target instanceof MethodParameterInfo) {
                    System.out.println("    Parameter: " + target);
                }

                List<AnnotationValue> values = instance.values();
                if (values.size() < 1)
                    continue;

                StringBuilder builder = new StringBuilder("        (");

                for (int i =  0; i < values.size(); i ++) {
                    builder.append(values.get(i));
                    if (i < values.size() - 1)
                        builder.append(", ");
                }
                builder.append(')');
                System.out.println(builder.toString());
            }
        }
    }

    /**
     * Print all classes that have known subclasses, and all their subclasses
     */
    public void printSubclasses()
    {
        System.out.println("Subclasses:");
        for (Map.Entry<DotName, List<ClassInfo>> entry : subclasses.entrySet()) {
            System.out.println(entry.getKey() + ":");
            for (ClassInfo clazz : entry.getValue())
                System.out.println("    " + clazz.name());
        }
    }
}
