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

import static org.inferred.freebuilder.processor.BuilderMethods.addAllMethod;
import static org.inferred.freebuilder.processor.BuilderMethods.addMethod;
import static org.inferred.freebuilder.processor.BuilderMethods.clearMethod;
import static org.inferred.freebuilder.processor.BuilderMethods.getter;
import static org.inferred.freebuilder.processor.BuilderMethods.mutator;
import static org.inferred.freebuilder.processor.BuilderMethods.removeMethod;
import static org.inferred.freebuilder.processor.BuilderMethods.setComparatorMethod;
import static org.inferred.freebuilder.processor.Util.erasesToAnyOf;
import static org.inferred.freebuilder.processor.Util.upperBound;
import static org.inferred.freebuilder.processor.util.Block.methodBody;
import static org.inferred.freebuilder.processor.util.FunctionalType.consumer;
import static org.inferred.freebuilder.processor.util.FunctionalType.functionalTypeAcceptedByMethod;
import static org.inferred.freebuilder.processor.util.ModelUtils.maybeDeclared;
import static org.inferred.freebuilder.processor.util.ModelUtils.maybeUnbox;
import static org.inferred.freebuilder.processor.util.ModelUtils.needsSafeVarargs;
import static org.inferred.freebuilder.processor.util.ModelUtils.overrides;
import static org.inferred.freebuilder.processor.util.PreconditionExcerpts.checkNotNullInline;
import static org.inferred.freebuilder.processor.util.PreconditionExcerpts.checkNotNullPreamble;
import static org.inferred.freebuilder.processor.util.feature.FunctionPackage.FUNCTION_PACKAGE;
import static org.inferred.freebuilder.processor.util.feature.GuavaLibrary.GUAVA;
import static org.inferred.freebuilder.processor.util.feature.SourceLevel.SOURCE_LEVEL;
import static org.inferred.freebuilder.processor.util.feature.SourceLevel.diamondOperator;
import static org.inferred.freebuilder.processor.util.feature.SourceLevel.nestedDiamondOperator;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSortedSet;

import org.inferred.freebuilder.processor.Metadata.Property;
import org.inferred.freebuilder.processor.excerpt.CheckedNavigableSet;
import org.inferred.freebuilder.processor.util.Block;
import org.inferred.freebuilder.processor.util.Excerpt;
import org.inferred.freebuilder.processor.util.Excerpts;
import org.inferred.freebuilder.processor.util.FunctionalType;
import org.inferred.freebuilder.processor.util.ParameterizedType;
import org.inferred.freebuilder.processor.util.PreconditionExcerpts;
import org.inferred.freebuilder.processor.util.QualifiedName;
import org.inferred.freebuilder.processor.util.SourceBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * {@link PropertyCodeGenerator} providing fluent methods for {@link SortedSet} properties.
 */
class SortedSetProperty extends PropertyCodeGenerator {

  static class Factory implements PropertyCodeGenerator.Factory {

    @Override
    public Optional<SortedSetProperty> create(Config config) {
      DeclaredType type = maybeDeclared(config.getProperty().getType()).orNull();
      if (type == null || !erasesToAnyOf(type, SortedSet.class, ImmutableSortedSet.class)) {
        return Optional.absent();
      }

      TypeMirror elementType = upperBound(config.getElements(), type.getTypeArguments().get(0));
      Optional<TypeMirror> unboxedType = maybeUnbox(elementType, config.getTypes());
      boolean needsSafeVarargs = needsSafeVarargs(unboxedType.or(elementType));
      boolean overridesAddMethod = hasAddMethodOverride(config, unboxedType.or(elementType));
      boolean overridesVarargsAddMethod =
          hasVarargsAddMethodOverride(config, unboxedType.or(elementType));

      FunctionalType mutatorType = functionalTypeAcceptedByMethod(
          config.getBuilder(),
          mutator(config.getProperty()),
          consumer(wildcardSuperSortedSet(elementType, config.getElements(), config.getTypes())),
          config.getElements(),
          config.getTypes());

      return Optional.of(new SortedSetProperty(
          config.getMetadata(),
          config.getProperty(),
          elementType,
          unboxedType,
          mutatorType,
          needsSafeVarargs,
          overridesAddMethod,
          overridesVarargsAddMethod));
    }

    private static boolean hasAddMethodOverride(Config config, TypeMirror elementType) {
      return overrides(
          config.getBuilder(),
          config.getTypes(),
          addMethod(config.getProperty()),
          elementType);
    }

