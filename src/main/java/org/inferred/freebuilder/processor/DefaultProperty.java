/*
 * Copyright 2014 Google Inc. All rights reserved.
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
package org.inferred.freebuilder.processor;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.inferred.freebuilder.processor.BuilderMethods.getter;
import static org.inferred.freebuilder.processor.BuilderMethods.mapper;
import static org.inferred.freebuilder.processor.BuilderMethods.setter;
import static org.inferred.freebuilder.processor.CodeGenerator.UNSET_PROPERTIES;
import static org.inferred.freebuilder.processor.util.Block.methodBody;
import static org.inferred.freebuilder.processor.util.FunctionalType.functionalTypeAcceptedByMethod;
import static org.inferred.freebuilder.processor.util.FunctionalType.unaryOperator;
import static org.inferred.freebuilder.processor.util.ObjectsExcerpts.Nullability.NOT_NULLABLE;
import static org.inferred.freebuilder.processor.util.PreconditionExcerpts.checkNotNullInline;
import static org.inferred.freebuilder.processor.util.PreconditionExcerpts.checkNotNullPreamble;
import static org.inferred.freebuilder.processor.util.feature.FunctionPackage.FUNCTION_PACKAGE;

import com.google.common.base.Optional;

import org.inferred.freebuilder.processor.Metadata.Property;
import org.inferred.freebuilder.processor.util.Block;
import org.inferred.freebuilder.processor.util.Excerpt;
import org.inferred.freebuilder.processor.util.Excerpts;
import org.inferred.freebuilder.processor.util.FieldAccess;
import org.inferred.freebuilder.processor.util.FunctionalType;
import org.inferred.freebuilder.processor.util.ObjectsExcerpts;
import org.inferred.freebuilder.processor.util.PreconditionExcerpts;
import org.inferred.freebuilder.processor.util.SourceBuilder;

import javax.lang.model.type.TypeKind;

/** Default {@link PropertyCodeGenerator}, providing reference semantics for any type. */
class DefaultProperty extends PropertyCodeGenerator {

  static class Factory implements PropertyCodeGenerator.Factory {

    @Override
    public Optional<DefaultProperty> create(Config config) {
      Property property = config.getProperty();
      boolean hasDefault = config.getMethodsInvokedInBuilderConstructor()
          .contains(setter(property));
      FunctionalType mapperType = functionalTypeAcceptedByMethod(
          config.getBuilder(),
          mapper(property),
          unaryOperator(firstNonNull(property.getBoxedType(), property.getType())),
          config.getElements(),
          config.getTypes());
      return Optional.of(new DefaultProperty(
          config.getMetadata(), property, hasDefault, mapperType));
    }
  }

  private final boolean hasDefault;
  private final FunctionalType mapperType;
  private final TypeKind kind;

  DefaultProperty(
      Metadata metadata,
      Property property,
      boolean hasDefault,
      FunctionalType mapperType) {
    super(metadata, property);
    this.hasDefault = hasDefault;
    this.mapperType = mapperType;
    this.kind = property.getType().getKind();
  }

  @Override
  public Type getType() {
    return hasDefault ? Type.HAS_DEFAULT : Type.REQUIRED;
  }

  @Override
  public void addBuilderFieldDeclaration(SourceBuilder code) {
    code.addLine("private %s %s;", property.getType(), property.getField());
  }

  @Override
  public void addBuilderFieldAccessors(SourceBuilder code) {
    addSetter(code, metadata);
    addMapper(code, metadata);
    addGetter(code, metadata);
  }

  private void addSetter(SourceBuilder code, final Metadata metadata) {
    code.addLine("")
        .addLine("/**")
        .addLine(" * Sets the value to be returned by %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName());
    if (!kind.isPrimitive()) {
      code.addLine(" * @throws NullPointerException if {@code %s} is null", property.getName());
    }
    code.addLine(" */");
    addAccessorAnnotations(code);
    code.addLine("public %s %s(%s %s) {",
        metadata.getBuilder(), setter(property), property.getType(), property.getName());
    Block body = methodBody(code, property.getName());
    if (kind.isPrimitive()) {
      body.addLine("  %s = %s;", property.getField(), property.getName());
    } else {
      body.add(checkNotNullPreamble(property.getName()))
          .addLine("  %s = %s;", property.getField(), checkNotNullInline(property.getName()));
    }
    if (!hasDefault) {
      body.addLine("  %s.remove(%s.%s);",
          UNSET_PROPERTIES, metadata.getPropertyEnum(), property.getAllCapsName());
    }
    if ((metadata.getBuilder() == metadata.getGeneratedBuilder())) {
      body.addLine("  return this;");
    } else {
      body.addLine("  return (%s) this;", metadata.getBuilder());
    }
    code.add(body)
        .addLine("}");
  }

  private void addMapper(SourceBuilder code, final Metadata metadata) {
    if (!code.feature(FUNCTION_PACKAGE).unaryOperator().isPresent()) {
      return;
    }
    code.addLine("")
        .addLine("/**")
        .addLine(" * Replaces the value to be returned by %s",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" * by applying {@code mapper} to it and using the result.")
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName())
        .addLine(" * @throws NullPointerException if {@code mapper} is null");
    if (mapperType.canReturnNull()) {
      code.addLine(" * or returns null");
    }
    if (!hasDefault) {
      code.addLine(" * @throws IllegalStateException if the field has not been set");
    }
    code.addLine(" */")
        .add("public %s %s(%s mapper) {",
            metadata.getBuilder(),
            mapper(property),
            mapperType.getFunctionalInterface());
    if (!hasDefault) {
      code.add(PreconditionExcerpts.checkNotNull("mapper"));
    }
    code.addLine("  return %s(mapper.%s(%s()));",
            setter(property), mapperType.getMethodName(), getter(property))
        .addLine("}");
  }

