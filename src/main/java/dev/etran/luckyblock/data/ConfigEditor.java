package dev.etran.luckyblock.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Changes single values in config.yml's text without touching anything else, so the comments that explain
 * the file survive (Bukkit's own save drops them on most versions). Works on the lines of the file.
 */
public final class ConfigEditor {
  private ConfigEditor() {}

  private static final Pattern KEY = Pattern.compile("^(\\s*)([A-Za-z0-9_.-]+):(\\s*)(.*)$");

  /** Index of the line holding the key at the dotted path, or -1. */
  private static int find(List<String> lines, String path) {
    List<Integer> indents = new ArrayList<>();
    List<String> keys = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      String trimmed = lines.get(i).trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-"))
        continue;
      Matcher m = KEY.matcher(lines.get(i));
      if (!m.matches())
        continue;
      int indent = m.group(1).length();
      while (!indents.isEmpty() && indents.get(indents.size() - 1) >= indent) {
        indents.remove(indents.size() - 1);
        keys.remove(keys.size() - 1);
      }
      indents.add(indent);
      keys.add(m.group(2));
      if (String.join(".", keys).equals(path))
        return i;
    }
    return -1;
  }

  /** Index where a trailing "# comment" starts in the text after the colon, or -1. */
  private static int commentStart(String rest) {
    boolean single = false;
    boolean dbl = false;
    for (int i = 0; i < rest.length(); i++) {
      char c = rest.charAt(i);
      if (c == '\'' && !dbl) {
        single = !single;
      } else if (c == '"' && !single) {
        dbl = !dbl;
      } else if (c == '#' && !single && !dbl && (i == 0 || rest.charAt(i - 1) == ' ')) {
        return i;
      }
    }
    return -1;
  }

  /** Text after the colon without its comment, trimmed. */
  private static String valueOf(String rest) {
    int comment = commentStart(rest);
    return (comment < 0 ? rest : rest.substring(0, comment)).trim();
  }

  /** The part after the colon with the value swapped for a new one; the comment and spacing stay. */
  private static String withValue(Matcher m, String newValue) {
    String rest = m.group(4);
    int comment = commentStart(rest);
    String tail = "";
    if (comment >= 0) {
      String beforeComment = rest.substring(0, comment);
      String spaces = beforeComment.substring(beforeComment.replaceAll("\\s+$", "").length());
      tail = (spaces.isEmpty() ? " " : spaces) + rest.substring(comment);
    }
    String gap = m.group(3).isEmpty() ? " " : m.group(3);
    return m.group(1) + m.group(2) + ":" + gap + newValue + tail;
  }

  /** Sets a plain value (number, true/false). Returns false if the key is not there or is not a plain value. */
  public static boolean setScalar(List<String> lines, String path, String value) {
    int at = find(lines, path);
    if (at < 0)
      return false;
    Matcher m = KEY.matcher(lines.get(at));
    if (!m.matches())
      return false;
    String current = valueOf(m.group(4));
    if (current.isEmpty() || current.startsWith("{") || current.startsWith("["))
      return false;
    lines.set(at, withValue(m, value));
    return true;
  }

  /** Sets one entry of a one-line map such as "wool: { team: 30, diamond: 10 }". */
  public static boolean setFlowEntry(List<String> lines, String mapPath, String entry, String value) {
    int at = find(lines, mapPath);
    if (at < 0)
      return false;
    Matcher m = KEY.matcher(lines.get(at));
    if (!m.matches())
      return false;
    String current = valueOf(m.group(4));
    if (!current.startsWith("{") || !current.endsWith("}"))
      return false;
    String inside = current.substring(1, current.length() - 1);
    Matcher e = Pattern.compile("(^|[\\s,])" + Pattern.quote(entry) + "\\s*:\\s*-?\\d+").matcher(inside);
    String replacement;
    if (e.find()) {
      replacement = inside.substring(0, e.start()) + e.group(1) + entry + ": " + value + inside.substring(e.end());
    } else {
      replacement = inside.trim() + ", " + entry + ": " + value + " ";
      replacement = " " + replacement.trim() + " ";
    }
    lines.set(at, withValue(m, "{" + replacement + "}"));
    return true;
  }

  /** Adds "child: value" as the last entry under an existing block-style map. Returns false if the parent is not there. */
  public static boolean insertChild(List<String> lines, String parentPath, String child, String value) {
    int at = find(lines, parentPath);
    if (at < 0)
      return false;
    Matcher m = KEY.matcher(lines.get(at));
    if (!m.matches() || !valueOf(m.group(4)).isEmpty())
      return false;
    int childIndent = m.group(1).length() + 2;
    int last = at;
    for (int k = at + 1; k < lines.size(); k++) {
      String line = lines.get(k);
      String t = line.trim();
      if (t.isEmpty() || t.startsWith("#"))
        continue;
      int indent = line.length() - line.replaceAll("^\\s+", "").length();
      if (indent <= m.group(1).length())
        break;
      childIndent = indent;
      last = k;
    }
    StringBuilder pad = new StringBuilder();
    for (int n = 0; n < childIndent; n++)
      pad.append(' ');
    lines.add(last + 1, pad + child + ": " + value);
    return true;
  }

  /** Replaces a list (block "- a" lines or "[a, b]") with the given items. Returns false if the key is not there. */
  public static boolean setList(List<String> lines, String path, List<String> items) {
    int at = find(lines, path);
    if (at < 0)
      return false;
    Matcher m = KEY.matcher(lines.get(at));
    if (!m.matches())
      return false;
    String current = valueOf(m.group(4));
    if (current.startsWith("[")) {
      lines.set(at, withValue(m, "[" + String.join(", ", items) + "]"));
      return true;
    }
    if (!current.isEmpty())
      return false;
    int keyIndent = m.group(1).length();
    int itemIndent = keyIndent + 2;
    int end = at + 1;
    int k = at + 1;
    boolean first = true;
    while (k < lines.size()) {
      String t = lines.get(k).trim();
      if (t.startsWith("- ") || t.equals("-")) {
        if (first) {
          itemIndent = lines.get(k).length() - lines.get(k).replaceAll("^\\s+", "").length();
          first = false;
        }
        end = k + 1;
        k++;
      } else if (t.isEmpty() || t.startsWith("#")) {
        k++;
      } else {
        break;
      }
    }
    for (int i = end - 1; i > at; i--)
      lines.remove(i);
    StringBuilder pad = new StringBuilder();
    for (int i = 0; i < itemIndent; i++)
      pad.append(' ');
    int insertAt = at + 1;
    for (String item : items)
      lines.add(insertAt++, pad + "- " + item);
    return true;
  }
}
