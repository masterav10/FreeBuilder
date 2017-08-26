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

import static org.inferred.freebuilder.processor.BuilderFactory.TypeInference.EXPLICIT_TYPES;
import static org.inferred.freebuilder.processor.BuilderMethods.getBuilderMethod;
import static org.inferred.freebuilder.processor.BuilderMethods.mutator;
import static org.inferred.freebuilder.processor.BuilderMethods.setter;
import static org.inferred.freebuilder.processor.util.Block.methodBody;
import static org.inferred.freebuilder.processor.util.feature.FunctionPackage.FUNCTION_PACKAGE;

import org.inferred.freebuilder.processor.BuildableType.MergeBuilderMethod;
import org.inferred.freebuilder.processor.BuildableType.PartialToBuilderMethod;
import org.inferred.freebuilder.processor.Metadata.Property;
import org.inferred.freebuilder.processor.util.Block;
import org.inferred.freebuilder.processor.util.Excerpt;
import org.inferred.freebuilder.processor.util.Excerpts;
import org.inferred.freebuilder.processor.util.ModelUtils;
import org.inferred.freebuilder.processor.util.ParameterizedType;
import org.inferred.freebuilder.processor.util.PreconditionExcerpts;
import org.inferred.freebuilder.processor.util.SourceBuilder;
import org.inferred.freebuilder.processor.util.Variable;

import com.google.common.base.Optional;

/**
 * {@link PropertyCodeGenerator} for <b>buildable</b> types: that is, types with a Builder class
 * providing a similar API to proto or &#64;FreeBuilder:<ul>
 * <li> a public constructor, or static builder()/newBuilder() method;
 * <li> build(), buildPartial() and clear() methods; and
 * <li> a mergeWith(Value) method.
 * </ul>
 */
class BuildableProperty extends PropertyCodeGenerator {

  static class Factory implements PropertyCodeGenerator.Factory {

    @Override
    public Optional<BuildableProperty> create(Config config) {
      BuildableType type = BuildableType.create(
          config.getProperty().getType(), config.getElements(), config.getTypes()).orNull();
      if (type == null) {
        return Optional.absent();
      }

      return Optional.of(new BuildableProperty(config.getMetadata(), config.getProperty(), type));
    }
  }

  private final BuildableType type;

  private BuildableProperty(Metadata metadata, Property property, BuildableType type) {
    super(metadata, property);
    this.type = type;
  }

  @Override
  public void addBuilderFieldDeclaration(SourceBuilder code) {
    code.addLine("private Object %s = null;", property.getField());
  }

  @Override
  public void addBuilderFieldAccessors(SourceBuilder code) {
    addSetter(code, metadata);
    addSetterTakingBuilder(code, metadata);
    addMutate(code, metadata);
    addGetter(code, metadata);
  }