    private static boolean hasVarargsAddMethodOverride(Config config, TypeMirror elementType) {
      return overrides(
          config.getBuilder(),
          config.getTypes(),
          addMethod(config.getProperty()),
          config.getTypes().getArrayType(elementType));
    }

    private static TypeMirror wildcardSuperSortedSet(
        TypeMirror elementType,
        Elements elements,
        Types types) {
      TypeElement setType = elements.getTypeElement(SortedSet.class.getName());
      return types.getWildcardType(null, types.getDeclaredType(setType, elementType));
    }
  }

  private static final ParameterizedType COLLECTION =
      QualifiedName.of(Collection.class).withParameters("E");
  private final TypeMirror elementType;
  private final Optional<TypeMirror> unboxedType;
  private final FunctionalType mutatorType;
  private final boolean needsSafeVarargs;
  private final boolean overridesAddMethod;
  private final boolean overridesVarargsAddMethod;

  SortedSetProperty(
      Metadata metadata,
      Property property,
      TypeMirror elementType,
      Optional<TypeMirror> unboxedType,
      FunctionalType mutatorType,
      boolean needsSafeVarargs,
      boolean overridesAddMethod,
      boolean overridesVarargsAddMethod) {
    super(metadata, property);
    this.elementType = elementType;
    this.unboxedType = unboxedType;
    this.mutatorType = mutatorType;
    this.needsSafeVarargs = needsSafeVarargs;
    this.overridesAddMethod = overridesAddMethod;
    this.overridesVarargsAddMethod = overridesVarargsAddMethod;
  }

  @Override
  public void addBuilderFieldDeclaration(SourceBuilder code) {
    code.addLine("private %s<%s> %s = null;", NavigableSet.class, elementType, property.getField());
  }

  @Override
  public void addBuilderFieldAccessors(SourceBuilder code) {
    addSetComparator(code, metadata);
    addAdd(code, metadata);
    addVarargsAdd(code, metadata);
    addAddAllMethods(code, metadata);
    addRemove(code, metadata);
    addMutator(code, metadata);
    addClear(code, metadata);
    addGetter(code, metadata);
  }

  private void addSetComparator(SourceBuilder code, Metadata metadata) {
    code.addLine("")
        .addLine("/**")
        .addLine(" * Sets the comparator of the set to be returned from %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" *")
        .addLine(" * Pass in {@code null} to use the {@linkplain Comparable natural ordering}")
        .addLine(" * of the elements.")
        .addLine(" *")
        .addLine(" * <p>If the set is accessed without calling this method first, the comparator")
        .addLine(" * will default to {@code null}, and cannot subsequently be changed.")
        .addLine(" * (Note that this immutability is an implementation detail that may change in")
        .addLine(" * future; it should not be relied on for correctness.)")
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName())
        .addLine(" * @throws IllegalStateException if the set has been accessed at all,")
        .addLine(" *     whether by adding an element, setting the comparator, or calling")
        .addLine(" *     {@link #%s()}.", getter(property));
    code.addLine(" */")
        .addLine("protected %s %s(%s<? super %s> comparator) {",
            metadata.getBuilder(),
            setComparatorMethod(property),
            Comparator.class,
            elementType);
    Block body = methodBody(code, "comparator");
    body.add(PreconditionExcerpts.checkState(
            Excerpts.add("%s == null", property.getField()),
            "Comparator already set for %s",
            property.getField()));
    if (body.feature(GUAVA).isAvailable()) {
      body.addLine("  if (comparator == null) {")
          .addLine("    %s = %s.of();", property.getField(), ImmutableSortedSet.class)
          .addLine("  } else {")
          .addLine("    %s = new %s<%s>(comparator).build();",
              property.getField(), ImmutableSortedSet.Builder.class, elementType)
          .addLine("  }");
    } else {
      body.addLine("  %s = new %s%s(comparator);",
          property.getField(), TreeSet.class, diamondOperator(elementType));
    }
    body.addLine("  return (%s) this;", metadata.getBuilder());
    code.add(body)
        .addLine("}");
  }

