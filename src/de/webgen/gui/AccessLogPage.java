/* Copyright (C) 2020 Christoph Theis */

package de.webgen.gui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders access.log (Apache "combined" format) as a human-readable HTML page:
 * summary, top pages, visitors, status codes and the latest requests,
 * or all requests from access.log and access.log.* within a time range.
 */
class AccessLogPage {

    private static final int MAX_RECENT = 200;   // rows in "Latest requests"
    private static final int MAX_ALL    = 5000;  // rows in "All requests", protects the browser
    private static final int MAX_TOP    = 15;    // rows in "Top pages" / "Visitors"

    // host ident user [time] "request" status bytes "referer" "agent"
    private static final Pattern LINE = Pattern.compile(
            "^(\\S+) \\S+ (\\S+) \\[([^\\]]+)\\] \"((?:[^\"\\\\]|\\\\.)*)\" (\\S+) (\\S+)"
          + "(?: \"((?:[^\"\\\\]|\\\\.)*)\" \"((?:[^\"\\\\]|\\\\.)*)\")?");

    private static final DateTimeFormatter LOG_TIME  =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
    private static final DateTimeFormatter SHOW_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private static class Entry {
        String  ip, user, time, method, path, protocol, referer, agent;
        Instant instant; // null if the time can't be read
        int     status;  // -1 if unknown
        long    bytes;
    }

    private static class Counter {
        int    hits;
        long   bytes;
        String last;     // time of the latest request (entries are chronological)
        String agent;    // latest user agent (visitors only)
    }

    /**
     * Options from the query string:
     *   refresh=N           auto-refresh in seconds, 0 = off
     *   show=all            all files; default: only the generated pages directly under "/"
     *   view=all            all requests from access.log and access.log.*; default: the latest from access.log
     *   from=..., to=...    time range for view=all (yyyy-MM-ddTHH:mm[:ss], as sent by datetime-local), each may be empty
     */
    private static class Options {
        int     refresh;
        boolean showAll;
        boolean allView;
        String  from = "", to = "";      // as entered, for the form
        Instant fromInstant, toInstant;  // null: open end
        boolean badRange;                // a value could not be read

        static Options parse(String query) {
            Options o = new Options();
            try {
                o.refresh = Math.max(0, Math.min(3600, Integer.parseInt(param(query, "refresh", "0"))));
            } catch (NumberFormatException e) {
                o.refresh = 0;
            }
            o.showAll = param(query, "show", "").equals("all");
            o.allView = param(query, "view", "").equals("all");
            if (o.allView) {
                o.from = param(query, "from", "").trim();
                o.to   = param(query, "to", "").trim();
                o.fromInstant = toInstant(o, o.from, false);
                o.toInstant   = toInstant(o, o.to, true);
            }
            return o;
        }

        // datetime-local value in the local time zone; "to" without seconds includes the whole minute
        private static Instant toInstant(Options o, String value, boolean endOfMinute) {
            if (value.isEmpty())
                return null;
            try {
                LocalDateTime t = LocalDateTime.parse(value);
                if (endOfMinute && value.length() == 16)
                    t = t.plusSeconds(59);
                return t.atZone(ZoneId.systemDefault()).toInstant();
            } catch (DateTimeParseException e) {
                o.badRange = true;
                return null;
            }
        }

        boolean inRange(Entry e) {
            if (fromInstant == null && toInstant == null)
                return true;
            if (e.instant == null)
                return false;
            return (fromInstant == null || !e.instant.isBefore(fromInstant))
                && (toInstant == null || !e.instant.isAfter(toInstant));
        }

        // Link with some options changed, the others kept
        String link(int refresh, boolean showAll, boolean allView) {
            StringBuilder sb = new StringBuilder("?refresh=").append(refresh);
            if (showAll)
                sb.append("&amp;show=all");
            if (allView) {
                sb.append("&amp;view=all");
                if (!from.isEmpty())
                    sb.append("&amp;from=").append(URLEncoder.encode(from, StandardCharsets.UTF_8));
                if (!to.isEmpty())
                    sb.append("&amp;to=").append(URLEncoder.encode(to, StandardCharsets.UTF_8));
            }
            return sb.toString();
        }
    }

    /** Renders the page for the query string of the request (may be null). */
    static byte[] render(File logFile, String serverName, String query) throws IOException {
        Options opt = Options.parse(query);

        // "Latest": access.log only. "All": access.log and the rotated access.log.* in the same directory
        List<File> files = new ArrayList<>();
        if (!opt.allView) {
            if (logFile.exists())
                files.add(logFile);
        } else {
            File[] all = logFile.getAbsoluteFile().getParentFile().listFiles(
                    f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).startsWith("access.log"));
            if (all != null)
                files.addAll(List.of(all));
            files.sort(Comparator.comparing(File::getName));
        }

