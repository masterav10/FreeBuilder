/*
 * Copyright 2015 Google Inc. All rights reserved.
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

import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.joining;
import static org.inferred.freebuilder.processor.util.ClassTypeImpl.INTEGER;
import static org.inferred.freebuilder.processor.util.ClassTypeImpl.STRING;
import static org.inferred.freebuilder.processor.util.FunctionalType.intUnaryOperator;
import static org.inferred.freebuilder.processor.util.FunctionalType.unaryOperator;
import static org.inferred.freebuilder.processor.util.PrimitiveTypeImpl.INT;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

import org.inferred.freebuilder.processor.Metadata.Property;
import org.inferred.freebuilder.processor.util.QualifiedName;
import org.inferred.freebuilder.processor.util.SourceBuilder;
import org.inferred.freebuilder.processor.util.SourceStringBuilder;
import org.inferred.freebuilder.processor.util.feature.Feature;
import org.inferred.freebuilder.processor.util.feature.GuavaLibrary;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.stream.Stream;

@RunWith(JUnit4.class)
public class RequiredPropertiesSourceTest {

  @Test
  public void testJ8_noGuava() {
    Metadata metadata = createMetadata(true);

    assertThat(generateSource(metadata)).isEqualTo(Stream.of(
        "/** Auto-generated superclass of {@link Person.Builder}, "
            + "derived from the API of {@link Person}. */",
        "abstract class Person_Builder {",
        "",
        "  /** Creates a new builder using {@code value} as a template. */",
        "  public static Person.Builder from(Person value) {",
        "    return new Person.Builder().mergeFrom(value);",
        "  }",
        "",
        "  private enum Property {",
        "    NAME(\"name\"),",
        "    AGE(\"age\"),",
        "    ;",
        "",
        "    private final String name;",
        "",
        "    private Property(String name) {",
        "      this.name = name;",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return name;",
        "    }",
        "  }",
        "",
        "  private String name;",
        "  private int age;",
        "  private final EnumSet<Person_Builder.Property> _unsetProperties =",
        "      EnumSet.allOf(Person_Builder.Property.class);",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code name} is null",
        "   */",
        "  public Person.Builder setName(String name) {",
        "    this.name = Objects.requireNonNull(name);",
        "    _unsetProperties.remove(Person_Builder.Property.NAME);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Replaces the value to be returned by {@link Person#getName()} by applying "
            + "{@code mapper} to it",
        "   * and using the result.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code mapper} is null or returns null",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public Person.Builder mapName(UnaryOperator<String> mapper) {",
        "    Objects.requireNonNull(mapper);",
        "    return setName(mapper.apply(getName()));",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#getName()}.",
        "   *",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public String getName() {",
        "    if (_unsetProperties.contains(Person_Builder.Property.NAME)) {",
        "      throw new IllegalStateException(\"name not set\");",
        "    }",
        "    return name;",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder setAge(int age) {",
        "    this.age = age;",
        "    _unsetProperties.remove(Person_Builder.Property.AGE);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Replaces the value to be returned by {@link Person#getAge()} by applying "
            + "{@code mapper} to it",
        "   * and using the result.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code mapper} is null",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public Person.Builder mapAge(IntUnaryOperator mapper) {",
        "    Objects.requireNonNull(mapper);",
        "    return setAge(mapper.applyAsInt(getAge()));",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#getAge()}.",
        "   *",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public int getAge() {",
        "    if (_unsetProperties.contains(Person_Builder.Property.AGE)) {",
        "      throw new IllegalStateException(\"age not set\");",
        "    }",
        "    return age;",
        "  }",
        "",
        "  /** Sets all property values using the given {@code Person} as a template. */",
        "  public Person.Builder mergeFrom(Person value) {",
        "    Person_Builder _defaults = new Person.Builder();",
        "    if (_defaults._unsetProperties.contains(Person_Builder.Property.NAME)",
        "        || !Objects.equals(value.getName(), _defaults.getName())) {",
        "      setName(value.getName());",
        "    }",
        "    if (_defaults._unsetProperties.contains(Person_Builder.Property.AGE)",
        "        || value.getAge() != _defaults.getAge()) {",
        "      setAge(value.getAge());",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Copies values from the given {@code Builder}. "
            + "Does not affect any properties not set on the",
        "   * input.",
        "   */",
        "  public Person.Builder mergeFrom(Person.Builder template) {",
        "    // Upcast to access private fields; otherwise, oddly, we get an access violation.",
        "    Person_Builder base = template;",
        "    Person_Builder _defaults = new Person.Builder();",
        "    if (!base._unsetProperties.contains(Person_Builder.Property.NAME)",
        "        && (_defaults._unsetProperties.contains(Person_Builder.Property.NAME)",
        "            || !Objects.equals(template.getName(), _defaults.getName()))) {",
        "      setName(template.getName());",
        "    }",
        "    if (!base._unsetProperties.contains(Person_Builder.Property.AGE)",
        "        && (_defaults._unsetProperties.contains(Person_Builder.Property.AGE)",
        "            || template.getAge() != _defaults.getAge())) {",
        "      setAge(template.getAge());",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /** Resets the state of this builder. */",
        "  public Person.Builder clear() {",
        "    Person_Builder _defaults = new Person.Builder();",
        "    name = _defaults.name;",
        "    age = _defaults.age;",
        "    _unsetProperties.clear();",
        "    _unsetProperties.addAll(_defaults._unsetProperties);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created {@link Person} based on the contents of the {@code Builder}.",
        "   *",
        "   * @throws IllegalStateException if any field has not been set",
        "   */",
        "  public Person build() {",
        "    if (!_unsetProperties.isEmpty()) {",
        "      throw new IllegalStateException(\"Not set: \" + _unsetProperties);",
        "    }",
        "    return new Person_Builder.Value(this);",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created partial {@link Person} for use in unit tests. "
            + "State checking will not",
        "   * be performed. Unset properties will throw an {@link UnsupportedOperationException} "
            + "when",
        "   * accessed via the partial object.",
        "   *",
        "   * <p>Partials should only ever be used in tests. "
            + "They permit writing robust test cases that won't",
        "   * fail if this type gains more application-level constraints "
            + "(e.g. new required fields) in",
        "   * future. If you require partially complete values in production code, "
            + "consider using a Builder.",
        "   */",
        "  public Person buildPartial() {",
        "    return new Person_Builder.Partial(this);",
        "  }",
        "",
        "  private static final class Value extends Person {",
        "    private final String name;",
        "    private final int age;",
        "",
        "    private Value(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "    }",
        "",
        "    @Override",
        "    public String getName() {",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public int getAge() {",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Value)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Value other = (Person_Builder.Value) obj;",
        "      return Objects.equals(name, other.name) && age == other.age;",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      return Objects.hash(name, age);",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"Person{name=\" + name + \", age=\" + age + \"}\";",
        "    }",
        "  }",
        "",
        "  private static final class Partial extends Person {",
        "    private final String name;",
        "    private final int age;",
        "    private final EnumSet<Person_Builder.Property> _unsetProperties;",
        "",
        "    Partial(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "      this._unsetProperties = builder._unsetProperties.clone();",
        "    }",
        "",
        "    @Override",
        "    public String getName() {",
        "      if (_unsetProperties.contains(Person_Builder.Property.NAME)) {",
        "        throw new UnsupportedOperationException(\"name not set\");",
        "      }",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public int getAge() {",
        "      if (_unsetProperties.contains(Person_Builder.Property.AGE)) {",
        "        throw new UnsupportedOperationException(\"age not set\");",
        "      }",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Partial)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Partial other = (Person_Builder.Partial) obj;",
        "      return Objects.equals(name, other.name)",
        "          && age == other.age",
        "          && Objects.equals(_unsetProperties, other._unsetProperties);",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      return Objects.hash(name, age, _unsetProperties);",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      StringBuilder result = new StringBuilder(\"partial Person{\");",
        "      String separator = \"\";",
        "      if (!_unsetProperties.contains(Person_Builder.Property.NAME)) {",
        "        result.append(\"name=\").append(name);",
        "        separator = \", \";",
        "      }",
        "      if (!_unsetProperties.contains(Person_Builder.Property.AGE)) {",
        "        result.append(separator).append(\"age=\").append(age);",
        "      }",
        "      return result.append(\"}\").toString();",
        "    }",
        "  }",
        "}\n").collect(joining("\n")));
  }

  @Test
  public void testJ8_guava() {
    Metadata metadata = createMetadata(true);

    String source = generateSource(metadata, GuavaLibrary.AVAILABLE);
    assertThat(source).isEqualTo(Stream.of(
        "/** Auto-generated superclass of {@link Person.Builder}, "
            + "derived from the API of {@link Person}. */",
        "abstract class Person_Builder {",
        "",
        "  /** Creates a new builder using {@code value} as a template. */",
        "  public static Person.Builder from(Person value) {",
        "    return new Person.Builder().mergeFrom(value);",
        "  }",
        "",
        "  private enum Property {",
        "    NAME(\"name\"),",
        "    AGE(\"age\"),",
        "    ;",
        "",
        "    private final String name;",
        "",
        "    private Property(String name) {",
        "      this.name = name;",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return name;",
        "    }",
        "  }",
        "",
        "  private String name;",
        "  private int age;",
        "  private final EnumSet<Person_Builder.Property> _unsetProperties =",
        "      EnumSet.allOf(Person_Builder.Property.class);",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getName()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code name} is null",
        "   */",
        "  public Person.Builder setName(String name) {",
        "    this.name = Objects.requireNonNull(name);",
        "    _unsetProperties.remove(Person_Builder.Property.NAME);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Replaces the value to be returned by {@link Person#getName()} "
            + "by applying {@code mapper} to it",
        "   * and using the result.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code mapper} is null or returns null",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public Person.Builder mapName(UnaryOperator<String> mapper) {",
        "    Objects.requireNonNull(mapper);",
        "    return setName(mapper.apply(getName()));",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#getName()}.",
        "   *",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public String getName() {",
        "    Preconditions.checkState(",
        "        !_unsetProperties.contains(Person_Builder.Property.NAME), \"name not set\");",
        "    return name;",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#getAge()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder setAge(int age) {",
        "    this.age = age;",
        "    _unsetProperties.remove(Person_Builder.Property.AGE);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Replaces the value to be returned by {@link Person#getAge()} "
            + "by applying {@code mapper} to it",
        "   * and using the result.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code mapper} is null",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public Person.Builder mapAge(IntUnaryOperator mapper) {",
        "    Objects.requireNonNull(mapper);",
        "    return setAge(mapper.applyAsInt(getAge()));",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#getAge()}.",
        "   *",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public int getAge() {",
        "    Preconditions.checkState(",
        "        !_unsetProperties.contains(Person_Builder.Property.AGE), \"age not set\");",
        "    return age;",
        "  }",
        "",
        "  /** Sets all property values using the given {@code Person} as a template. */",
        "  public Person.Builder mergeFrom(Person value) {",
        "    Person_Builder _defaults = new Person.Builder();",
        "    if (_defaults._unsetProperties.contains(Person_Builder.Property.NAME)",
        "        || !Objects.equals(value.getName(), _defaults.getName())) {",
        "      setName(value.getName());",
        "    }",
        "    if (_defaults._unsetProperties.contains(Person_Builder.Property.AGE)",
        "        || value.getAge() != _defaults.getAge()) {",
        "      setAge(value.getAge());",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Copies values from the given {@code Builder}. "
            + "Does not affect any properties not set on the",
        "   * input.",
        "   */",
        "  public Person.Builder mergeFrom(Person.Builder template) {",
        "    // Upcast to access private fields; otherwise, oddly, we get an access violation.",
        "    Person_Builder base = template;",
        "    Person_Builder _defaults = new Person.Builder();",
        "    if (!base._unsetProperties.contains(Person_Builder.Property.NAME)",
        "        && (_defaults._unsetProperties.contains(Person_Builder.Property.NAME)",
        "            || !Objects.equals(template.getName(), _defaults.getName()))) {",
        "      setName(template.getName());",
        "    }",
        "    if (!base._unsetProperties.contains(Person_Builder.Property.AGE)",
        "        && (_defaults._unsetProperties.contains(Person_Builder.Property.AGE)",
        "            || template.getAge() != _defaults.getAge())) {",
        "      setAge(template.getAge());",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /** Resets the state of this builder. */",
        "  public Person.Builder clear() {",
        "    Person_Builder _defaults = new Person.Builder();",
        "    name = _defaults.name;",
        "    age = _defaults.age;",
        "    _unsetProperties.clear();",
        "    _unsetProperties.addAll(_defaults._unsetProperties);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created {@link Person} based on the contents of the {@code Builder}.",
        "   *",
        "   * @throws IllegalStateException if any field has not been set",
        "   */",
        "  public Person build() {",
        "    Preconditions.checkState(_unsetProperties.isEmpty(),"
            + " \"Not set: %s\", _unsetProperties);",
        "    return new Person_Builder.Value(this);",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created partial {@link Person} for use in unit tests. "
            + "State checking will not",
        "   * be performed. Unset properties will throw an {@link UnsupportedOperationException} "
            + "when",
        "   * accessed via the partial object.",
        "   *",
        "   * <p>Partials should only ever be used in tests. "
            + "They permit writing robust test cases that won't",
        "   * fail if this type gains more application-level constraints "
            + "(e.g. new required fields) in",
        "   * future. If you require partially complete values in production code, "
            + "consider using a Builder.",
        "   */",
        "  @VisibleForTesting()",
        "  public Person buildPartial() {",
        "    return new Person_Builder.Partial(this);",
        "  }",
        "",
        "  private static final class Value extends Person {",
        "    private final String name;",
        "    private final int age;",
        "",
        "    private Value(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "    }",
        "",
        "    @Override",
        "    public String getName() {",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public int getAge() {",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Value)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Value other = (Person_Builder.Value) obj;",
        "      return Objects.equals(name, other.name) && age == other.age;",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      return Objects.hash(name, age);",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"Person{name=\" + name + \", age=\" + age + \"}\";",
        "    }",
        "  }",
        "",
        "  private static final class Partial extends Person {",
        "    private final String name;",
        "    private final int age;",
        "    private final EnumSet<Person_Builder.Property> _unsetProperties;",
        "",
        "    Partial(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "      this._unsetProperties = builder._unsetProperties.clone();",
        "    }",
        "",
        "    @Override",
        "    public String getName() {",
        "      if (_unsetProperties.contains(Person_Builder.Property.NAME)) {",
        "        throw new UnsupportedOperationException(\"name not set\");",
        "      }",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public int getAge() {",
        "      if (_unsetProperties.contains(Person_Builder.Property.AGE)) {",
        "        throw new UnsupportedOperationException(\"age not set\");",
        "      }",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Partial)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Partial other = (Person_Builder.Partial) obj;",
        "      return Objects.equals(name, other.name)",
        "          && age == other.age",
        "          && Objects.equals(_unsetProperties, other._unsetProperties);",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      return Objects.hash(name, age, _unsetProperties);",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      StringBuilder result = new StringBuilder(\"partial Person{\");",
        "      String separator = \"\";",
        "      if (!_unsetProperties.contains(Person_Builder.Property.NAME)) {",
        "        result.append(\"name=\").append(name);",
        "        separator = \", \";",
        "      }",
        "      if (!_unsetProperties.contains(Person_Builder.Property.AGE)) {",
        "        result.append(separator).append(\"age=\").append(age);",
        "      }",
        "      return result.append(\"}\").toString();",
        "    }",
        "  }",
        "}\n").collect(joining("\n")));
  }

  @Test
  public void testPrefixless() {
    Metadata metadata = createMetadata(false);

    assertThat(generateSource(metadata, GuavaLibrary.AVAILABLE)).isEqualTo(Stream.of(
        "/** Auto-generated superclass of {@link Person.Builder}, "
            + "derived from the API of {@link Person}. */",
        "abstract class Person_Builder {",
        "",
        "  /** Creates a new builder using {@code value} as a template. */",
        "  public static Person.Builder from(Person value) {",
        "    return new Person.Builder().mergeFrom(value);",
        "  }",
        "",
        "  private enum Property {",
        "    NAME(\"name\"),",
        "    AGE(\"age\"),",
        "    ;",
        "",
        "    private final String name;",
        "",
        "    private Property(String name) {",
        "      this.name = name;",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return name;",
        "    }",
        "  }",
        "",
        "  private String name;",
        "  private int age;",
        "  private final EnumSet<Person_Builder.Property> _unsetProperties =",
        "      EnumSet.allOf(Person_Builder.Property.class);",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#name()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code name} is null",
        "   */",
        "  public Person.Builder name(String name) {",
        "    this.name = Objects.requireNonNull(name);",
        "    _unsetProperties.remove(Person_Builder.Property.NAME);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Replaces the value to be returned by {@link Person#name()} by applying "
            + "{@code mapper} to it and",
        "   * using the result.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code mapper} is null or returns null",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public Person.Builder mapName(UnaryOperator<String> mapper) {",
        "    Objects.requireNonNull(mapper);",
        "    return name(mapper.apply(name()));",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#name()}.",
        "   *",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public String name() {",
        "    Preconditions.checkState(",
        "        !_unsetProperties.contains(Person_Builder.Property.NAME), \"name not set\");",
        "    return name;",
        "  }",
        "",
        "  /**",
        "   * Sets the value to be returned by {@link Person#age()}.",
        "   *",
        "   * @return this {@code Builder} object",
        "   */",
        "  public Person.Builder age(int age) {",
        "    this.age = age;",
        "    _unsetProperties.remove(Person_Builder.Property.AGE);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Replaces the value to be returned by {@link Person#age()} by applying {@code mapper} "
            + "to it and",
        "   * using the result.",
        "   *",
        "   * @return this {@code Builder} object",
        "   * @throws NullPointerException if {@code mapper} is null",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public Person.Builder mapAge(IntUnaryOperator mapper) {",
        "    Objects.requireNonNull(mapper);",
        "    return age(mapper.applyAsInt(age()));",
        "  }",
        "",
        "  /**",
        "   * Returns the value that will be returned by {@link Person#age()}.",
        "   *",
        "   * @throws IllegalStateException if the field has not been set",
        "   */",
        "  public int age() {",
        "    Preconditions.checkState(",
        "        !_unsetProperties.contains(Person_Builder.Property.AGE), \"age not set\");",
        "    return age;",
        "  }",
        "",
        "  /** Sets all property values using the given {@code Person} as a template. */",
        "  public Person.Builder mergeFrom(Person value) {",
        "    Person_Builder _defaults = new Person.Builder();",
        "    if (_defaults._unsetProperties.contains(Person_Builder.Property.NAME)",
        "        || !Objects.equals(value.name(), _defaults.name())) {",
        "      name(value.name());",
        "    }",
        "    if (_defaults._unsetProperties.contains(Person_Builder.Property.AGE)",
        "        || value.age() != _defaults.age()) {",
        "      age(value.age());",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Copies values from the given {@code Builder}. "
            + "Does not affect any properties not set on the",
        "   * input.",
        "   */",
        "  public Person.Builder mergeFrom(Person.Builder template) {",
        "    // Upcast to access private fields; otherwise, oddly, we get an access violation.",
        "    Person_Builder base = template;",
        "    Person_Builder _defaults = new Person.Builder();",
        "    if (!base._unsetProperties.contains(Person_Builder.Property.NAME)",
        "        && (_defaults._unsetProperties.contains(Person_Builder.Property.NAME)",
        "            || !Objects.equals(template.name(), _defaults.name()))) {",
        "      name(template.name());",
        "    }",
        "    if (!base._unsetProperties.contains(Person_Builder.Property.AGE)",
        "        && (_defaults._unsetProperties.contains(Person_Builder.Property.AGE)",
        "            || template.age() != _defaults.age())) {",
        "      age(template.age());",
        "    }",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /** Resets the state of this builder. */",
        "  public Person.Builder clear() {",
        "    Person_Builder _defaults = new Person.Builder();",
        "    name = _defaults.name;",
        "    age = _defaults.age;",
        "    _unsetProperties.clear();",
        "    _unsetProperties.addAll(_defaults._unsetProperties);",
        "    return (Person.Builder) this;",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created {@link Person} based on the contents of the {@code Builder}.",
        "   *",
        "   * @throws IllegalStateException if any field has not been set",
        "   */",
        "  public Person build() {",
        "    Preconditions.checkState(_unsetProperties.isEmpty(),"
            + " \"Not set: %s\", _unsetProperties);",
        "    return new Person_Builder.Value(this);",
        "  }",
        "",
        "  /**",
        "   * Returns a newly-created partial {@link Person} for use in unit tests. "
            + "State checking will not",
        "   * be performed. Unset properties will throw an {@link UnsupportedOperationException} "
            + "when",
        "   * accessed via the partial object.",
        "   *",
        "   * <p>Partials should only ever be used in tests. "
            + "They permit writing robust test cases that won't",
        "   * fail if this type gains more application-level constraints "
            + "(e.g. new required fields) in",
        "   * future. If you require partially complete values in production code, "
            + "consider using a Builder.",
        "   */",
        "  @VisibleForTesting()",
        "  public Person buildPartial() {",
        "    return new Person_Builder.Partial(this);",
        "  }",
        "",
        "  private static final class Value extends Person {",
        "    private final String name;",
        "    private final int age;",
        "",
        "    private Value(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "    }",
        "",
        "    @Override",
        "    public String name() {",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public int age() {",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Value)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Value other = (Person_Builder.Value) obj;",
        "      return Objects.equals(name, other.name) && age == other.age;",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      return Objects.hash(name, age);",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      return \"Person{name=\" + name + \", age=\" + age + \"}\";",
        "    }",
        "  }",
        "",
        "  private static final class Partial extends Person {",
        "    private final String name;",
        "    private final int age;",
        "    private final EnumSet<Person_Builder.Property> _unsetProperties;",
        "",
        "    Partial(Person_Builder builder) {",
        "      this.name = builder.name;",
        "      this.age = builder.age;",
        "      this._unsetProperties = builder._unsetProperties.clone();",
        "    }",
        "",
        "    @Override",
        "    public String name() {",
        "      if (_unsetProperties.contains(Person_Builder.Property.NAME)) {",
        "        throw new UnsupportedOperationException(\"name not set\");",
        "      }",
        "      return name;",
        "    }",
        "",
        "    @Override",
        "    public int age() {",
        "      if (_unsetProperties.contains(Person_Builder.Property.AGE)) {",
        "        throw new UnsupportedOperationException(\"age not set\");",
        "      }",
        "      return age;",
        "    }",
        "",
        "    @Override",
        "    public boolean equals(Object obj) {",
        "      if (!(obj instanceof Person_Builder.Partial)) {",
        "        return false;",
        "      }",
        "      Person_Builder.Partial other = (Person_Builder.Partial) obj;",
        "      return Objects.equals(name, other.name)",
        "          && age == other.age",
        "          && Objects.equals(_unsetProperties, other._unsetProperties);",
        "    }",
        "",
        "    @Override",
        "    public int hashCode() {",
        "      return Objects.hash(name, age, _unsetProperties);",
        "    }",
        "",
        "    @Override",
        "    public String toString() {",
        "      StringBuilder result = new StringBuilder(\"partial Person{\");",
        "      String separator = \"\";",
        "      if (!_unsetProperties.contains(Person_Builder.Property.NAME)) {",
        "        result.append(\"name=\").append(name);",
        "        separator = \", \";",
        "      }",
        "      if (!_unsetProperties.contains(Person_Builder.Property.AGE)) {",
        "        result.append(separator).append(\"age=\").append(age);",
        "      }",
        "      return result.append(\"}\").toString();",
        "    }",
        "  }",
        "}\n").collect(joining("\n")));
  }

  private static String generateSource(Metadata metadata, Feature<?>... features) {
    SourceBuilder sourceBuilder = SourceStringBuilder.simple(features);
    new CodeGenerator().writeBuilderSource(sourceBuilder, metadata);
    try {
      return new Formatter().formatSource(sourceBuilder.toString());
    } catch (FormatterException e) {
      throw new RuntimeException(e);
    }
  }

  private static Metadata createMetadata(boolean bean) {
    QualifiedName person = QualifiedName.of("com.example", "Person");
    QualifiedName generatedBuilder = QualifiedName.of("com.example", "Person_Builder");
    Property name = new Property.Builder()
        .setAllCapsName("NAME")
        .setBoxedType(STRING)
        .setCapitalizedName("Name")
        .setFullyCheckedCast(true)
        .setGetterName(bean ? "getName" : "name")
        .setName("name")
        .setType(STRING)
        .setUsingBeanConvention(bean)
        .build();
    Property age = new Property.Builder()
        .setAllCapsName("AGE")
        .setBoxedType(INTEGER)
        .setCapitalizedName("Age")
        .setFullyCheckedCast(true)
        .setGetterName(bean ? "getAge" : "age")
        .setName("age")
        .setType(INT)
        .setUsingBeanConvention(bean)
        .build();
    Metadata metadata = new Metadata.Builder()
        .setBuilder(person.nestedType("Builder").withParameters())
        .setExtensible(true)
        .setBuilderFactory(BuilderFactory.NO_ARGS_CONSTRUCTOR)
        .setBuilderSerializable(false)
        .setGeneratedBuilder(generatedBuilder.withParameters())
        .setInterfaceType(false)
        .setPartialType(generatedBuilder.nestedType("Partial").withParameters())
        .addProperties(name, age)
        .setPropertyEnum(generatedBuilder.nestedType("Property").withParameters())
        .setType(person.withParameters())
        .setValueType(generatedBuilder.nestedType("Value").withParameters())
        .build();
    return metadata.toBuilder()
        .clearProperties()
        .addProperties(name.toBuilder()
            .setCodeGenerator(new DefaultProperty(metadata, name, false, unaryOperator(STRING)))
            .build())
        .addProperties(age.toBuilder()
            .setCodeGenerator(new DefaultProperty(metadata, age, false, intUnaryOperator(INT)))
            .build())
        .build();
  }

}
