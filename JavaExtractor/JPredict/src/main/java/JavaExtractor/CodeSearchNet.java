package JavaExtractor;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.FeaturesEntities.ProgramFeatures;
import com.github.javaparser.ParseException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CodeSearchNet {
    public static void main(String[] args) throws IOException {
        String input_filepath = new String("/home/larumuga/Downloads/codesearchnet_java/java/final/jsonl/train");
        String train_in = input_filepath + "/java_sample.jsonl";
        System.out.println(train_in);

        String output_filepath = new String("/home/larumuga/Downloads/codesearchnet_java/java/code2vec_features/train");
        String train_out = output_filepath + "/java_sample.jsonl";

        CommandLineValues s_CommandLineValues;
        try {
            s_CommandLineValues = new CommandLineValues(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
            return;
        }

        try (Stream<String> lines = Files.lines(Paths.get(train_in), Charset.defaultCharset());
             PrintWriter output = new PrintWriter(train_out, Charset.defaultCharset()))
        {
            //lines.forEachOrdered(line -> processLine(line, s_CommandLineValues));
            lines.map(line -> processLine(line, s_CommandLineValues)).forEachOrdered(output::println);
        }
    }

    private static String processLine(String line, CommandLineValues s_CommandLineValues) {
        JsonObject jsonObject = JsonParser.parseString(line).getAsJsonObject();
        String code = jsonObject.get("code").getAsString();
        // code = code.replaceAll("\\\\n", "").replaceAll("\\\\t", "");
        // System.out.println(code);

        FeatureExtractor featureExtractor = new FeatureExtractor(s_CommandLineValues);
        ArrayList<ProgramFeatures> features = null;
        try {
            features = featureExtractor.extractFeatures(code);
        } catch (ParseException | IOException e) {
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

