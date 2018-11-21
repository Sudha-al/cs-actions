package io.cloudslang.content.cfg.action;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.hp.oo.sdk.content.annotations.Action;
import com.hp.oo.sdk.content.annotations.Output;
import com.hp.oo.sdk.content.annotations.Param;
import com.hp.oo.sdk.content.annotations.Response;
import com.hp.oo.sdk.content.plugin.ActionMetadata.MatchType;
import com.hp.oo.sdk.content.plugin.ActionMetadata.ResponseType;
import com.hp.oo.sdk.content.plugin.GlobalSessionObject;
import io.cloudslang.content.constants.ResponseNames;
import io.cloudslang.content.utils.BooleanUtilities;

import static io.cloudslang.content.constants.OutputNames.*;
import static io.cloudslang.content.constants.ReturnCodes.FAILURE;
import static io.cloudslang.content.constants.ReturnCodes.SUCCESS;

public class UpdatePostgresConfigAction {

    @Action(name = "Update Property Value",
            outputs = {
                    @Output(RETURN_CODE),
                    @Output(RETURN_RESULT),
                    @Output(EXCEPTION),
                    @Output(STDERR)
            },
            responses = {
                    @Response(text = ResponseNames.SUCCESS, field = RETURN_CODE, value = SUCCESS,
                            matchType = MatchType.COMPARE_EQUAL,
                            responseType = ResponseType.RESOLVED),
                    @Response(text = ResponseNames.FAILURE, field = RETURN_CODE, value = FAILURE,
                            matchType = MatchType.COMPARE_EQUAL, responseType = ResponseType.ERROR, isOnFail = true)
            })
    public Map<String, String> execute(
            @Param(value ="file_path", required = true) String installationPath,
            @Param(value = "port") String port,
            @Param(value = "ssl") String ssl,
            @Param(value = "ssl_ca_file") String sslCaFile,
            @Param(value = "ssl_cert_file") String sslCertFile,
            @Param(value = "ssl_key_file") String sslKeyFile,
            @Param(value = "max_connections") String maxConnections,
            @Param(value = "shared_buffers") String sharedBuffers,
            @Param(value = "effective_cache_size") String effectiveCacheSize,
            @Param(value = "autovacuum") String autovacuum,
            @Param(value = "work_mem") String workMem
    ) {

        try {
            Map<String, Object> keyValues = new HashMap<String, Object>();
            if(port != null) {
                keyValues.put("port", Integer.parseInt(port));
            }
            if(ssl != null && ssl.length() > 0) {
                keyValues.put("ssl", ssl);
            }
            if(sslCaFile != null && sslCaFile.length() > 0) {
                keyValues.put("ssl_ca_file", sslCaFile);
            }
            if(sslCertFile != null && sslCertFile.length() >0) {
                keyValues.put("ssl_cert_file", sslCertFile);
            }
            if(sslKeyFile != null && sslKeyFile.length() > 0) {
                keyValues.put("ssl_key_file", sslKeyFile);
            }

            if(maxConnections != null) {
                keyValues.put("max_connections", Integer.parseInt(maxConnections));
            }

            if(sharedBuffers != null && sharedBuffers.length() >0) {
                 keyValues.put("shared_buffers", sharedBuffers);
            }

            if(effectiveCacheSize != null && effectiveCacheSize.length() >0) {
                keyValues.put("effective_cache_size", effectiveCacheSize);
            }

            if(autovacuum != null && autovacuum.length() >0) {
                keyValues.put("autovacuum", autovacuum);
            }

            if(workMem != null && workMem.length() >0) {
                keyValues.put("work_mem", workMem);
            }

            changeProperty(installationPath, keyValues);
            final Map<String, String> result = new HashMap<>();
            result.put(RETURN_CODE, "0");
            result.put(RETURN_RESULT, "Updated postgresql.conf successfully");
            return result ;
        } catch (Exception e) {
            return exceptionResult("Failed to update postgresql.conf", e);
        }
    }

    public static void changeProperty(String filename, Map<String, Object> keyValuePairs) throws IOException {
        if(keyValuePairs.size() == 0) {
            return;
        }
        final File file = new File(filename);
        final File tmpFile = new File(file + ".tmp");

        PrintWriter pw = new PrintWriter(tmpFile);
        BufferedReader br = new BufferedReader(new FileReader(file));

        Set<String> keys = keyValuePairs.keySet();

        for (String line; (line = br.readLine()) != null; ) {
            int keyPos = line.indexOf('=');
            if (keyPos > -1) {
                String key = line.substring(0, keyPos).trim();
                int startKeyIndex = key.startsWith("#") ? 1 : 0;
                key = key.startsWith("#") ? key.substring(1) : key;
                if (keys.contains(key)) {
                    // Check if the line has any comments.  Split by '#' to funs all tokens.
                    String[] tokens = line.split("#");
                    String[] keyValuePair = tokens[startKeyIndex].split("=");

                    StringBuilder lineBuilder = new StringBuilder();
                    lineBuilder.append(keyValuePair[0]).append(" = ").append(keyValuePairs.get(key)).append('\t');
                    // Concat all other tokens with '#' if they exists
                    for(int i = startKeyIndex + 1; i< tokens.length; i++) {
                        lineBuilder.append("#").append(tokens[i]);
                    }
                    line = lineBuilder.toString();
                }
            }
            pw.println(line);
        }
        br.close();
        pw.close();
        // https://bugs.java.com/view_bug.do?bug_id=6213298
        file.delete();
        tmpFile.renameTo(file);
    }
    private Map<String, String> exceptionResult(String message, Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        String exStr = writer.toString().replace("" + (char) 0x00, "");
        Map<String, String> returnResult = new HashMap<>();
        returnResult.put(RETURN_RESULT, message);
        returnResult.put(RETURN_CODE, FAILURE);
        returnResult.put(EXCEPTION, e.getMessage());
        returnResult.put(STDERR, exStr);
        return returnResult;
    }
}