  private void addGetter(SourceBuilder code, final Metadata metadata) {
    code.addLine("")
        .addLine("/**")
        .addLine(" * Returns the value that will be returned by %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()));
    if (!hasDefault) {
      code.addLine(" *")
          .addLine(" * @throws IllegalStateException if the field has not been set");
    }
    code.addLine(" */")
        .addLine("public %s %s() {", property.getType(), getter(property));
    if (!hasDefault) {
      Excerpt propertyIsSet = Excerpts.add("!%s.contains(%s.%s)",
              UNSET_PROPERTIES, metadata.getPropertyEnum(), property.getAllCapsName());
      code.add(PreconditionExcerpts.checkState(propertyIsSet, property.getName() + " not set"));
    }
    code.addLine("  return %s;", property.getField())
        .addLine("}");
  }

  @Override
  public void addValueFieldDeclaration(SourceBuilder code, FieldAccess finalField) {
    code.add("private final %s %s;\n", property.getType(), finalField);
  }

  @Override
  public void addFinalFieldAssignment(SourceBuilder code, Excerpt finalField, String builder) {
    code.addLine("%s = %s;", finalField, property.getField().on(builder));
  }

  @Override
  public void addMergeFromValue(Block code, String value) {
    Excerpt defaults = Declarations.freshBuilder(code, metadata).orNull();
    if (defaults != null) {
      code.add("if (");
      if (!hasDefault) {
        code.add("%s.contains(%s.%s) || ",
            UNSET_PROPERTIES.on(defaults), metadata.getPropertyEnum(), property.getAllCapsName());
      }
      code.add(ObjectsExcerpts.notEquals(
          Excerpts.add("%s.%s()", value, property.getGetterName()),
          Excerpts.add("%s.%s()", defaults, getter(property)),
          kind,
          NOT_NULLABLE));
      code.add(") {%n");
    }
    code.addLine("  %s(%s.%s());", setter(property), value, property.getGetterName());
    if (defaults != null) {
      code.addLine("}");
    }
  }

  @Override
  public void addMergeFromBuilder(Block code, String builder) {
    Excerpt base =
        hasDefault ? null : Declarations.upcastToGeneratedBuilder(code, metadata, builder);
    Excerpt defaults = Declarations.freshBuilder(code, metadata).orNull();
    if (defaults != null) {
      code.add("if (");
      if (!hasDefault) {
        code.add("!%s.contains(%s.%s) && ",
                UNSET_PROPERTIES.on(base), metadata.getPropertyEnum(), property.getAllCapsName())
            .add("(%s.contains(%s.%s) ||",
                UNSET_PROPERTIES.on(defaults),
                metadata.getPropertyEnum(),
                property.getAllCapsName());
      }
      code.add(ObjectsExcerpts.notEquals(
          Excerpts.add("%s.%s()", builder, getter(property)),
          Excerpts.add("%s.%s()", defaults, getter(property)),
          kind,
          NOT_NULLABLE));
      if (!hasDefault) {
        code.add(")");
      }
      code.add(") {%n");
    } else if (!hasDefault) {
      code.addLine("if (!%s.contains(%s.%s)) {",
          UNSET_PROPERTIES.on(base), metadata.getPropertyEnum(), property.getAllCapsName());
    }
    code.addLine("  %s(%s.%s());", setter(property), builder, getter(property));
    if (defaults != null || !hasDefault) {
      code.addLine("}");
    }
  }

  @Override
  public void addSetBuilderFromPartial(Block code, String builder) {
    if (!hasDefault) {
      code.add("if (!%s.contains(%s.%s)) {",
          UNSET_PROPERTIES, metadata.getPropertyEnum(), property.getAllCapsName());
    }
    code.addLine("  %s.%s(%s);", builder, setter(property), property.getField());
    if (!hasDefault) {
      code.addLine("}");
    }
  }

  @Override
  public void addSetFromResult(SourceBuilder code, Excerpt builder, Excerpt variable) {
    code.addLine("%s.%s(%s);", builder, setter(property), variable);
  }

  @Override
  public void addClearField(Block code) {
    Optional<Excerpt> defaults = Declarations.freshBuilder(code, metadata);
    // Cannot clear property without defaults
    if (defaults.isPresent()) {
      code.addLine("%s = %s;", property.getField(), property.getField().on(defaults.get()));
    }
  }
}
