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
package org.inferred.freebuilder.processor.util;

import static com.google.common.truth.Truth.assertThat;
import static org.inferred.freebuilder.processor.util.ClassTypeImpl.STRING;
import static org.inferred.freebuilder.processor.util.ClassTypeImpl.newNestedClass;
import static org.inferred.freebuilder.processor.util.ClassTypeImpl.newTopLevelClass;
import static org.junit.Assert.assertEquals;

import com.google.common.reflect.TypeToken;

import org.inferred.freebuilder.processor.util.ClassTypeImpl.ClassElementImpl;
import org.inferred.freebuilder.processor.util.testing.Model;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.lang.model.element.Name;

@RunWith(JUnit4.class)
public class ImportManagerTest {

  // Models are expensive to create, so tests share a lazy singleton
  private static Model model;

  private static void createModel() {
    if (model == null) {
      model = Model.create();
    }
  }

  @AfterClass
  public static void destroySharedModel() {
    if (model != null) {
      model.destroy();
      model = null;
    }
  }

  @Test
  public void testTypeMirrorShortening() {
    ImportManager manager = new ImportManager.Builder().build();
    assertEquals("String", manager.shorten(STRING));
    assertEquals("List", manager.shorten(newTopLevelClass("java.util.List")));
    assertEquals("java.awt.List", manager.shorten(newTopLevelClass("java.awt.List")));
    ClassTypeImpl mapType = newTopLevelClass("java.util.Map");
    assertEquals("Map", manager.shorten(mapType));
    assertEquals("Map.Entry", manager.shorten(newNestedClass(mapType.asElement(), "Entry")));
    assertThat(manager.getClassImports())
        .containsExactly("java.util.List", "java.util.Map").inOrder();
  }

  @Test
  public void testTypeMirrorShortening_withConflicts() {
    ClassElementImpl listType = newTopLevelClass("org.example.List").asElement();
    ClassElementImpl stringType = newNestedClass(listType, "String").asElement();
    ImportManager manager = new ImportManager.Builder()
        .addImplicitImport(QualifiedName.of(listType))
        .addImplicitImport(QualifiedName.of(stringType))
        .build();
    assertEquals("java.lang.String",
        manager.shorten(ClassTypeImpl.STRING));
    assertEquals("java.util.List", manager.shorten(newTopLevelClass("java.util.List")));
    ClassTypeImpl awtListType = newTopLevelClass("java.awt.List");
    assertEquals("java.awt.List", manager.shorten(awtListType));
    assertEquals("java.awt.List.Sucks",
        manager.shorten(newNestedClass(awtListType.asElement(), "Sucks")));
    assertEquals("Map", manager.shorten(newTopLevelClass("java.util.Map")));
    assertEquals("List", manager.shorten(listType.asType()));
    assertEquals("List.String", manager.shorten(stringType.asType()));
    assertThat(manager.getClassImports()).containsExactly("java.util.Map");
  }

  @Test
  public void testTypeMirrorShortening_nestedGenericClass() {
    createModel();
    ImportManager manager = new ImportManager.Builder().build();
    assertEquals("Map.Entry<String, List<Double>>",
        manager.shorten(model.typeMirror(new TypeToken<Map.Entry<String, List<Double>>>() {})));
    assertThat(manager.getClassImports())
        .containsExactly("java.util.List", "java.util.Map").inOrder();
  }

  @Test
  public void testTypeMirrorShortening_innerClassOnGenericType() {
    createModel();
    ImportManager manager = new ImportManager.Builder().build();
    assertEquals("ImportManagerTest.OuterClass<List<String>>.InnerClass",
        manager.shorten(model.typeMirror(
            new TypeToken<ImportManagerTest.OuterClass<List<String>>.InnerClass>() {})));
    assertThat(manager.getClassImports())
        .containsExactly("java.util.List", this.getClass().getCanonicalName()).inOrder();
  }

  @Test
  public void testTypeMirrorShortening_typeVariables() {
    createModel();
    ImportManager manager = new ImportManager.Builder().build();

    // Type variables should be printed as name only, not e.g. "E extends Enum<E>"
    assertEquals("Enum<E>", manager.shorten(model.typeElement(Enum.class).asType()));
  }

  @Test
  public void testTypeMirrorShortening_wildcards() {
    createModel();
    ImportManager manager = new ImportManager.Builder().build();

    assertEquals("Map<Name, ? extends Logger>",
        manager.shorten(model.typeMirror(new TypeToken<Map<Name, ? extends Logger>>() {})));
    assertThat(manager.getClassImports())
        .containsExactly(
            "java.util.Map", "java.util.logging.Logger", "javax.lang.model.element.Name")
        .inOrder();
  }

  @Test
  public void testTypeReferenceShortening() {
    ImportManager manager = new ImportManager.Builder().build();
    assertEquals("String", manager.shorten(QualifiedName.of("java.lang", "String")));
    assertEquals("List", manager.shorten(QualifiedName.of("java.util", "List")));
    assertEquals("java.awt.List", manager.shorten(QualifiedName.of("java.awt", "List")));
    assertEquals("Map", manager.shorten(QualifiedName.of("java.util", "Map")));
    assertEquals("Map.Entry", manager.shorten(QualifiedName.of("java.util", "Map", "Entry")));
    assertThat(manager.getClassImports())
        .containsExactly("java.util.List", "java.util.Map").inOrder();
  }

  @Test
  public void testTypeReferenceShortening_withConflicts() {
    ClassElementImpl listType = newTopLevelClass("org.example.List").asElement();
    ClassElementImpl stringType = newNestedClass(listType, "String").asElement();
    ImportManager manager = new ImportManager.Builder()
        .addImplicitImport(QualifiedName.of(listType))
        .addImplicitImport(QualifiedName.of(stringType))
        .build();
    assertEquals("java.lang.String", manager.shorten(QualifiedName.of("java.lang", "String")));
    assertEquals("java.util.List", manager.shorten(QualifiedName.of("java.util", "List")));
    assertEquals("java.awt.List", manager.shorten(QualifiedName.of("java.awt", "List")));
    assertEquals("Map", manager.shorten(QualifiedName.of("java.util", "Map")));
    assertThat(manager.getClassImports()).containsExactly("java.util.Map");
  }

  private static class OuterClass<T> {
    private class InnerClass { }
  }
}
