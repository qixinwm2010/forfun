/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yourorg;

import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.J.Annotation;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodDeclaration;
import org.openrewrite.java.tree.J.Modifier;
import org.openrewrite.java.tree.J.VariableDeclarations;
import org.openrewrite.marker.Markers;

import com.fasterxml.jackson.annotation.JsonProperty;

// the recipe that add static modifier to class level method
public class AddStaticModifierToClassLevelMethodRecipe extends Recipe {
    
    private boolean debug = true;

    @Option(displayName = "Fully Qualified Class Name",
            description = "A fully-qualified class name indicating which class to run the recipe against.",
            example = "com.yourorg.FooBar")
    @NonNull
    private final String fullyQualifiedClassName;
    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }
    
    // Recipes must be serializable. This is verified by RecipeTest.assertChanged() and RecipeTest.assertUnchanged()
    public AddStaticModifierToClassLevelMethodRecipe(@NonNull @JsonProperty("fullyQualifiedClassName") String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;

    }

    @Override
    public String getDisplayName() {
        return "Add Static Modifier To Class Level Method";
    }

    @Override
    public String getDescription() {
        return "Add static modifier to class level method that does not access instance data";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new AddStaticModifierToClassLevelMethodVisitor();
    }

    public class AddStaticModifierToClassLevelMethodVisitor extends JavaIsoVisitor<ExecutionContext> {
        private Set<String> instanceVariables = new HashSet<String>();
        private final String instanceVariablesKey = "InstanceVariables";
        private final String insideAMethodKey = "InsideAMethod";
        private final String methodUsesInstanceVariableKey = "MethodUsesInstanceVariable";

        @Override
        public ClassDeclaration visitClassDeclaration(ClassDeclaration classDecl, ExecutionContext executionContext) {
            // the class instance variables will be stored in instanceVariables
            // unset the insideAMethod flag
            executionContext.putMessage(instanceVariablesKey, instanceVariables);
            executionContext.putMessage(insideAMethodKey, false);
            classDecl = super.visitClassDeclaration(classDecl, executionContext);
            if (classDecl == null || classDecl.getType() == null || !classDecl.getType().getFullyQualifiedName().equals(fullyQualifiedClassName)) {
                return classDecl;
            }

            if (debug) {
                System.out.println("Final Class methods: ");
                classDecl.getBody().getStatements().stream()
                .filter(statement -> statement instanceof MethodDeclaration)
                .map(MethodDeclaration.class::cast)
                .map(method -> method.getModifiers() + "  " + method.getSimpleName())
                .forEach((System.out::println));
            }
            return classDecl;
        }

        @Override
        public MethodDeclaration visitMethodDeclaration(MethodDeclaration methodDeclaration, ExecutionContext executionContext) {
            if (debug) {
                System.out.println("visitMethodDeclaration: " + methodDeclaration.getSimpleName());
                Set<String> instanceVariables = executionContext.getMessage(instanceVariablesKey);
                System.out.println("Instance Variables: " + instanceVariables);
            }
            // set the insideAMethod flags
            executionContext.putMessage(insideAMethodKey, true);
            // unset the methodUsesInstanceVariable flag
            executionContext.putMessage(methodUsesInstanceVariableKey, false);
            methodDeclaration = super.visitMethodDeclaration(methodDeclaration, executionContext);
            // if it is non-override method, return immediately
            if(methodDeclaration.getAllAnnotations()== null ){
                return methodDeclaration;
            }
            if(methodDeclaration.getAllAnnotations().stream().anyMatch(annotaion -> annotaion.getSimpleName().equals("Override"))){
                if (debug) System.out.println("leaveMethodDeclaration: " + methodDeclaration.getSimpleName());
                return methodDeclaration;
            }

            // unset the insideAMethod flag
            executionContext.putMessage(insideAMethodKey, false);
            // get the methodUsesInstanceVariable flag
            boolean methodUsesInstanceVariable = executionContext.getMessage(methodUsesInstanceVariableKey);
            // unset the methodUsesInstanceVariable flag
            executionContext.putMessage(methodUsesInstanceVariableKey, false);

            if (!methodUsesInstanceVariable && !Modifier.hasModifier(methodDeclaration.getModifiers(), Modifier.Type.Static)) {
                if (debug) System.out.println("We need to change " + methodDeclaration.getSimpleName());
                Modifier staticModifier = new Modifier(Tree.randomId(), Space.build(" ", Collections.emptyList()), Markers.EMPTY, Modifier.Type.Static, Collections.emptyList());
                List<Modifier> modifiers = methodDeclaration.getModifiers();
                modifiers.add(staticModifier);
                methodDeclaration = methodDeclaration.withModifiers(modifiers);
            }
            if (debug) System.out.println("leaveMethodDeclaration: " + methodDeclaration.getSimpleName());
            return methodDeclaration;
        }

        @Override
        public VariableDeclarations visitVariableDeclarations(VariableDeclarations variableDeclaration, ExecutionContext executionContext) {
            if (debug)   System.out.println("visitVariableDeclarations");
            variableDeclaration = super.visitVariableDeclarations(variableDeclaration, executionContext);
            if(variableDeclaration==null || variableDeclaration.getModifiers() == null || variableDeclaration.getVariables() == null){
                return variableDeclaration;
            }
            // get the insideAMethod flag
            boolean insideAMethod = executionContext.getMessage(insideAMethodKey);
            // if it is not inside a method and variable has a static modifer, return
            // if it is inside a method, return
            if(!insideAMethod){
                if (Modifier.hasModifier(variableDeclaration.getModifiers(), Modifier.Type.Static)){
                    if (debug) System.out.println("leaveVariableDeclarations");
                    return variableDeclaration;
                }
            } else {
                if (debug) System.out.println("leaveVariableDeclarations");
                return variableDeclaration;
            }
            
            // collect the class instance variables
            for (VariableDeclarations.NamedVariable variable : variableDeclaration.getVariables()){
                Set<String> instanceVariables = executionContext.getMessage(instanceVariablesKey);
                instanceVariables.add(variable.getSimpleName());
            }
            if (debug) System.out.println("leaveVariableDeclarations");
            return variableDeclaration;
        }

        @Override
        public Identifier visitIdentifier(Identifier identifier, ExecutionContext executionContext) {
            identifier = super.visitIdentifier(identifier, executionContext);
            if(identifier==null){
                return identifier;
            }
            // get the insideAMethod flag
            Boolean insideAMethod = executionContext.getMessage(insideAMethodKey);
            
            // if the identifier is inside a method, decide whether the method uses the instance variable or not
            if (insideAMethod!=null && insideAMethod){
                Set<String> instanceVariables = executionContext.getMessage(instanceVariablesKey);
                boolean isInstanceVariable = instanceVariables.contains(identifier.toString());
                boolean methodUsesInstanceVariable = executionContext.getMessage(methodUsesInstanceVariableKey);
                executionContext.putMessage(methodUsesInstanceVariableKey, methodUsesInstanceVariable || isInstanceVariable);
            }
            return identifier;
        }
    }
}