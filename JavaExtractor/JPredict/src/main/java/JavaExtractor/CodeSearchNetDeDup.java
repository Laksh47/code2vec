package JavaExtractor;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.FeaturesEntities.ProgramFeatures;
import com.github.javaparser.ParseException;
import com.github.javaparser.ParseProblemException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CodeSearchNetDeDup {
    private static CommandLineValues s_CommandLineValues;
    private static List<Integer> lineNumbers;

    public static void main(String[] args) throws IOException {
        try {
            s_CommandLineValues = new CommandLineValues(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
            return;
        }

//        String inputFile = "/home/larumuga/Downloads/codesearchnet_java/java_dedupe_definitions_v2.pkl";
        String inputFile =  "/home/larumuga/Downloads/codesearchnet_java/java_dedupe_definitions_v2.jsonl";
        String outputPath = "/home/larumuga/Downloads/codesearchnet_java/java/code2vec_features";
        processFile(Paths.get(inputFile), outputPath);
    }

    private static void processFile(Path f, String outputDir) throws IOException {
        String outputFile = outputDir + "/" + f.getFileName();
        lineNumbers = new ArrayList<Integer>();
        AtomicInteger i = new AtomicInteger(1);

        try (Stream<String> lines = Files.lines(f, Charset.defaultCharset());
             PrintWriter output = new PrintWriter(outputFile, Charset.defaultCharset()))
        {
            lines.map(line -> {
                String result = processLine(line, i.get());
                i.set(i.get() + 1);
                return result;
            }).forEachOrdered(output::println);
        }

        System.out.print(f.getFileName() + ": ");
        System.out.println(lineNumbers.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }


    private static String processLine(String line, int lineNumber) {
        JsonObject jsonObject = JsonParser.parseString(line).getAsJsonObject();
        String code = jsonObject.get("function").getAsString();
        //code = code.replaceAll("\\\\n", "").replaceAll("\\\\t", "");
        //if (lineNumber == 2501) {
        //    System.out.println(code);
        //}

        FeatureExtractor featureExtractor = new FeatureExtractor(s_CommandLineValues);
        ArrayList<ProgramFeatures> features = null;
        try {
            features = featureExtractor.extractFeatures(code);
        } catch (ParseProblemException e) {
            jsonObject.addProperty("path_contexts", "INCOMPLETE_SNIPPET void,void,void");
            lineNumbers.add(lineNumber);
            return jsonObject.toString();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        String toPrint = featuresToString(features);
        // System.out.println(toPrint);
        jsonObject.addProperty("path_contexts", toPrint);
        return jsonObject.toString();
    }

    public static String featuresToString(ArrayList<ProgramFeatures> features) {
        if (features == null || features.isEmpty()) {
            return Common.EmptyString;
        }

        List<String> methodsOutputs = new ArrayList<>();

        for (ProgramFeatures singleMethodfeatures : features) {
            StringBuilder builder = new StringBuilder();

            String toPrint = Common.EmptyString;
            toPrint = singleMethodfeatures.toPathString();
            builder.append(toPrint);

            methodsOutputs.add(builder.toString());

        }
        return StringUtils.join(methodsOutputs, "\n");
    }
}

