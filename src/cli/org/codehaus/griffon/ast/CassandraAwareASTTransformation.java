/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.griffon.ast;

import griffon.plugins.cassandra.DefaultCassandraProvider;
import griffon.plugins.cassandra.CassandraAware;
import griffon.plugins.cassandra.CassandraContributionHandler;
import griffon.plugins.cassandra.CassandraProvider;
import lombok.core.handlers.CassandraAwareConstants;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.codehaus.griffon.ast.GriffonASTUtils.*;

/**
 * Handles generation of code for the {@code @CassandraAware} annotation.
 * <p/>
 *
 * @author Andres Almiray
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class CassandraAwareASTTransformation extends AbstractASTTransformation implements CassandraAwareConstants {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraAwareASTTransformation.class);
    private static final ClassNode CASSANDRA_CONTRIBUTION_HANDLER_CNODE = makeClassSafe(CassandraContributionHandler.class);
    private static final ClassNode CASSANDRA_AWARE_CNODE = makeClassSafe(CassandraAware.class);
    private static final ClassNode CASSANDRA_PROVIDER_CNODE = makeClassSafe(CassandraProvider.class);
    private static final ClassNode DEFAULT_CASSANDRA_PROVIDER_CNODE = makeClassSafe(DefaultCassandraProvider.class);

    private static final String[] DELEGATING_METHODS = new String[] {
        METHOD_WITH_CQL
    };

    static {
        Arrays.sort(DELEGATING_METHODS);
    }

    /**
     * Convenience method to see if an annotated node is {@code @CassandraAware}.
     *
     * @param node the node to check
     * @return true if the node is an event publisher
     */
    public static boolean hasCassandraAwareAnnotation(AnnotatedNode node) {
        for (AnnotationNode annotation : node.getAnnotations()) {
            if (CASSANDRA_AWARE_CNODE.equals(annotation.getClassNode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the bulk of the processing, mostly delegating to other methods.
     *
     * @param nodes  the ast nodes
     * @param source the source unit for the nodes
     */
    public void visit(ASTNode[] nodes, SourceUnit source) {
        checkNodesForAnnotationAndType(nodes[0], nodes[1]);
        addCassandraContributionIfNeeded(source, (ClassNode) nodes[1]);
    }

    public static void addCassandraContributionIfNeeded(SourceUnit source, ClassNode classNode) {
        if (needsCassandraContribution(classNode, source)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Injecting " + CassandraContributionHandler.class.getName() + " into " + classNode.getName());
            }
            apply(classNode);
        }
    }

    protected static boolean needsCassandraContribution(ClassNode declaringClass, SourceUnit sourceUnit) {
        boolean found1 = false, found2 = false, found3 = false, found4 = false;
        ClassNode consideredClass = declaringClass;
        while (consideredClass != null) {
            for (MethodNode method : consideredClass.getMethods()) {
                // just check length, MOP will match it up
                found1 = method.getName().equals(METHOD_WITH_CQL) && method.getParameters().length == 1;
                found2 = method.getName().equals(METHOD_WITH_CQL) && method.getParameters().length == 2;
                found3 = method.getName().equals(METHOD_SET_CASSANDRA_PROVIDER) && method.getParameters().length == 1;
                found4 = method.getName().equals(METHOD_GET_CASSANDRA_PROVIDER) && method.getParameters().length == 0;
                if (found1 && found2 && found3 && found4) {
                    return false;
                }
            }
            consideredClass = consideredClass.getSuperClass();
        }
        if (found1 || found2 || found3 || found4) {
            sourceUnit.getErrorCollector().addErrorAndContinue(
                new SimpleMessage("@CassandraAware cannot be processed on "
                    + declaringClass.getName()
                    + " because some but not all of methods from " + CassandraContributionHandler.class.getName() + " were declared in the current class or super classes.",
                    sourceUnit)
            );
            return false;
        }
        return true;
    }

    public static void apply(ClassNode declaringClass) {
        injectInterface(declaringClass, CASSANDRA_CONTRIBUTION_HANDLER_CNODE);

        // add field:
        // protected CassandraProvider this$cassandraProvider = DefaultCassandraProvider.instance
        FieldNode providerField = declaringClass.addField(
            CASSANDRA_PROVIDER_FIELD_NAME,
            ACC_PRIVATE | ACC_SYNTHETIC,
            CASSANDRA_PROVIDER_CNODE,
            defaultCassandraProviderInstance());

        // add method:
        // CassandraProvider getCassandraProvider() {
        //     return this$cassandraProvider
        // }
        injectMethod(declaringClass, new MethodNode(
            METHOD_GET_CASSANDRA_PROVIDER,
            ACC_PUBLIC,
            CASSANDRA_PROVIDER_CNODE,
            Parameter.EMPTY_ARRAY,
            NO_EXCEPTIONS,
            returns(field(providerField))
        ));

        // add method:
        // void setCassandraProvider(CassandraProvider provider) {
        //     this$cassandraProvider = provider ?: DefaultCassandraProvider.instance
        // }
        injectMethod(declaringClass, new MethodNode(
            METHOD_SET_CASSANDRA_PROVIDER,
            ACC_PUBLIC,
            ClassHelper.VOID_TYPE,
            params(
                param(CASSANDRA_PROVIDER_CNODE, PROVIDER)),
            NO_EXCEPTIONS,
            block(
                ifs_no_return(
                    cmp(var(PROVIDER), ConstantExpression.NULL),
                    assigns(field(providerField), defaultCassandraProviderInstance()),
                    assigns(field(providerField), var(PROVIDER))
                )
            )
        ));

        for (MethodNode method : CASSANDRA_CONTRIBUTION_HANDLER_CNODE.getMethods()) {
            if (Arrays.binarySearch(DELEGATING_METHODS, method.getName()) < 0) continue;
            List<Expression> variables = new ArrayList<Expression>();
            Parameter[] parameters = new Parameter[method.getParameters().length];
            for (int i = 0; i < method.getParameters().length; i++) {
                Parameter p = method.getParameters()[i];
                parameters[i] = new Parameter(makeClassSafe(p.getType()), p.getName());
                parameters[i].getType().setGenericsTypes(p.getType().getGenericsTypes());
                variables.add(var(p.getName()));
            }
            ClassNode returnType = makeClassSafe(method.getReturnType());
            returnType.setGenericsTypes(method.getReturnType().getGenericsTypes());
            returnType.setGenericsPlaceHolder(method.getReturnType().isGenericsPlaceHolder());

            MethodNode newMethod = new MethodNode(
                method.getName(),
                ACC_PUBLIC,
                returnType,
                parameters,
                NO_EXCEPTIONS,
                returns(call(
                    field(providerField),
                    method.getName(),
                    args(variables)))
            );
            newMethod.setGenericsTypes(method.getGenericsTypes());
            injectMethod(declaringClass, newMethod);
        }
    }

    private static Expression defaultCassandraProviderInstance() {
        return call(DEFAULT_CASSANDRA_PROVIDER_CNODE, "getInstance", NO_ARGS);
    }
}
