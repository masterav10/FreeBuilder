package org.inferred.freebuilder.processor.excerpt;

import static org.inferred.freebuilder.processor.util.feature.FunctionPackage.FUNCTION_PACKAGE;

import org.inferred.freebuilder.processor.util.Excerpt;
import org.inferred.freebuilder.processor.util.ParameterizedType;
import org.inferred.freebuilder.processor.util.SourceBuilder;
import org.inferred.freebuilder.processor.util.LazyName;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Excerpts defining a set implementation that delegates to a provided add method to perform
 * element validation and insertion into a backing set.
 */
public class CheckedSet extends Excerpt {

  public static final LazyName TYPE = new LazyName("CheckedSet", new CheckedSet());

  private CheckedSet() {}

  @Override
  public void addTo(SourceBuilder code) {
    ParameterizedType consumer = code.feature(FUNCTION_PACKAGE).consumer().get();
    code.addLine("")
        .addLine("/**")
        .addLine(" * A set implementation that delegates to a provided add method")
        .addLine(" * to perform element validation and insertion into a backing set.")
        .addLine(" */")
        .addLine("private static class %s<E> extends %s<E> {", TYPE, AbstractSet.class)
        .addLine("")
        .addLine("  private final %s<E> set;", Set.class)
        .addLine("  private final %s<E> add;", consumer.getQualifiedName())
        .addLine("")
        .addLine("  %s(%s<E> set, %s<E> add) {", TYPE, Set.class, consumer.getQualifiedName())
        .addLine("    this.set = set;")
        .addLine("    this.add = add;")
        .addLine("  }")
        .addLine("")
        .addLine("")
        .addLine("  @Override public %s<E> iterator() {", Iterator.class)
        .addLine("    return set.iterator();")
        .addLine("  }")
        .addLine("")
        .addLine("  @Override public int size() {")
        .addLine("    return set.size();")
        .addLine("  }")
        .addLine("")
        .addLine("  @Override public boolean contains(Object e) {")
        .addLine("    return set.contains(e);")
        .addLine("  }")
        .addLine("")
        .addLine("  @Override public boolean add(E e) {")
        .addLine("    if (!set.contains(e)) {")
        .addLine("      add.accept(e);")
        .addLine("      return true;")
        .addLine("    } else {")
        .addLine("      return false;")
        .addLine("    }")
        .addLine("  }")
        .addLine("")
        .addLine("  @Override public boolean remove(Object e) {")
        .addLine("    return set.remove(e);")
        .addLine("  }")
        .addLine("}");
  }

  @Override
  protected void addFields(FieldReceiver fields) {}
}
