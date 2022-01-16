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

package com.zeoflow.depot.dispatcher.processor.model;

import static com.zeoflow.depot.dispatcher.processor.repository.RepositoryGenerator.REPOSITORY_CLASS_PREFIX;
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
public class ModelGenerator {

    private static final String CLAZZ_PREFIX = "_Model";
    private static final String FIELD_SUFFIX = "m";
    private final ModelAnnotatedClass annotatedClazz;
    private final Elements annotatedElementUtils;
    private final String PACKAGE_LIFECYCLE_OWNER = "androidx.lifecycle.LifecycleOwner";
    private final String PACKAGE_LIFECYCLE_OBSERVER = "androidx.lifecycle.Observer";

    public ModelGenerator(
            @NonNull ModelAnnotatedClass annotatedClass,
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
        builder.addMethods(addObserverMethods());
        return builder.build();
    }

    private FieldSpec getObjectDaoField() {
        return FieldSpec.builder(
                getDaoRepositoryPackage(),
                getRepositoryDaoFieldName(),
                Modifier.PRIVATE,
                FINAL
        ).build();
    }

    private MethodSpec getInitMethod() {
        MethodSpec.Builder method = MethodSpec.constructorBuilder();
        method.addModifiers(PUBLIC);
        method.addStatement(
                "$N = new $N()",
                getRepositoryDaoFieldName(),
                getRepositoryClassName()
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
            MethodSpec.Builder method = MethodSpec.methodBuilder(getKeyFieldReturnName(keyField.variableName));
            method.returns(typeName);
            method.addModifiers(PUBLIC);
            if (parametersList.size() == 0) {
                if (!isType(typeName, getLiveDataClass()) || keyField.observable == null) {
                    method.addStatement(
                            "return $N()",
                            getFieldReturnName(keyField.keyName)
                    );
                } else {
                    method.addStatement(
                            "return $N.$N()",
                            getRepositoryDaoFieldName(),
                            getKeyFieldReturnName(keyField.variableName)
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
                            getRepositoryDaoFieldName(),
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
            if (keyField.executableElement.getReturnType().getKind() != TypeKind.VOID ||
                    isType(typeName, getLiveDataClass()) && keyField.observable != null) {
                continue;
            }
            List<? extends VariableElement> parametersList = keyField.executableElement.getParameters();
            MethodSpec.Builder method = MethodSpec.methodBuilder(getKeyFieldPutName(keyField.variableName));
            method.addModifiers(PUBLIC);
            if (parametersList.size() == 0) {
                method.addStatement(
                        "$N.$N()",
                        getRepositoryDaoFieldName(),
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
                        "$N.$N($N)",
                        getRepositoryDaoFieldName(),
                        keyField.executableElement.getSimpleName(),
                        methodParams.toString()
                );
            }
            methods.add(method.build());
        }
        return methods;
    }

    private List<MethodSpec> addObserverMethods() {
        List<MethodSpec> methods = new ArrayList<>();
        for (PreferenceKeyField keyField : this.annotatedClazz.keyFields) {
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
            if (!isType(typeName, getLiveDataClass()) || keyField.observable == null) {
                continue;
            }
            MethodSpec.Builder method = MethodSpec.methodBuilder(getKeyFieldObserverName(keyField.variableName));
            method.addModifiers(PUBLIC);
            for (VariableElement itemParameter: parametersList) {
                method.addParameter(ParameterSpec.builder(
                        TypeName.get(itemParameter.asType()),
                        itemParameter.getSimpleName().toString()
                ).build());
            }
            ParameterSpec.Builder lifecycleOwner = ParameterSpec.builder(
                    getLifecycleOwnerPackage(),
                    "owner"
            );
            String typeNameStr = typeName.toString();
            int firstIndex = typeNameStr.indexOf("<") + 1;
            int lastIndex = typeNameStr.lastIndexOf(">");
            typeNameStr = typeNameStr.substring(firstIndex, lastIndex);
            typeName = typeName.disassemble();
            method.addParameter(lifecycleOwner.build());
            ParameterSpec.Builder lifecycleObserver = ParameterSpec.builder(
                    ParameterizedTypeName.get(
                            ClassName.get("androidx.lifecycle", "Observer"),
                            typeName
                    ),
                    "observer"
            );
            method.addParameter(lifecycleObserver.build());
            if (parametersList.size() == 0) {
                method.addStatement(
                        "$N().observe(owner, observer)",
                        getKeyFieldReturnName(keyField.variableName)
                );
            } else {
                method.addStatement(
                        "$N($N).observe(owner, observer)",
                        getKeyFieldReturnName(keyField.variableName),
                        parseParams(parametersList)
                );
            }
            methods.add(method.build());
        }
        return methods;
    }

    private String parseParams(List<? extends VariableElement> parametersList) {
        StringBuilder params = new StringBuilder();
        for (VariableElement parameter: parametersList) {
            params.append(parameter.getSimpleName().toString());
            params.append(", ");
        }
        params = new StringBuilder(params.substring(0, params.length() - 2));
        return params.toString();
    }

    public class ClassBean {
        public String classPackage;
        public String className;

        public ClassBean(String classData) {
            this.classPackage = classData.substring(0, classData.lastIndexOf("."));
            this.className = classData.substring(classData.lastIndexOf(".") + 1);
        }

        public ClassBean(String classPackage, String className) {
            this.classPackage = classPackage;
            this.className = className;
        }

    }

    private TypeName getDaoRepositoryPackage() {
        return ClassName.bestGuess(getRepositoryClassName());
    }

    private TypeName getAtomDBPackage() {
        return TypeName.get(annotatedElementUtils.getTypeElement(getAtomDBName()).asType());
    }

    private TypeName getLifecycleOwnerPackage() {
        return TypeName.get(annotatedElementUtils.getTypeElement(PACKAGE_LIFECYCLE_OWNER).asType());
    }

    private TypeName getLifecycleObserverPackage() {
        return TypeName.get(annotatedElementUtils.getTypeElement(PACKAGE_LIFECYCLE_OBSERVER).asType());
    }

    private String getRepositoryClassName() {
        return annotatedClazz.entityName + REPOSITORY_CLASS_PREFIX;
    }

    private String getAtomDBName() {
        // full name of class with package
        // annotatedClazz.atomAnnotatedClass.packageName + "." + annotatedClazz.atomAnnotatedClass.entityName + "_AtomDB";
        return annotatedClazz.atomAnnotatedClass.entityName + AtomGenerator.ATOM_CLASS_PREFIX;
    }

    private String getClazzName() {
        return annotatedClazz.entityName + CLAZZ_PREFIX;
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

    private String getKeyFieldObserverName(String variableName) {
        return "observer" + StringUtils.toUpperCamel(variableName);
    }

    private String getKeyFieldPutName(String variableName) {
        return StringUtils.toLowerCamel(variableName);//"put" + StringUtils.toUpperCamel(variableName);
    }

    private String getRepositoryDaoFieldName() {
        return "mRepository";
    }

    private String getFieldReturnName(String keyName) {
        return getRepositoryDaoFieldName() + "." + keyName;
    }

}