        List<Entry> entries = new ArrayList<>();
        int unparsed = 0;
        int total = 0;          // readable requests in the time range, before the page filter
        long filesSize = 0;

        for (File file : files) {
            filesSize += file.length();
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                if (line.isBlank())
                    continue;
                Entry e = parse(line);
                if (e == null) {
                    unparsed++;
                } else if (isAccessLog(e.path)) {
                    // Requests for the log itself are logged, but not shown here
                } else if (opt.inRange(e)) {
                    total++;
                    if (opt.showAll || isTopLevel(e.path))
                        entries.add(e);
                }
            }
        }

        // Several files: bring them into chronological order (stable, so equal times keep their order)
        if (files.size() > 1)
            entries.sort(Comparator.comparing((Entry e) -> e.instant, Comparator.nullsFirst(Comparator.naturalOrder())));

        // ---- statistics ----
        Map<String, Counter> pages    = new HashMap<>();
        Map<String, Counter> visitors = new HashMap<>();
        Map<Integer, Integer> statuses = new TreeMap<>();
        long totalBytes = 0;
        int  errors = 0;

        for (Entry e : entries) {
            Counter p = pages.computeIfAbsent(e.path, k -> new Counter());
            p.hits++;
            p.bytes += e.bytes;
            p.last = e.time;

            Counter v = visitors.computeIfAbsent(e.ip, k -> new Counter());
            v.hits++;
            v.bytes += e.bytes;
            v.last = e.time;
            v.agent = e.agent;

            statuses.merge(e.status, 1, Integer::sum);
            totalBytes += e.bytes;
            if (e.status >= 400)
                errors++;
        }

        // ---- HTML ----
        StringBuilder h = new StringBuilder(64 * 1024);
        h.append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\">\n");
        if (opt.refresh > 0)
            h.append("<meta http-equiv=\"refresh\" content=\"").append(opt.refresh).append("\">\n");
        h.append("<title>").append(esc(serverName)).append(" \u2013 Access log</title>\n");
        h.append(CSS);
        h.append("</head><body>\n");

        h.append("<h1>Access log \u2013 ").append(esc(serverName)).append("</h1>\n");
        h.append("<p class=\"meta\">");
        if (files.isEmpty()) {
            h.append(esc(logFile.getPath()));
        } else {
            for (int i = 0; i < files.size(); i++)
                h.append(i == 0 ? "" : ", ").append(esc(files.get(i).getName()));
            h.append(" in ").append(esc(logFile.getAbsoluteFile().getParent()));
        }
        h.append(" \u00b7 ").append(formatBytes(filesSize));
        h.append(" \u00b7 generated ").append(ZonedDateTime.now().format(SHOW_TIME));

        h.append("<br>Show: ");
        h.append(opt.showAll ? "<a href=\"" + opt.link(opt.refresh, false, opt.allView) + "\">pages in /</a> <b>all files</b>"
                             : "<b>pages in /</b> <a href=\"" + opt.link(opt.refresh, true, opt.allView) + "\">all files</a>");
        h.append(" \u00b7 Auto-refresh: ");
        for (int r : new int[] {0, 10, 30, 60}) {
            String label = r == 0 ? "off" : r + " s";
            if (r == opt.refresh)
                h.append("<b>").append(label).append("</b> ");
            else
                h.append("<a href=\"").append(opt.link(r, opt.showAll, opt.allView)).append("\">").append(label).append("</a> ");
        }
        h.append("</p>\n");

        // Requests: latest, or all within a time range (both ends optional)
        h.append("<form method=\"get\" class=\"range\">Requests: ");
        h.append(opt.allView ? "<a href=\"" + opt.link(opt.refresh, opt.showAll, false) + "\">latest " + MAX_RECENT + "</a> \u00b7 <b>all</b>"
                             : "<b>latest " + MAX_RECENT + "</b> \u00b7 all");
        h.append(" from <input type=\"datetime-local\" name=\"from\" step=\"1\" value=\"").append(esc(opt.from)).append("\">")
         .append(" to <input type=\"datetime-local\" name=\"to\" step=\"1\" value=\"").append(esc(opt.to)).append("\">")
         .append(" <input type=\"hidden\" name=\"view\" value=\"all\">");
        if (opt.showAll)
            h.append("<input type=\"hidden\" name=\"show\" value=\"all\">");
        h.append("<input type=\"hidden\" name=\"refresh\" value=\"").append(opt.refresh).append("\">")
         .append(" <button type=\"submit\">Show</button>");
        if (opt.allView && (!opt.from.isEmpty() || !opt.to.isEmpty()))
            h.append(" <a href=\"").append(opt.link(opt.refresh, opt.showAll, false)).append("&amp;view=all").append("\">clear range</a>");
        if (opt.badRange)
            h.append(" <span class=\"bad\">Invalid date, ignored</span>");
        h.append("</form>\n");

        if (entries.isEmpty()) {
            h.append("<p class=\"empty\">")
             .append(files.isEmpty()   ? "No access log yet."
                     : total == 0 && opt.allView ? "No requests in this time range."
                     : total == 0      ? "No requests logged yet."
                     : "No requests for pages in /. Choose \u201call files\u201d to see all " + total + " requests.")
             .append("</p>\n</body></html>\n");
            return h.toString().getBytes(StandardCharsets.UTF_8);
        }

        // summary tiles
        h.append("<div class=\"tiles\">\n");
        tile(h, "Requests", Integer.toString(entries.size()), "");
        tile(h, "Visitors", Integer.toString(visitors.size()), "distinct IP addresses");
        tile(h, "Errors", Integer.toString(errors), "status 4xx / 5xx");
        tile(h, "Data sent", formatBytes(totalBytes), "");
        h.append("</div>\n");
        h.append("<p class=\"meta\">From ").append(showTime(entries.get(0).time))
         .append(" to ").append(showTime(entries.get(entries.size() - 1).time));
        if (!opt.showAll && total > entries.size())
            h.append(" \u00b7 ").append(total - entries.size()).append(" requests for files in subdirectories hidden");
        if (unparsed > 0)
            h.append(" \u00b7 ").append(unparsed).append(" unreadable line(s) skipped");
        h.append("</p>\n");

        h.append("<div class=\"cols\">\n");

        // top pages
        h.append("<section><h2>Top pages</h2>\n<table><tr><th>Page</th><th class=\"num\">Hits</th><th class=\"num\">Data</th><th>Last access</th></tr>\n");
        for (Map.Entry<String, Counter> e : top(pages)) {
            h.append("<tr><td class=\"path\">").append(esc(e.getKey())).append("</td>")
             .append("<td class=\"num\">").append(e.getValue().hits).append("</td>")
             .append("<td class=\"num\">").append(formatBytes(e.getValue().bytes)).append("</td>")
             .append("<td class=\"ts\">").append(showTime(e.getValue().last)).append("</td></tr>\n");
        }
        h.append("</table>").append(moreNote(pages.size())).append("</section>\n");

        // visitors
        h.append("<section><h2>Visitors</h2>\n<table><tr><th>IP address</th><th class=\"num\">Hits</th><th>Browser</th><th>Last seen</th></tr>\n");
        for (Map.Entry<String, Counter> e : top(visitors)) {
            String agent = e.getValue().agent;
            h.append("<tr><td>").append(esc(e.getKey())).append("</td>")
             .append("<td class=\"num\">").append(e.getValue().hits).append("</td>")
             .append("<td title=\"").append(esc(agent == null ? "" : agent)).append("\">").append(esc(browser(agent))).append("</td>")
             .append("<td class=\"ts\">").append(showTime(e.getValue().last)).append("</td></tr>\n");
        }
        h.append("</table>").append(moreNote(visitors.size())).append("</section>\n");

        // status codes
        h.append("<section><h2>Status codes</h2>\n<table><tr><th>Status</th><th class=\"num\">Count</th></tr>\n");
        for (Map.Entry<Integer, Integer> e : statuses.entrySet()) {
            h.append("<tr><td><span class=\"st ").append(statusClass(e.getKey())).append("\">")
             .append(e.getKey() < 0 ? "\u2013" : e.getKey()).append("</span> ")
             .append(esc(statusText(e.getKey()))).append("</td>")
             .append("<td class=\"num\">").append(e.getValue()).append("</td></tr>\n");
        }
        h.append("</table></section>\n");

        h.append("</div>\n");

        // requests, newest first
        int max   = opt.allView ? MAX_ALL : MAX_RECENT;
        int shown = Math.min(max, entries.size());
        h.append(opt.allView ? "<h2>All requests</h2>\n" : "<h2>Latest requests</h2>\n");
        if (entries.size() > shown)
            h.append("<p class=\"meta\">The latest ").append(shown).append(" of ").append(entries.size()).append(" requests")
             .append(opt.allView ? " \u2013 narrow the time range to see older ones.</p>\n" : ".</p>\n");
        h.append("<table class=\"recent\"><tr><th>Time</th><th>IP address</th><th>Request</th><th>Status</th><th class=\"num\">Data</th><th>Browser</th></tr>\n");
        for (int i = entries.size() - 1; i >= entries.size() - shown; i--) {
            Entry e = entries.get(i);
            h.append("<tr><td class=\"ts\">").append(showTime(e.time)).append("</td>")
             .append("<td>").append(esc(e.ip)).append("</td>")
             .append("<td class=\"path\"><span class=\"method\">").append(esc(e.method)).append("</span> ")
             .append(esc(e.path)).append("</td>")
             .append("<td><span class=\"st ").append(statusClass(e.status)).append("\" title=\"")
             .append(esc(statusText(e.status))).append("\">").append(e.status < 0 ? "\u2013" : e.status).append("</span></td>")
             .append("<td class=\"num\">").append(e.bytes > 0 ? formatBytes(e.bytes) : "\u2013").append("</td>")
             .append("<td title=\"").append(esc(e.agent == null ? "" : e.agent)).append("\">").append(esc(browser(e.agent))).append("</td></tr>\n");
        }
        h.append("</table>\n</body></html>\n");

        return h.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String param(String query, String name, String def) {
        if (query == null)
            return def;
        for (String p : query.split("&"))
            if (p.startsWith(name + "="))
                return URLDecoder.decode(p.substring(name.length() + 1), StandardCharsets.UTF_8);
        return def;
    }

    private static boolean isAccessLog(String path) {
        return path.toLowerCase(Locale.ROOT).startsWith("/access.log");
    }

    // The generated files live directly in the server directory: "/", "/index.html", "/MS_CP.html" ...
    // Files in subdirectories (js, css, flags, ...) are left out by default
    private static boolean isTopLevel(String path) {
        int q = path.indexOf('?');
        String p = q >= 0 ? path.substring(0, q) : path;
        return p.startsWith("/") && p.indexOf('/', 1) < 0;
    }

    // ---- helpers ----

    private static Entry parse(String line) {
        Matcher m = LINE.matcher(line);
        if (!m.find())
            return null;

        Entry e = new Entry();
        e.ip   = m.group(1);
        e.user = m.group(2);
        e.time = m.group(3);
        try {
            e.instant = ZonedDateTime.parse(e.time, LOG_TIME).toInstant();
        } catch (Exception ex) {
            e.instant = null;
        }

        String[] request = unescape(m.group(4)).split(" ", 3);
        e.method   = request.length > 0 ? request[0] : "";
        e.path     = request.length > 1 ? request[1] : "";
        e.protocol = request.length > 2 ? request[2] : "";

        try {
            e.status = Integer.parseInt(m.group(5));
        } catch (NumberFormatException ex) {
            e.status = -1;
        }
        try {
            e.bytes = Long.parseLong(m.group(6));
        } catch (NumberFormatException ex) {
            e.bytes = 0;
        }

        e.referer = m.group(7) == null || m.group(7).equals("-") ? null : unescape(m.group(7));
        e.agent   = m.group(8) == null || m.group(8).equals("-") ? null : unescape(m.group(8));
        return e;
    }

    private static List<Map.Entry<String, Counter>> top(Map<String, Counter> map) {
        List<Map.Entry<String, Counter>> list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<String, Counter> e) -> e.getValue().hits).reversed()
                            .thenComparing(Map.Entry::getKey));
        return list.subList(0, Math.min(MAX_TOP, list.size()));
    }

    private static String moreNote(int size) {
        return size > MAX_TOP ? "<p class=\"meta\">" + (size - MAX_TOP) + " more not shown</p>" : "";
    }

    private static void tile(StringBuilder h, String label, String value, String note) {
        h.append("<div class=\"tile\"><div class=\"label\">").append(label).append("</div>")
         .append("<div class=\"value\">").append(esc(value)).append("</div>")
         .append("<div class=\"note\">").append(esc(note)).append("</div></div>\n");
    }

    private static String showTime(String logTime) {
        try {
            return ZonedDateTime.parse(logTime, LOG_TIME).format(SHOW_TIME);
        } catch (Exception e) {
            return esc(logTime);
        }
    }

    private static String statusClass(int status) {
        if (status >= 500) return "s5";
        if (status >= 400) return "s4";
        if (status >= 300) return "s3";
        if (status >= 200) return "s2";
        return "s0";
    }

    private static final Map<Integer, String> STATUS_TEXT = new LinkedHashMap<>();
    static {
        STATUS_TEXT.put(200, "OK");
        STATUS_TEXT.put(204, "No Content");
        STATUS_TEXT.put(301, "Moved Permanently");
        STATUS_TEXT.put(302, "Found");
        STATUS_TEXT.put(304, "Not Modified");
        STATUS_TEXT.put(400, "Bad Request");
        STATUS_TEXT.put(403, "Forbidden");
        STATUS_TEXT.put(404, "Not Found");
        STATUS_TEXT.put(405, "Method Not Allowed");
        STATUS_TEXT.put(500, "Internal Server Error");
    }

    private static String statusText(int status) {
        if (status < 0)
            return "no response";
        return STATUS_TEXT.getOrDefault(status, "");
    }

    // Short browser name from the User-Agent; the full string is shown as tooltip
    private static String browser(String agent) {
        if (agent == null || agent.isEmpty())
            return "\u2013";
        if (agent.contains("Edg/"))     return "Edge";
        if (agent.contains("OPR/"))     return "Opera";
        if (agent.contains("Firefox/")) return "Firefox";
        if (agent.contains("Chrome/"))  return agent.contains("Mobile") ? "Chrome (mobile)" : "Chrome";
        if (agent.contains("Safari/"))  return agent.contains("Mobile") ? "Safari (mobile)" : "Safari";
        String first = agent.split("[ /]", 2)[0];
        return first.length() > 30 ? first.substring(0, 30) + "\u2026" : first;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024)
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024));
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 'x' && i + 2 < s.length()) {
                    try {
                        sb.append((char) Integer.parseInt(s.substring(i + 1, i + 3), 16));
                        i += 2;
                        continue;
                    } catch (NumberFormatException e) {
                        // not an escape, keep as is
                    }
                }
                sb.append(n);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null)
            return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '&':  sb.append("&amp;");  break;
                case '"':  sb.append("&quot;"); break;
                default:
                    if (c < 0x20 && c != '\t')
                        sb.append('\u00b7');    // control characters from the log
                    else
                        sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final String CSS =
          """
          <style>
          body  { font-family: system-ui, sans-serif; margin: 1.5em; color: #222; }
          h1    { margin-bottom: 0.2em; }
          h2    { margin: 1.4em 0 0.5em; border-bottom: 1px solid #ccc; font-size: 1.15em; }
          .meta { color: #666; font-size: 0.85em; margin: 0.3em 0; }
          .meta a { margin-right: 0.3em; }
          .empty { color: #888; font-style: italic; margin-top: 2em; }
          .tiles { display: flex; flex-wrap: wrap; gap: 12px; margin: 1em 0 0.3em; }
          .tile  { border: 1px solid #ddd; border-radius: 6px; padding: 8px 14px; min-width: 9em; background: #fafafa; }
          .tile .label { font-size: 0.8em; color: #666; }
          .tile .value { font-size: 1.6em; font-weight: 600; }
          .tile .note  { font-size: 0.75em; color: #888; min-height: 1em; }
          .cols  { display: flex; flex-wrap: wrap; gap: 24px; align-items: flex-start; }
          .cols section { min-width: 18em; }
          table { border-collapse: collapse; font-size: 0.88em; }
          th, td { border-bottom: 1px solid #e3e3e3; padding: 3px 8px; text-align: left; vertical-align: top; }
          th    { background: #f0f0f0; border-bottom: 1px solid #ccc; }
          tr:hover td { background: #f7f9fc; }
          .num  { text-align: right; font-variant-numeric: tabular-nums; white-space: nowrap; }
          .ts   { font-variant-numeric: tabular-nums; white-space: nowrap; color: #444; }
          .path { font-family: ui-monospace, Consolas, monospace; word-break: break-all; max-width: 40em; }
          .method { color: #888; }
          .st   { display: inline-block; min-width: 2.6em; text-align: center; border-radius: 3px; padding: 0 4px; font-weight: 600; }
          .s2   { background: #e3f4e6; color: #1b6e2c; }
          .s3   { background: #e4edfa; color: #1f4f96; }
          .s4   { background: #fdf0dc; color: #9a5a00; }
          .s5   { background: #fbe3e3; color: #a31515; }
          .s0   { background: #eee; color: #666; }
          form.range { font-size: 0.85em; color: #444; margin: 0.6em 0 0.2em; }
          form.range input { font: inherit; padding: 1px 3px; }
          form.range a { margin-left: 0.3em; }
          .bad  { color: #a31515; margin-left: 0.5em; }
          </style>
          """;
}