  private void addSetter(SourceBuilder code, Metadata metadata) {
    Variable builder = new Variable("builder");
    code.addLine("")
        .addLine("/**")
        .addLine(" * Sets the value to be returned by %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName())
        .addLine(" * @throws NullPointerException if {@code %s} is null", property.getName())
        .addLine(" */");
    addAccessorAnnotations(code);
    code.addLine("public %s %s(%s %s) {",
            metadata.getBuilder(),
            setter(property),
            property.getType(),
            property.getName());
    Block body = methodBody(code, property.getName())
        .add(PreconditionExcerpts.checkNotNull(property.getName()))
        .addLine("  if (%1$s == null || %1$s instanceof %2$s) {",
            property.getField(), ModelUtils.maybeAsTypeElement(property.getType()).get())
        .addLine("    %s = %s;", property.getField(), property.getName())
        .addLine("  } else {")
        .addLine("    %1$s %2$s %3$s = (%2$s) %4$s;",
            type.suppressUnchecked(), type.builderType(), builder, property.getField())
        .addLine("    %s.clear();", builder)
        .addLine("    %s.mergeFrom(%s);", builder, property.getName())
        .addLine("  }")
        .addLine("  return (%s) this;", metadata.getBuilder());
    code.add(body)
        .addLine("}");
  }

  private void addSetterTakingBuilder(SourceBuilder code, Metadata metadata) {
    code.addLine("")
        .addLine("/**")
        .addLine(" * Sets the value to be returned by %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName())
        .addLine(" * @throws NullPointerException if {@code builder} is null")
        .addLine(" */")
        .addLine("public %s %s(%s builder) {",
            metadata.getBuilder(),
            setter(property),
            type.builderType())
        .addLine("  return %s(builder.build());", setter(property))
        .addLine("}");
  }

  private void addMutate(SourceBuilder code, Metadata metadata) {
    ParameterizedType consumer = code.feature(FUNCTION_PACKAGE).consumer().orNull();
    if (consumer == null) {
      return;
    }
    code.addLine("")
        .addLine("/**")
        .addLine(" * Applies {@code mutator} to the builder for the value that will be")
        .addLine(" * returned by %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" *")
        .addLine(" * <p>This method mutates the builder in-place. {@code mutator} is a void")
        .addLine(" * consumer, so any value returned from a lambda will be ignored.")
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName())
        .addLine(" * @throws NullPointerException if {@code mutator} is null")
        .addLine(" */")
        .addLine("public %s %s(%s<%s> mutator) {",
            metadata.getBuilder(),
            mutator(property),
            consumer.getQualifiedName(),
            type.builderType())
        .add(methodBody(code, "mutator")
            .addLine("  mutator.accept(%s());", getBuilderMethod(property))
            .addLine("  return (%s) this;", metadata.getBuilder()))
        .addLine("}");
  }

  private void addGetter(SourceBuilder code, Metadata metadata) {
    Variable builder = new Variable("builder");
    Variable value = new Variable("value");
    code.addLine("")
        .addLine("/**")
        .addLine(" * Returns a builder for the value that will be returned by %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" */")
        .addLine("public %s %s() {", type.builderType(), getBuilderMethod(property));
    Block body = methodBody(code)
        .addLine("  if (%s == null) {", property.getField())
        .addLine("    %s = %s;",
            property.getField(),
            type.builderFactory().newBuilder(type.builderType(), EXPLICIT_TYPES))
        .addLine("  } else if (%s instanceof %s) {",
            property.getField(), ModelUtils.maybeAsTypeElement(property.getType()).get())
        .addLine("    %1$s %2$s %3$s = (%2$s) %4$s;",
            type.suppressUnchecked(), property.getType(), value, property.getField());
    if (type.partialToBuilder() == PartialToBuilderMethod.TO_BUILDER_AND_MERGE) {
      body.addLine("    %s = %s.toBuilder();", property.getField(), value);
    } else {
      body.addLine("    %s = %s",
              property.getField(),
            type.builderFactory().newBuilder(type.builderType(), EXPLICIT_TYPES))
          .addLine("        .mergeFrom(%s);", value);
    }
    body.addLine("  }")
        .addLine("  %1$s %2$s %3$s = (%2$s) %4$s;",
            type.suppressUnchecked(), type.builderType(), builder, property.getField())
        .addLine("  return %s;", builder);
    code.add(body)
        .addLine("}");
  }

  @Override
  public void addFinalFieldAssignment(Block code, Excerpt finalField, String builder) {
    addFieldAssignment(code, finalField, builder, "build");
  }

  @Override
  public void addPartialFieldAssignment(Block code, Excerpt finalField, String builder) {
    addFieldAssignment(code, finalField, builder, "buildPartial");
  }

  private void addFieldAssignment(
      SourceBuilder code,
      Excerpt finalField,
      String builder,
      String buildMethod) {
    Variable fieldBuilder = new Variable(property.getName() + "Builder");
    Variable fieldValue = new Variable(property.getName() + "Value");
    code.addLine("if (%s == null) {", property.getField().on(builder))
        .addLine("  %s = %s.%s();",
            finalField,
            type.builderFactory().newBuilder(type.builderType(), EXPLICIT_TYPES),
            buildMethod)
        .addLine("} else if (%s instanceof %s) {",
            property.getField().on(builder),
            ModelUtils.maybeAsTypeElement(property.getType()).get());
    if (type.suppressUnchecked() != Excerpts.empty()) {
      code.addLine("  %1$s %2$s %3$s = (%2$s) %4$s;",
              type.suppressUnchecked(),
              property.getType(),
              fieldValue,
              property.getField().on(builder))
          .addLine("  %s = %s;", finalField, fieldValue);
    } else {
      code.addLine("  %s = (%s) %s;",
              finalField, property.getType(), property.getField().on(builder));
    }
    code.addLine("} else {")
        .addLine("  %1$s %2$s %3$s = (%2$s) %4$s;",
            type.suppressUnchecked(),
            type.builderType(),
            fieldBuilder,
            property.getField().on(builder))
        .addLine("  %s = %s.%s();", finalField, fieldBuilder, buildMethod)
        .addLine("}");
  }

  @Override
  public void addMergeFromValue(Block code, String value) {
    code.addLine("if (%s == null) {", property.getField())
        .addLine("  %s = %s.%s();", property.getField(), value, property.getGetterName())
        .addLine("} else {")
        .addLine("  %s().mergeFrom(%s.%s());",
            getBuilderMethod(property), value, property.getGetterName())
        .addLine("}");
  }

  @Override
  public void addMergeFromBuilder(Block code, String builder) {
    Excerpt base = Declarations.upcastToGeneratedBuilder(code, metadata, builder);
    Variable fieldValue = new Variable(property.getName() + "Value");
    code.addLine("if (%s == null) {", property.getField().on(base))
        .addLine("  // Nothing to merge")
        .addLine("} else if (%s instanceof %s) {",
            property.getField().on(base),
            ModelUtils.maybeAsTypeElement(property.getType()).get())
        .addLine("  %1$s %2$s %3$s = (%2$s) %4$s;",
           type.suppressUnchecked(), property.getType(), fieldValue, property.getField().on(base))
        .addLine("  if (%s == null) {", property.getField())
        .addLine("    %s = %s;", property.getField(), fieldValue)
        .addLine("  } else {")
        .addLine("    %s().mergeFrom(%s);", getBuilderMethod(property), fieldValue)
        .addLine("  }")
        .addLine("} else {")
        .add("  %s().mergeFrom(%s.%s()",
            getBuilderMethod(property), base, getBuilderMethod(property));
    if (type.mergeBuilder() == MergeBuilderMethod.BUILD_PARTIAL_AND_MERGE) {
      code.add(".buildPartial()");
    }
    code.add(");\n")
        .addLine("}");
  }

  @Override
  public void addSetBuilderFromPartial(Block code, String builder) {
    if (type.partialToBuilder() == PartialToBuilderMethod.TO_BUILDER_AND_MERGE) {
      code.add("%s.%s().mergeFrom(%s.toBuilder());",
          builder, getBuilderMethod(property), property.getField());
    } else {
      code.add("%s.%s().mergeFrom(%s);",
          builder, getBuilderMethod(property), property.getField());
    }
  }

  @Override
  public void addSetFromResult(SourceBuilder code, Excerpt builder, Excerpt variable) {
    code.addLine("%s.%s(%s);", builder, setter(property), variable);
  }

  @Override
  public void addClearField(Block code) {
    Variable fieldBuilder = new Variable(property.getName() + "Builder");
    code.addLine("  if (%1$s == null || %1$s instanceof %2$s) {",
            property.getField(),
            ModelUtils.maybeAsTypeElement(property.getType()).get())
        .addLine("    %s = null;", property.getField())
        .addLine("  } else {")
        .addLine("    %1$s %2$s %3$s = (%2$s) %4$s;",
            type.suppressUnchecked(), type.builderType(), fieldBuilder, property.getField())
        .addLine("    %s.clear();", fieldBuilder)
        .addLine("  }");
  }
}
