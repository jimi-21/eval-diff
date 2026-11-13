package dev.robotwin.evaldiff;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class ReportWriter {

    private final ObjectMapper objectMapper;

    public ReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String console(ComparisonReport report) {
        StringBuilder output = new StringBuilder();
        output.append("EvalDiff · local evaluation comparison\n");
        output.append("========================================\n");
        output.append("Verdict: ").append(report.verdict()).append('\n');
        output.append("Scenes: ").append(report.sceneCount())
                .append(" | improvements: ").append(report.improvements())
                .append(" | regressions: ").append(report.regressions())
                .append(" | faster: ").append(report.fasterScenes())
                .append(" | slower: ").append(report.slowerScenes()).append('\n');
        for (MetricDelta metric : report.metrics()) {
            output.append(String.format("%-18s %s -> %s (%s)%n", metric.name(), number(metric.baseline()),
                    number(metric.candidate()), signed(metric.delta())));
        }
        output.append("\nScene details\n");
        for (SceneComparison scene : report.scenes()) {
            output.append(String.format("  episode %-3d seed %-4d %-18s steps %d -> %d (%+d)%n",
                    scene.episode(), scene.seed(), scene.change(), scene.baselineSteps(), scene.candidateSteps(),
                    scene.stepsDelta()));
        }
        output.append("\nLocal experiment data only; not an official RoboTwin score.\n");
        return output.toString();
    }

    public void writeJson(ComparisonReport report, Path path) {
        write(path, () -> objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), report));
    }

    public void writeHtml(ComparisonReport report, Path path) {
        write(path, () -> Files.writeString(path, html(report)));
    }

    private void write(Path path, IoAction action) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            action.run();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not write report: " + path, exception);
        }
    }

    private String html(ComparisonReport report) {
        StringBuilder html = new StringBuilder("""
                <!doctype html><html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1"><title>EvalDiff Report</title>
                <style>
                :root{color-scheme:dark;--bg:#08131f;--panel:#0f2030;--line:#244054;--text:#e9f2f7;--muted:#8fa8ba;--teal:#38d7bd;--red:#ff7e89;--green:#62dda3}*{box-sizing:border-box}body{margin:0;background:radial-gradient(circle at 90% 0,#153d4c,transparent 38%),var(--bg);color:var(--text);font-family:Inter,Segoe UI,sans-serif}.wrap{max-width:1180px;margin:auto;padding:40px 24px}.eyebrow{color:var(--teal);font-size:12px;font-weight:800;letter-spacing:2px}h1{margin:10px 0;font-size:36px}.sub,.note{color:var(--muted);line-height:1.7}.cards{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin:28px 0}.card,.panel{background:#0f2030ee;border:1px solid var(--line);border-radius:16px}.card{padding:20px}.label{color:var(--muted);font-size:12px}.value{font-size:28px;font-weight:750;margin-top:8px}.verdict{color:var(--red)}.panel{overflow:hidden}.panel h2{padding:20px;margin:0;font-size:17px;border-bottom:1px solid var(--line)}table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:14px 20px;border-bottom:1px solid #1d3345;white-space:nowrap}th{color:var(--muted);font-size:11px}td{font-size:13px}.tag{padding:5px 9px;border-radius:20px;font-size:11px;font-weight:700}.REGRESSION{background:#ff7e8918;color:var(--red)}.IMPROVED,.IMPROVEMENT,.FASTER{background:#62dda318;color:var(--green)}.NO_CHANGE,.UNCHANGED_SUCCESS{background:#68a7ff18;color:#82b4f7}.UNCHANGED_FAILURE,.SLOWER{background:#ffb86b18;color:#ffbd7a}.metrics{display:grid;grid-template-columns:repeat(2,1fr);gap:0}.metric{padding:18px 20px;border-bottom:1px solid #1d3345}.metric strong{display:block;margin-bottom:6px}.delta{color:var(--muted);font-family:Consolas,monospace}.footer{margin-top:22px;font-size:12px;color:var(--muted)}@media(max-width:720px){.cards{grid-template-columns:repeat(2,1fr)}.metrics{grid-template-columns:1fr}.panel{overflow-x:auto}h1{font-size:28px}}
                </style></head><body><main class="wrap"><div class="eyebrow">EVALDIFF / LOCAL EXPERIMENT</div>
                <h1>Evaluation comparison</h1><div class="sub">RoboTwin score audit regression analysis</div>
                <section class="cards">
                """);
        html.append(card("VERDICT", report.verdict(), "verdict"))
                .append(card("SCENES", Integer.toString(report.sceneCount()), ""))
                .append(card("IMPROVEMENTS", Integer.toString(report.improvements()), ""))
                .append(card("REGRESSIONS", Integer.toString(report.regressions()), ""));
        html.append("</section><section class=\"panel\"><h2>Metric deltas</h2><div class=\"metrics\">");
        for (MetricDelta metric : report.metrics()) {
            html.append("<div class=\"metric\"><strong>").append(escape(metric.name())).append("</strong><span class=\"delta\">")
                    .append(number(metric.baseline())).append(" → ").append(number(metric.candidate()))
                    .append(" (").append(signed(metric.delta())).append(")</span></div>");
        }
        html.append("</div></section><br><section class=\"panel\"><h2>Scene details</h2><table><thead><tr><th>EPISODE</th><th>SEED</th><th>BASELINE</th><th>CANDIDATE</th><th>STEPS Δ</th><th>CHANGE</th></tr></thead><tbody>");
        for (SceneComparison scene : report.scenes()) {
            html.append("<tr><td>").append(scene.episode()).append("</td><td>").append(scene.seed())
                    .append("</td><td>").append(scene.baselineSuccess() ? "SUCCESS" : "FAILURE")
                    .append("</td><td>").append(scene.candidateSuccess() ? "SUCCESS" : "FAILURE")
                    .append("</td><td>").append(String.format("%+d", scene.stepsDelta())).append("</td><td><span class=\"tag ")
                    .append(scene.change()).append("\">").append(scene.change()).append("</span></td></tr>");
        }
        html.append("</tbody></table></section><div class=\"footer\">Generated at ")
                .append(escape(Instant.now().toString()))
                .append("<br>Local experiment data only; not an official RoboTwin score or ranking.</div></main></body></html>");
        return html.toString();
    }

    private String card(String label, String value, String cssClass) {
        return "<div class=\"card\"><div class=\"label\">" + escape(label) + "</div><div class=\"value " + cssClass
                + "\">" + escape(value) + "</div></div>";
    }

    private String number(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String signed(BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + number(value);
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}
