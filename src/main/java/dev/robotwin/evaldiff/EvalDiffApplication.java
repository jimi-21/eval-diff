package dev.robotwin.evaldiff;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EvalDiffApplication {

    public static void main(String[] args) {
        System.exit(new EvalDiffApplication().run(args));
    }

    public int run(String[] args) {
        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
            System.out.println(usage());
            return 0;
        }
        try {
            CliOptions options = CliOptions.parse(args);
            ObjectMapper objectMapper = new ObjectMapper();
            ScoreAuditLoader loader = new ScoreAuditLoader(objectMapper);
            ScoreAudit baseline = loader.load(options.baseline());
            ScoreAudit candidate = loader.load(options.candidate());
            ComparisonReport report = new EvalComparator().compare(baseline, candidate);
            ReportWriter writer = new ReportWriter(objectMapper);
            System.out.print(writer.console(report));
            if (options.jsonOutput() != null) {
                writer.writeJson(report, options.jsonOutput());
            }
            if (options.htmlOutput() != null) {
                writer.writeHtml(report, options.htmlOutput());
            }
            return report.verdict().equals("REGRESSION") ? 1 : 0;
        } catch (IllegalArgumentException exception) {
            System.err.println("eval-diff: " + exception.getMessage());
            System.err.println(usage());
            return 2;
        }
    }

    public String usage() {
        return "Usage: java -jar eval-diff.jar <baseline.json> <candidate.json> [--json report.json] [--html report.html]";
    }

    record CliOptions(Path baseline, Path candidate, Path jsonOutput, Path htmlOutput) {
        static CliOptions parse(String[] args) {
            List<Path> inputs = new ArrayList<>();
            Path json = null;
            Path html = null;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--json") || args[i].equals("--html")) {
                    if (++i >= args.length) {
                        throw new IllegalArgumentException("Missing output path after " + args[i - 1]);
                    }
                    if (args[i - 1].equals("--json")) {
                        json = Path.of(args[i]);
                    } else {
                        html = Path.of(args[i]);
                    }
                } else {
                    inputs.add(Path.of(args[i]));
                }
            }
            if (inputs.size() != 2) {
                throw new IllegalArgumentException("Expected exactly two score_audit JSON inputs");
            }
            return new CliOptions(inputs.get(0), inputs.get(1), json, html);
        }
    }
}
