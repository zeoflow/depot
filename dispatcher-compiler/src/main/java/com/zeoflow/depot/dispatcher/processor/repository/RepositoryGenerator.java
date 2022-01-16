/*
 * Copyright (C) 2017 depot
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zeoflow.depot.dispatcher.processor.repository;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

import androidx.annotation.NonNull;

import com.zeoflow.depot.dispatcher.processor.PreferenceKeyField;
import com.zeoflow.depot.dispatcher.processor.StringUtils;
import com.zeoflow.depot.dispatcher.processor.atom.AtomGenerator;
import com.zeoflow.jx.file.ClassName;
import com.zeoflow.jx.file.FieldSpec;
import com.zeoflow.jx.file.MethodSpec;
import com.zeoflow.jx.file.ParameterSpec;
import com.zeoflow.jx.file.ParameterizedTypeName;
import com.zeoflow.jx.file.TypeName;
import com.zeoflow.jx.file.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

@SuppressWarnings({"WeakerAccess", "unused", "RedundantSuppression"})
public class RepositoryGenerator {

    public static final String REPOSITORY_CLASS_PREFIX = "_Repository";
    private static final String FIELD_SUFFIX = "m";
    private final RepositoryAnnotatedClass annotatedClazz;
    private final Elements annotatedElementUtils;

    public RepositoryGenerator(
            @NonNull RepositoryAnnotatedClass annotatedClass,
            @NonNull Elements elementUtils
    ) {
        this.annotatedClazz = annotatedClass;
        this.annotatedElementUtils = elementUtils;
    }

    public TypeSpec generate() {
        TypeSpec.Builder builder = TypeSpec
                .classBuilder(getClazzName())
                .addModifiers(PUBLIC);
        builder.addField(getObjectDaoField());
        builder.addMethod(getInitMethod());
        builder.addMethods(addGetMethods());
        builder.addMethods(addVoidMethods());
        return builder.build();
    }

    private FieldSpec getObjectDaoField() {
        return FieldSpec.builder(
                getDaoObjectPackage(),
                getObjectDaoFieldName(),
                Modifier.PRIVATE,
                FINAL
        ).build();
    }

    private MethodSpec getInitMethod() {
        MethodSpec.Builder method = MethodSpec.constructorBuilder();
        method.addStatement("$N db = $N.getDatabase()", getAtomDBName(), getAtomDBName());
        method.addStatement(
                "$N = db.$N()",
                getObjectDaoFieldName(),
                StringUtils.toLowerCamel(annotatedClazz.entityName)
        );
        return method.build();
    }

    private List<MethodSpec> addGetMethods() {
        List<MethodSpec> methods = new ArrayList<>();
        for (PreferenceKeyField keyField : this.annotatedClazz.keyFields) {
            if (keyField.executableElement.getReturnType().getKind() == TypeKind.VOID) {
                continue;
            }
            List<? extends VariableElement> parametersList = keyField.executableElement.getParameters();
            TypeName typeName = ParameterSpec.builder(keyField.typeName, keyField.keyName.toLowerCase()).build().type;
            switch (keyField.typeStringName) {
                case "Boolean":
                    typeName = TypeName.get(Boolean.class);
                    break;
                case "Int":
                    typeName = TypeName.get(Integer.class);
                    break;
                case "Float":
                    typeName = TypeName.get(Float.class);
                    break;
                case "Long":
                    typeName = TypeName.get(Long.class);
                    break;
            }
            // todo handle empty params
            MethodSpec.Builder method = MethodSpec.methodBuilder(getKeyFieldReturnName(keyField.variableName));
            method.returns(typeName);
            if (parametersList.size() == 0) {
                if (!isType(typeName, getLiveDataClass()) || keyField.observable == null) {
                    method.addStatement(
                            "return $N()",
                            getFieldReturnName(keyField.keyName)
                    );
                } else {
                    method.addStatement(
                            "return $N.$N()",
                            getObjectDaoFieldName(),
                            keyField.executableElement.getSimpleName()
                    );
                }
            } else {
                StringBuilder methodParams = new StringBuilder();
                for (int index = 0; index < parametersList.size(); index++) {
                    VariableElement element = parametersList.get(index);
                    ParameterSpec.Builder param = ParameterSpec.builder(
                            TypeName.get(element.asType()),
                            String.valueOf(element.getSimpleName())
                    );
                    method.addParameter(param.build());
                    methodParams.append(element.getSimpleName());
                    if (index < parametersList.size() - 1) {
                        methodParams.append(", ");
                    }
                }
                if (!isType(typeName, getLiveDataClass()) || keyField.observable == null) {
                    method.addStatement(
                            "return $N($N)",
                            getFieldReturnName(keyField.keyName),
                            methodParams.toString()
                    );
                } else {
                    method.addStatement(
                            "return $N.$N($N)",
                            getObjectDaoFieldName(),
                            keyField.executableElement.getSimpleName(),
                            methodParams.toString()
                    );
                }
            }
            methods.add(method.build());
        }
        return methods;
    }

    private List<MethodSpec> addVoidMethods() {
        List<MethodSpec> methods = new ArrayList<>();
        for (PreferenceKeyField keyField : this.annotatedClazz.keyFields) {
            if (keyField.executableElement.getReturnType().getKind() != TypeKind.VOID) {
                continue;
            }
            List<? extends VariableElement> parametersList = keyField.executableElement.getParameters();
            MethodSpec.Builder method = MethodSpec.methodBuilder(getKeyFieldPutName(keyField.variableName));
            if (keyField.keyName.contains("delete")) {
                if (parametersList.size() == 0) {
                    method.addStatement(
                            "$N.$N.execute(() -> $N.$N())",
                            getAtomDBName(),
                            AtomGenerator.FIELD_NAME_DATABASE_EXECUTOR,
                            getObjectDaoFieldName(),
                            keyField.executableElement.getSimpleName()
                    );
                } else {
                    StringBuilder methodParams = new StringBuilder();
                    for (int index = 0; index < parametersList.size(); index++) {
                        VariableElement element = parametersList.get(index);
                        ParameterSpec.Builder param = ParameterSpec.builder(
                                TypeName.get(element.asType()),
                                String.valueOf(element.getSimpleName())
                        );
                        method.addParameter(param.build());
                        methodParams.append(element.getSimpleName());
                        if (index < parametersList.size() - 1) {
                            methodParams.append(", ");
                        }
                    }
                    method.addStatement(
                            "$N.$N.execute(() -> $N.$N($N))",
                            getAtomDBName(),
                            AtomGenerator.FIELD_NAME_DATABASE_EXECUTOR,
                            getObjectDaoFieldName(),
                            keyField.executableElement.getSimpleName(),
                            methodParams.toString()
                    );
                }
            } else if (parametersList.size() == 0) {
                method.addStatement(
                        "$N.$N()",
                        getObjectDaoFieldName(),
                        keyField.executableElement.getSimpleName()
                );
            } else {
                // todo just because it has some parameters it doesn't mean that
                //  it should run in the background thread
                StringBuilder methodParams = new StringBuilder();
                for (int index = 0; index < parametersList.size(); index++) {
                    VariableElement element = parametersList.get(index);
                    ParameterSpec.Builder param = ParameterSpec.builder(
                            TypeName.get(element.asType()),
                            String.valueOf(element.getSimpleName())
                    );
                    method.addParameter(param.build());
                    methodParams.append(element.getSimpleName());
                    if (index < parametersList.size() - 1) {
                        methodParams.append(", ");
                    }
                }
                method.addStatement(
                        "$N.$N.execute(() -> $N.$N($N))",
                        getAtomDBName(),
                        AtomGenerator.FIELD_NAME_DATABASE_EXECUTOR,
                        getObjectDaoFieldName(),
                        keyField.executableElement.getSimpleName(),
                        methodParams.toString()
                );
            }
            method.addComment("addVoidMethod???");
            methods.add(method.build());
        }
        return methods;
    }

    private TypeName getDaoObjectPackage() {
        return TypeName.get(annotatedElementUtils.getTypeElement(annotatedClazz.packageName + "." + annotatedClazz.entityName).asType());
    }

    private TypeName getAtomDBPackage() {
        return TypeName.get(annotatedElementUtils.getTypeElement(getAtomDBName()).asType());
    }

    private String getAtomDBName() {
        // full name of class with package
        // annotatedClazz.atomAnnotatedClass.packageName + "." + annotatedClazz.atomAnnotatedClass.entityName + "_AtomDB";
        return annotatedClazz.atomAnnotatedClass.entityName + AtomGenerator.ATOM_CLASS_PREFIX;
    }

    private String getClazzName() {
        return annotatedClazz.entityName + REPOSITORY_CLASS_PREFIX;
    }

    private ClassName getLiveDataClass() {
        return ClassName.get("androidx.lifecycle", "LiveData");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isType(TypeName requestType, TypeName expectedType) {
        if (requestType instanceof ParameterizedTypeName) {
            TypeName typeName = ((ParameterizedTypeName) requestType).rawType;
            return (typeName.equals(expectedType));
        }
        return false;
    }

    private String getKeyFieldVariableName(String variableName) {
        return "m" + StringUtils.toUpperCamel(variableName);
    }

    private String getKeyFieldReturnName(String variableName) {
        if (variableName.toLowerCase().startsWith("get")) {
            return StringUtils.toLowerCamel(variableName);
        }
        return "get" + StringUtils.toUpperCamel(variableName);
    }

    private String getKeyFieldPutName(String variableName) {
        return StringUtils.toLowerCamel(variableName);//"put" + StringUtils.toUpperCamel(variableName);
    }

    private String getObjectDaoFieldName() {
        return FIELD_SUFFIX + StringUtils.toUpperCamel(annotatedClazz.entityName);
    }

    private String getFieldReturnName(String keyName) {
        return getObjectDaoFieldName() + "." + keyName;
    }

}
