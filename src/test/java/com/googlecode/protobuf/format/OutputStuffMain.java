package com.googlecode.protobuf.format;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.issue23.Issue23;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by scr on 4/5/16.
 */
public class OutputStuffMain {

    static final FormatFactory FORMAT_FACTORY = new FormatFactory();

    private final List<Path> needPretty = new ArrayList<Path>();

    private Issue23.NewTestMessage getComplex() {
        return Issue23.NewTestMessage.newBuilder()
            .setKnownfield("hello")
            .setUnknownfieldstring1("world")
            .addUnknownfieldstring2("I")
            .addUnknownfieldstring2("am")
            .setUnknownfieldMessage(Issue23.InnerTestMessage.newBuilder()
                                        .setValue(30)
                                        .setInnerMessage(
                                            Issue23.InnerTestMessage.InnerInnerTestMessage.newBuilder().setValue(1.2f))
            )
            .setUnknownfieldInt64(51232271120233L)
            .setUnknownfieldInt32(6)
            .setUnknownfieldFloat(2.3f)
            .setUnknownfieldDouble(3.14d)
            .addUnknownGroup(Issue23.NewTestMessage.UnknownGroup.newBuilder()
                                 .setName("hi")
                                 .setIntvalue(23)
                                 .setFloatvalue(5.2f)
                                 .setLongvalue(44232993922327L))
            .addUnknownfieldRepeatedMessage(Issue23.InnerTestMessage.newBuilder()
                                                .setValue(6)
                                                .setInnerMessage(
                                                    Issue23.InnerTestMessage.InnerInnerTestMessage.newBuilder()
                                                        .setValue(-1.3f))
            )
            .addUnknownfieldRepeatedMessage(Issue23.InnerTestMessage.newBuilder()
                                                .setValue(-110)
                                                .setInnerMessage(
                                                    Issue23.InnerTestMessage.InnerInnerTestMessage.newBuilder()
                                                        .setValue(0f))
            )
            .setUnknownBytes(ByteString.copyFromUtf8("foo bar baz"))
            .build();
    }

    private Issue23.OldTestMessage loadOldMessage(Path inputPath, ProtobufFormatter formatter) throws IOException {
        Issue23.OldTestMessage.Builder ret = Issue23.OldTestMessage.newBuilder();
        InputStream inputStream = null;
        try {
            inputStream = Files.newInputStream(inputPath);
            formatter.merge(inputStream, ret);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return ret.build();
    }

    private void writeFile(Path filePath, Message message, ProtobufFormatter formatter) throws IOException {
        needPretty.add(filePath);
        OutputStream outputStream = null;
        try {
            outputStream = Files.newOutputStream(filePath);
            formatter.print(message, outputStream);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    public void doRun() throws IOException, InterruptedException {
        Message complexMessage = getComplex();
        JsonFormat jsonFormat = (JsonFormat) FORMAT_FACTORY.createFormatter(FormatFactory.Formatter.JSON);
        JsonJacksonFormat jsonJacksonFormat =
            (JsonJacksonFormat) FORMAT_FACTORY.createFormatter(FormatFactory.Formatter.JSON_JACKSON);

        // Write out the "new" complex messages with the two formatters
        Path complexJson = Paths.get("complexJson.json");
        writeFile(complexJson, complexMessage, jsonFormat);
        Path complexJsonJackson = Paths.get("complexJsonJackson.json");
        writeFile(complexJsonJackson, complexMessage, jsonJacksonFormat);

        // Now read them directly to an old message and write with the two formatters
        Issue23.OldTestMessage oldComplexMessage = Issue23.OldTestMessage.parseFrom(complexMessage.toByteArray());
        Path oldComplexJson = Paths.get("oldComplexJson.json");
        writeFile(oldComplexJson, oldComplexMessage, jsonFormat);
        Path oldComplexJsonJackson = Paths.get("oldComplexJsonJackson.json");
        writeFile(oldComplexJsonJackson, oldComplexMessage, jsonJacksonFormat);

        // Now read them back in to old messages and write them out with the two formatters
        Issue23.OldTestMessage oldJsonMessage = loadOldMessage(complexJson, jsonFormat);
        Path complexOldJson = Paths.get("complexOldJson.json");
        writeFile(complexOldJson, oldJsonMessage, jsonFormat);
        Issue23.OldTestMessage oldJsonJacksonMessage = loadOldMessage(complexJsonJackson, jsonFormat);
        Path complexOldJsonJackon = Paths.get("complexOldJsonJackson.json");
        writeFile(complexOldJsonJackon, oldJsonJacksonMessage, jsonJacksonFormat);

        Runtime runtime = Runtime.getRuntime();
        for (Path path : needPretty) {
            // Pretty the input file to a tmp file
            Path tmpPath = path.resolveSibling(path.getFileName() + ".tmp");
            runtime.exec(new String[]{
                "python",
                "-mjson.tool",
                path.toString(),
                tmpPath.toString()
            }).waitFor();

            // Move the tmp file over the orig file
            Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
        }

    }

    public static void main(String[] args) {
        try {
            new OutputStuffMain().doRun();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