  private void addAdd(SourceBuilder code, Metadata metadata) {
    code.addLine("")
        .addLine("/**")
        .addLine(" * Adds {@code element} to the set to be returned from %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" * If the set already contains {@code element}, then {@code %s}",
            addMethod(property))
        .addLine(" * has no effect (only the previously added element is retained).")
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName());
    if (!unboxedType.isPresent()) {
      code.addLine(" * @throws NullPointerException if {@code element} is null");
    }
    code.addLine(" */")
        .addLine("public %s %s(%s element) {",
            metadata.getBuilder(),
            addMethod(property),
            unboxedType.or(elementType));
    Block body = methodBody(code, "element");
    addConvertToTreeSet(body);
    if (unboxedType.isPresent()) {
      body.addLine("  %s.add(element);", property.getField());
    } else {
      body.add(checkNotNullPreamble("element"))
          .addLine("  %s.add(%s);", property.getField(), checkNotNullInline("element"));
    }
    body.addLine("  return (%s) this;", metadata.getBuilder());
    code.add(body)
        .addLine("}");
  }

  private void addConvertToTreeSet(SourceBuilder code) {
    code.addLine("  if (%s == null) {", property.getField())
        .addLine("    // Use default comparator")
        .addLine("    %s = new %s%s();",
            property.getField(), TreeSet.class, diamondOperator(elementType));
    if (code.feature(GUAVA).isAvailable()) {
      code.addLine("  } else if (%s instanceof %s) {",
              property.getField(), ImmutableSortedSet.class)
          .addLine("    %1$s = new %2$s%3$s(%1$s);",
              property.getField(), TreeSet.class, diamondOperator(elementType));
    }
    code.addLine("  }");
  }

  private void addVarargsAdd(SourceBuilder code, Metadata metadata) {
    code.addLine("")
        .addLine("/**")
        .addLine(" * Adds each element of {@code elements} to the set to be returned from")
        .addLine(" * %s, ignoring duplicate elements",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" * (only the first duplicate element is added).")
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName());
    if (!unboxedType.isPresent()) {
      code.addLine(" * @throws NullPointerException if {@code elements} is null or contains a")
          .addLine(" *     null element");
    }
    code.addLine(" */");
    QualifiedName safeVarargs = code.feature(SOURCE_LEVEL).safeVarargs().orNull();
    if (safeVarargs != null && needsSafeVarargs) {
      if (!overridesVarargsAddMethod) {
        code.addLine("@%s", safeVarargs)
            .addLine("@%s({\"varargs\"})", SuppressWarnings.class);
      } else {
        code.addLine("@%s({\"unchecked\", \"varargs\"})", SuppressWarnings.class);
      }
    }
    code.add("public ");
    if (safeVarargs != null && needsSafeVarargs && !overridesVarargsAddMethod) {
      code.add("final ");
    }
    code.add("%s %s(%s... elements) {\n",
            metadata.getBuilder(),
            addMethod(property),
            unboxedType.or(elementType));
    Optional<Class<?>> arrayUtils = code.feature(GUAVA).arrayUtils(unboxedType.or(elementType));
    if (arrayUtils.isPresent()) {
      code.addLine("  return %s(%s.asList(elements));", addAllMethod(property), arrayUtils.get());
    } else {
      // Primitive type, Guava not available
      code.addLine("  for (%s element : elements) {", elementType)
          .addLine("    %s(element);", addMethod(property))
          .addLine("  }")
          .addLine("  return (%s) this;", metadata.getBuilder());
    }
    code.addLine("}");
  }

  private void addAddAllMethods(SourceBuilder code, Metadata metadata) {
    if (code.feature(SOURCE_LEVEL).stream().isPresent()) {
      addSpliteratorAddAll(code, metadata);
      addStreamAddAll(code, metadata);
    }
    addIterableAddAll(code, metadata);
  }

  private void addSpliteratorAddAll(SourceBuilder code, Metadata metadata) {
    QualifiedName spliterator = code.feature(SOURCE_LEVEL).spliterator().get();
    addJavadocForAddAll(code, metadata);
    code.addLine("public %s %s(%s<? extends %s> elements) {",
            metadata.getBuilder(),
            addAllMethod(property),
            spliterator,
            elementType)
        .addLine("  elements.forEachRemaining(this::%s);", addMethod(property))
        .addLine("  return (%s) this;", metadata.getBuilder())
        .addLine("}");
  }

  private void addStreamAddAll(SourceBuilder code, Metadata metadata) {
    QualifiedName baseStream = code.feature(SOURCE_LEVEL).baseStream().get();
    addJavadocForAddAll(code, metadata);
    code.addLine("public %s %s(%s<? extends %s, ?> elements) {",
            metadata.getBuilder(),
            addAllMethod(property),
            baseStream,
            elementType)
        .addLine("  return %s(elements.spliterator());", addAllMethod(property))
        .addLine("}");
  }

  private void addIterableAddAll(SourceBuilder code, Metadata metadata) {
    addJavadocForAddAll(code, metadata);
    addAccessorAnnotations(code);
    code.addLine("public %s %s(%s<? extends %s> elements) {",
            metadata.getBuilder(),
            addAllMethod(property),
            Iterable.class,
            elementType)
        .add(Excerpts.forEach(unboxedType.or(elementType), "elements", addMethod(property)))
        .addLine("  return (%s) this;", metadata.getBuilder())
        .addLine("}");
  }

  private void addJavadocForAddAll(SourceBuilder code, Metadata metadata) {
    code.addLine("")
        .addLine("/**")
        .addLine(" * Adds each element of {@code elements} to the set to be returned from")
        .addLine(" * %s, ignoring duplicate elements",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" * (only the first duplicate element is added).")
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName())
        .addLine(" * @throws NullPointerException if {@code elements} is null or contains a")
        .addLine(" *     null element")
        .addLine(" */");
  }

  private void addRemove(SourceBuilder code, Metadata metadata) {
    code.addLine("")
        .addLine("/**")
        .addLine(" * Removes {@code element} from the set to be returned from %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" * Does nothing if {@code element} is not a member of the set.")
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName());
    if (!unboxedType.isPresent()) {
      code.addLine(" * @throws NullPointerException if {@code element} is null");
    }
    code.addLine(" */")
        .addLine("public %s %s(%s element) {",
            metadata.getBuilder(),
            removeMethod(property),
            unboxedType.or(elementType));
    Block body = methodBody(code, "mutator");
    addConvertToTreeSet(body);
    if (unboxedType.isPresent()) {
      body.addLine("  %s.remove(element);", property.getField());
    } else {
      body.add(checkNotNullPreamble("element"))
          .addLine("  %s.remove(%s);", property.getField(), checkNotNullInline("element"));
    }
    body.addLine("  return (%s) this;", metadata.getBuilder());
    code.add(body)
        .addLine("}");
  }

  private void addMutator(SourceBuilder code, Metadata metadata) {
    ParameterizedType consumer = code.feature(FUNCTION_PACKAGE).consumer().orNull();
    if (consumer == null) {
      return;
    }
    code.addLine("")
        .addLine("/**")
        .addLine(" * Applies {@code mutator} to the set to be returned from %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" *")
        .addLine(" * <p>This method mutates the set in-place. {@code mutator} is a void")
        .addLine(" * consumer, so any value returned from a lambda will be ignored. Take care")
        .addLine(" * not to call pure functions, like %s.",
            COLLECTION.javadocNoArgMethodLink("stream"))
        .addLine(" *")
        .addLine(" * @return this {@code Builder} object")
        .addLine(" * @throws NullPointerException if {@code mutator} is null")
        .addLine(" */")
        .addLine("public %s %s(%s mutator) {",
            metadata.getBuilder(),
            mutator(property),
            mutatorType.getFunctionalInterface());
    Block body = methodBody(code, "mutator");
    addConvertToTreeSet(body);
    if (overridesAddMethod) {
      body.addLine("  mutator.%s(new %s<%s>(%s, this::%s));",
          mutatorType.getMethodName(),
          CheckedNavigableSet.TYPE,
          elementType,
          property.getField(),
          addMethod(property));
    } else {
      body.addLine("  // If %s is overridden, this method will be updated to delegate to it",
              addMethod(property))
          .addLine("  mutator.%s(%s);", mutatorType.getMethodName(), property.getField());
    }
    body.addLine("  return (%s) this;", metadata.getBuilder());
    code.add(body)
        .addLine("}");
  }

  private void addClear(SourceBuilder code, Metadata metadata) {
    code.addLine("")
        .addLine("/**")
        .addLine(" * Clears the set to be returned from %s.",
            metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" *")
        .addLine(" * @return this {@code %s} object", metadata.getBuilder().getSimpleName())
        .addLine(" */")
        .addLine("public %s %s() {", metadata.getBuilder(), clearMethod(property));
    if (code.feature(GUAVA).isAvailable()) {
      code.addLine("  if (%s instanceof %s) {", property.getField(), ImmutableSortedSet.class)
          .addLine("    if (%s.isEmpty()) {", property.getField())
          .addLine("       // Do nothing")
          .addLine("    } else if (%s.comparator() != null) {", property.getField())
          .addLine("      %1$s = new %2$s<%3$s>(%1$s.comparator()).build();",
              property.getField(), ImmutableSortedSet.Builder.class, elementType)
          .addLine("    } else {")
          .addLine("      %s = %s.of();", property.getField(), ImmutableSortedSet.class)
          .addLine("    }")
          .add("  } else ");
    }
    code.addLine("  if (%s != null) {", property.getField())
        .addLine("    %s.clear();", property.getField())
        .addLine("  }")
        .addLine("  return (%s) this;", metadata.getBuilder())
        .addLine("}");
  }

  private void addGetter(SourceBuilder code, Metadata metadata) {
    code.addLine("")
        .addLine("/**")
        .addLine(" * Returns an unmodifiable view of the set that will be returned by")
        .addLine(" * %s.", metadata.getType().javadocNoArgMethodLink(property.getGetterName()))
        .addLine(" * Changes to this builder will be reflected in the view.")
        .addLine(" */")
        .addLine("public %s<%s> %s() {", SortedSet.class, elementType, getter(property));
    addConvertToTreeSet(code);
    code.addLine("  return %s.unmodifiableSortedSet(%s);", Collections.class, property.getField())
        .addLine("}");
  }

  @Override
  public void addFinalFieldAssignment(SourceBuilder code, Excerpt finalField, String builder) {
    code.addLine("if (%s == null) {", property.getField().on(builder));
    if (code.feature(GUAVA).isAvailable()) {
      code.addLine("  %s = %s.<%s>of();",
              finalField, ImmutableSortedSet.class, elementType)
          .addLine("} else if (%s instanceof %s) {",
              property.getField().on(builder), ImmutableSortedSet.class)
          .addLine("  %s = (%s<%s>) %s;",
              finalField, ImmutableSortedSet.class, elementType, property.getField().on(builder))
          .addLine("} else {")
          .addLine("  %s = %s.copyOfSorted(%s);",
              finalField, ImmutableSortedSet.class, property.getField().on(builder));
    } else {
      code.addLine("  %s = %s.unmodifiableSortedSet(new %s%s());",
              finalField,
              Collections.class,
              TreeSet.class,
              nestedDiamondOperator(elementType))
          .addLine("} else {")
          .addLine("  %s = %s.unmodifiableSortedSet(new %s%s(%s));",
              finalField,
              Collections.class,
              TreeSet.class,
              diamondOperator(elementType),
              property.getField().on(builder));
    }
    code.addLine("}");
  }

  @Override
  public void addMergeFromValue(Block code, String value) {
    if (code.feature(GUAVA).isAvailable()) {
      code.addLine("if (%s instanceof %s", value, metadata.getValueType().getQualifiedName())
          .addLine("      && (%s == null", property.getField())
          .addLine("          || (%s instanceof %s ",
              property.getField(), ImmutableSortedSet.class)
          .addLine("              && %s.isEmpty()", property.getField())
          .addLine("              && %s))) {",
              Excerpts.equals(
                  Excerpts.add("%s.comparator()", property.getField()),
                  Excerpts.add("%s.%s().comparator()", value, property.getGetterName())))
          .addLine("  @%s(\"unchecked\")", SuppressWarnings.class)
          .addLine("  %1$s<%2$s> _temporary = (%1$s<%2$s>) (%1$s<?>) %3$s.%4$s();",
              ImmutableSortedSet.class,
              elementType,
              value,
              property.getGetterName())
          .addLine("  %s = _temporary;", property.getField())
          .addLine("} else {");
    }
    code.addLine("%s(%s.%s());", addAllMethod(property), value, property.getGetterName());
    if (code.feature(GUAVA).isAvailable()) {
      code.addLine("}");
    }
  }

  @Override
  public void addMergeFromBuilder(Block code, String builder) {
    Excerpt base = Declarations.upcastToGeneratedBuilder(code, metadata, builder);
    code.addLine("if (%s != null) {", property.getField().on(base))
        .addLine("  %s(%s);", addAllMethod(property), property.getField().on(base))
        .addLine("}");
  }

  @Override
  public void addSetFromResult(SourceBuilder code, Excerpt builder, Excerpt variable) {
    code.addLine("%s.%s(%s);", builder, addAllMethod(property), variable);
  }

  @Override
  public void addClearField(Block code) {
    code.addLine("%s();", clearMethod(property));
  }
}
