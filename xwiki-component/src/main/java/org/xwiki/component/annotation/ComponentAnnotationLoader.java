/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 *
 */
package org.xwiki.component.annotation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDependency;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;

/**
 * Dynamically loads all components defined using Annotations.
 * 
 * @version $Id: $
 * @since 1.8.1
 */
public class ComponentAnnotationLoader
{
    /**
     * Location in the classloader of the file defining the list of component implementation class to parser for 
     * annotations.
     */
    private static final String COMPONENT_LIST = "META-INF/components.txt";
    
    /**
     * Loads all components defined using annotations.
     * 
     * @param manager the component manager to use to dynamically register components
     */
    public void initialize(ComponentManager manager)
    {
        ClassLoader cl = ComponentAnnotationLoader.class.getClassLoader();
        
        try {
            // 1) Find all components by retrieving the list defined in COMPONENT_LIST
            List<String> componentClassNames = findAnnotatedComponentClasses(cl);

            // 2) For each component class name found, load its class and use introspection to find the necessary 
            //    annotations required to register it as a component
            for (String componentClassName : componentClassNames) {
                Class< ? > componentClass = cl.loadClass(componentClassName);
                
                // Look for ComponentRole annotations and register one component per ComponentRole found
                for (Class< ? > componentRoleClass : findComponentRoleClasses(componentClass)) {
                    ComponentDescriptor descriptor = createComponentDescriptor(componentClass, componentRoleClass);
                    manager.registerComponent(descriptor);
                }
            }

        } catch (Exception e) {
            // Make sure we make the calling code fail in order to fail fast and prevent the application to start
            // if something is amiss.
            throw new RuntimeException("Failed to dynamically load components with annotations", e);
        }
    }
    
    /**
     * Create a component descriptor for the passed component implementation class and component role class.
     * 
     * @param componentClass the component implementation class
     * @param componentRoleClass the component role class
     * @return the component descriptor with resolved component dependencies
     */
    protected ComponentDescriptor createComponentDescriptor(Class< ? > componentClass, Class< ? > componentRoleClass)
    {
        DefaultComponentDescriptor descriptor = new DefaultComponentDescriptor();
        descriptor.setRole(componentRoleClass.getName());
        descriptor.setImplementation(componentClass.getName());
        
        // Set the hint if it exists
        ComponentHint hint = componentClass.getAnnotation(ComponentHint.class);
        if (hint != null) {
            descriptor.setRoleHint(hint.value());
        } else {
            // The descriptor sets a default hint by default. 
        }

        // Set the instantiation strategy
        InstantiationStrategy instantiationStrategy = componentClass.getAnnotation(InstantiationStrategy.class);
        if (instantiationStrategy != null) {
            descriptor.setInstantiationStrategy(instantiationStrategy.value());
        } else {
            descriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        }
        
        // Set the requirements
        for (Field field : componentClass.getDeclaredFields()) {
            Requirement requirement = field.getAnnotation(Requirement.class);
            if (requirement != null) {
                DefaultComponentDependency dependency = new DefaultComponentDependency();
                dependency.setRole(field.getType().getName());

                RequirementHint requirementHint = field.getAnnotation(RequirementHint.class);
                if (requirementHint != null) {
                    dependency.setRoleHint(requirementHint.value());
                }
                
                descriptor.addComponentDependency(dependency);
            }
        }
        
        return descriptor;
    }
    
    /**
     * Finds the interfaces that implement component roles by looking recursively in all interfaces of
     * the passed component implementation class.
     * 
     * @param componentClass the component implementation class for which to find the component roles it implements 
     * @return the list of component role classes implemented
     */
    protected List<Class< ? >> findComponentRoleClasses(Class< ? > componentClass)
    {
        List<Class< ? >> classes = new ArrayList<Class< ? >>();
        
        // Only look in interfaces since we only want to allow using @ComponentRole in interfaces
        for (Class< ? > interfaceClass : componentClass.getInterfaces()) {
            classes.addAll(findComponentRoleClasses(interfaceClass));
            for (Annotation annotation : interfaceClass.getDeclaredAnnotations()) {
                if (annotation.annotationType().getName().equals(ComponentRole.class.getName())) {
                    classes.add(interfaceClass);
                }
            }
        }
        
        return classes;
    }
    
    /**
     * Find all components defined using Annotations. These components are looked for in
     * {@link #COMPONENT_LIST} resources.
     * 
     * @param classLoader the classloader to use to load the {@link #COMPONENT_LIST} resources
     * @return the list of component implementation class names
     * @throws IOException in case of an error loading the component list resource
     */
    private List<String> findAnnotatedComponentClasses(ClassLoader classLoader) throws IOException
    {
        List<String> annotatedClassNames = new ArrayList<String>();
        Enumeration<URL> urls = classLoader.getResources(COMPONENT_LIST);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            
            // Read all components definition from the URL
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                annotatedClassNames.add(inputLine);
            }
            in.close();
        }
        return annotatedClassNames;
    }
}
