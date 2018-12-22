package org.inferred.freebuilder.processor.util;

import static org.inferred.freebuilder.processor.util.FunctionalType.maybeFunctionalType;
import static org.inferred.freebuilder.processor.util.ModelUtils.maybeDeclared;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.reflect.TypeToken;

import org.inferred.freebuilder.processor.util.testing.ModelRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;
import java.util.function.UnaryOperator;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

public class FunctionalTypeTest {

  public interface MyFunction {
    int doSomething(long a, String b);
  }

  @Rule public final ModelRule model = new ModelRule();

  @Test
  public void testDetectsBasicFunctionalInterface() {
    FunctionalType myFunction = functionalType(MyFunction.class);

    assertNotNull(myFunction);
    assertEquals("doSomething", myFunction.getMethodName());
    assertEquals(2, myFunction.getParameters().size());
    assertEquals("long", myFunction.getParameters().get(0).toString());
    assertEquals("java.lang.String", myFunction.getParameters().get(1).toString());
    assertEquals("int", myFunction.getReturnType().toString());
  }

  @Test
  public void testReturnsAbsentIfGivenNonFunctionalInterface() {
    FunctionalType iterator = functionalType(Iterator.class);

    assertNull(iterator);
  }

  @Test
  public void testHandlesGenericTypes() {
    FunctionalType unaryOperator = functionalType(new TypeToken<UnaryOperator<Boolean>>() {});

    assertNotNull(unaryOperator);
    assertEquals("apply", unaryOperator.getMethodName());
    assertEquals(1, unaryOperator.getParameters().size());
    assertEquals("java.lang.Boolean", unaryOperator.getParameters().get(0).toString());
    assertEquals("java.lang.Boolean", unaryOperator.getReturnType().toString());
  }

  @Test
  public void testIsAssignableAllowsBoxingAndUnboxing() {
    FunctionalType myFunction = functionalType(MyFunction.class);
    FunctionalType biFunction = functionalType(
        new TypeToken<BiFunction<Long, String, Integer>>() {});

    assertTrue(FunctionalType.isAssignable(biFunction, myFunction, model.typeUtils()));
  }

  @Test
  public void testIsAssignablePreventsTypeMismatches() {
    FunctionalType myFunction = functionalType(MyFunction.class);
    FunctionalType biFunction = functionalType(
        new TypeToken<BiFunction<String, Long, Integer>>() {});

    assertFalse(FunctionalType.isAssignable(biFunction, myFunction, model.typeUtils()));
  }

  @Test
  public void testUsesPrototypeIfMethodNotFound() {
    TypeElement myBuilder = model.newType(
        "package com.example;",
        "public class MyBuilder extends NotYetMade {",
        "  @Override public MyBuilder mapBar(" + LongUnaryOperator.class.getName() + " mapper) {",
        "    return super.mapBar(mapper);",
        "  }",
        "}");
    FunctionalType unaryOperator = functionalType(new TypeToken<UnaryOperator<Long>>() {});

    FunctionalType result = FunctionalType.functionalTypeAcceptedByMethod(
        (DeclaredType) myBuilder.asType(),
        "mapFoo",
        unaryOperator,
        model.elementUtils(),
        model.typeUtils());

    assertEquals(unaryOperator, result);
  }

  @Test
  public void testUsesUserChosenTypeIfPresent() {
    TypeElement myBuilder = model.newType(
        "package com.example;",
        "public class MyBuilder extends NotYetMade {",
        "  @Override public MyBuilder mapFoo(" + LongUnaryOperator.class.getName() + " mapper) {",
        "    return super.mapBar(mapper);",
        "  }",
        "}");
    FunctionalType unaryOperator = functionalType(new TypeToken<UnaryOperator<Long>>() {});
    FunctionalType longUnaryOperator = functionalType(new TypeToken<LongUnaryOperator>() {});

    FunctionalType result = FunctionalType.functionalTypeAcceptedByMethod(
        (DeclaredType) myBuilder.asType(),
        "mapFoo",
        unaryOperator,
        model.elementUtils(),
        model.typeUtils());

    assertEquals(longUnaryOperator, result);
  }

  @Test
  public void testHandlesVoidFunctions() {
    TypeElement myBuilder = model.newType(
        "package com.example;",
        "public class MyBuilder extends NotYetMade {",
        "  @Override public MyBuilder mutateFoo(" + LongConsumer.class.getName() + " mutator) {",
        "    return super.mapBar(mapper);",
        "  }",
        "}");
    FunctionalType consumer = functionalType(new TypeToken<Consumer<Long>>() {});
    FunctionalType longConsumer = functionalType(new TypeToken<LongConsumer>() {});

    FunctionalType result = FunctionalType.functionalTypeAcceptedByMethod(
        (DeclaredType) myBuilder.asType(),
        "mutateFoo",
        consumer,
        model.elementUtils(),
        model.typeUtils());

    assertEquals(longConsumer, result);
  }

  private interface IntListConsumer {
    void take(List<Integer> stuff);
  }

  private interface Foo {
    Foo mutate(IntListConsumer mutator);
  }

  @Test
  public void testHandlesWildcards() {
    // Get a functional type representing Consumer<? super List<Integer>>
    TypeElement list = model.elementUtils().getTypeElement(List.class.getName());
    TypeMirror integer = model.elementUtils().getTypeElement(Integer.class.getName()).asType();
    DeclaredType intList = model.typeUtils().getDeclaredType(list, integer);
    WildcardType superIntList = model.typeUtils().getWildcardType(null, intList);
    FunctionalType consumerSuperIntList = FunctionalType.consumer(superIntList);

    FunctionalType result = FunctionalType.functionalTypeAcceptedByMethod(
        (DeclaredType) model.typeElement(Foo.class).asType(),
        "mutate",
        consumerSuperIntList,
        model.elementUtils(),
        model.typeUtils());

    assertEquals(
        QualifiedName.of(IntListConsumer.class).withParameters(),
        result.getFunctionalInterface());
  }

  private interface Bar<T> {
    Bar<T> mutate(Consumer<List<T>> stuff);
  }

  @Test
  public void testPreservesGenericTypeInformation() {
    DeclaredType barInteger = model.typeUtils().getDeclaredType(
        model.typeElement(Bar.class), model.typeMirror(Integer.class));
    FunctionalType result = FunctionalType.functionalTypeAcceptedByMethod(
        barInteger,
        "mutate",
        functionalType(IntListConsumer.class),
        model.elementUtils(),
        model.typeUtils());


    assertEquals(
        QualifiedName.of(Consumer.class),
        result.getFunctionalInterface().getQualifiedName());
  }

  private FunctionalType functionalType(Class<?> cls) {
    DeclaredType declaredType = maybeDeclared(model.typeMirror(cls)).get();
    return maybeFunctionalType(declaredType, model.elementUtils(), model.typeUtils()).orNull();
  }

  private FunctionalType functionalType(TypeToken<?> type) {
    DeclaredType declaredType = maybeDeclared(model.typeMirror(type)).get();
    return maybeFunctionalType(declaredType, model.elementUtils(), model.typeUtils()).orNull();
  }
}
