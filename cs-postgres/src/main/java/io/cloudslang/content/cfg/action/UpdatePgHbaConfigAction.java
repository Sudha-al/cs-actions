package io.cloudslang.content.cfg.action;

import com.hp.oo.sdk.content.annotations.Action;
import com.hp.oo.sdk.content.annotations.Output;
import com.hp.oo.sdk.content.annotations.Param;
import com.hp.oo.sdk.content.annotations.Response;
import com.hp.oo.sdk.content.plugin.ActionMetadata.MatchType;
import com.hp.oo.sdk.content.plugin.ActionMetadata.ResponseType;
import io.cloudslang.content.constants.ResponseNames;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.cloudslang.content.constants.OutputNames.*;
import static io.cloudslang.content.constants.OutputNames.STDERR;
import static io.cloudslang.content.constants.ReturnCodes.FAILURE;
import static io.cloudslang.content.constants.ReturnCodes.SUCCESS;

public class UpdatePgHbaConfigAction {

    @Action(name = "Update pg_hba.config",
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
            @Param(value = "allowed_hosts") String allowedHosts,
            @Param(value = "allowed_users") String allowedUsers
    ) {

        try {
            if(allowedHosts == null || allowedHosts.trim().length() == 0) {
                final Map<String, String> result = new HashMap<>();
                result.put(RETURN_CODE, "0");
                result.put(RETURN_RESULT, "No changes in  pg_hba.conf");
                return result ;
            }

            allowedHosts =  allowedHosts.replace("\'", "");
            if(allowedUsers == null || allowedUsers.trim().length() == 0) {
                allowedUsers = "all";
            }
            changeProperty(installationPath,allowedHosts.split(";"), allowedUsers.split(";"));
            final Map<String, String> result = new HashMap<>();
            result.put(RETURN_CODE, "0");
            result.put(RETURN_RESULT, "Updated pg_hba.conf successfully");
            return result ;
        } catch (Exception e) {
            return exceptionResult("Failed to update pg_hba.conf", e);
        }
    }

    public static void changeProperty(String filename, String[] allowedHosts, String[] allowedUsers) throws IOException {

        final File file = new File(filename);
        FileWriter pw = new FileWriter(file, true);

        for(int i = 0; i < allowedHosts.length; i++){
            StringBuilder addUserHostLineBuilder  = new StringBuilder();
             for (int j = 0; j < allowedUsers.length; j++) {
                 addUserHostLineBuilder.append("host").append("\t").append("all").append("\t").append(allowedUsers[j])
                         .append("\t").append(allowedHosts[i]).append("\t").append("trust").append("\n");
             }

             pw.write(addUserHostLineBuilder.toString());
        }
        pw.close();
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
