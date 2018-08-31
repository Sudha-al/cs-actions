/*
 * (c) Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 which accompany this distribution.
 *
 * The Apache License is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cloudslang.content.amazon.actions.servicecatalog;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.servicecatalog.AWSServiceCatalog;
import com.amazonaws.services.servicecatalog.model.ProvisionProductResult;
import com.hp.oo.sdk.content.annotations.Action;
import com.hp.oo.sdk.content.annotations.Output;
import com.hp.oo.sdk.content.annotations.Param;
import com.hp.oo.sdk.content.annotations.Response;
import com.hp.oo.sdk.content.plugin.ActionMetadata.MatchType;
import com.hp.oo.sdk.content.plugin.ActionMetadata.ResponseType;
import io.cloudslang.content.amazon.entities.constants.Outputs;
import io.cloudslang.content.amazon.entities.validators.Validator;
import io.cloudslang.content.amazon.factory.CloudFormationClientBuilder;
import io.cloudslang.content.amazon.factory.ServiceCatalogClientBuilder;
import io.cloudslang.content.amazon.services.AmazonServiceCatalogService;
import io.cloudslang.content.amazon.utils.DefaultValues;

import java.util.List;
import java.util.Map;

import static io.cloudslang.content.amazon.entities.constants.Constants.ServiceCatalogActions.CREATE_IN_PROGRESS;
import static io.cloudslang.content.amazon.entities.constants.Descriptions.Common.*;
import static io.cloudslang.content.amazon.entities.constants.Descriptions.ProvisionProductAction.*;
import static io.cloudslang.content.amazon.entities.constants.Inputs.CommonInputs.*;
import static io.cloudslang.content.amazon.entities.constants.Inputs.ServiceCatalogInputs.*;
import static io.cloudslang.content.amazon.entities.constants.Inputs.ServiceCatalogInputs.PATH_ID;
import static io.cloudslang.content.amazon.entities.constants.Inputs.ServiceCatalogInputs.PRODUCT_ID;
import static io.cloudslang.content.amazon.entities.constants.Inputs.ServiceCatalogInputs.PROVISIONED_PRODUCT_NAME;
import static io.cloudslang.content.amazon.entities.constants.Inputs.ServiceCatalogInputs.PROVISIONING_ARTIFACT_ID;
import static io.cloudslang.content.amazon.entities.constants.Outputs.*;
import static io.cloudslang.content.amazon.services.AmazonServiceCatalogService.describeCloudFormationStack;
import static io.cloudslang.content.amazon.services.AmazonServiceCatalogService.describeStackResources;
import static io.cloudslang.content.amazon.services.AmazonServiceCatalogService.getCloudFormationStackName;
import static io.cloudslang.content.amazon.utils.OutputsUtil.*;
import static io.cloudslang.content.amazon.utils.ServiceCatalogUtil.*;
import static io.cloudslang.content.utils.OutputUtilities.getFailureResultsMap;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;


public class ProvisionProductAction {

    @Action(name = "Provision Product", description = PROVISION_PRODUCT_DESCRIPTION,
            outputs = {
                    @Output(value = Outputs.RETURN_CODE, description = RETURN_CODE_DESC),
                    @Output(value = Outputs.RETURN_RESULT, description = RETURN_RESULT_DESC),
                    @Output(value = Outputs.EXCEPTION, description = EXCEPTION_DESC),
                    @Output(value = Outputs.CREATED_TIME, description = CREATED_TIME_DESC),
                    @Output(value = Outputs.PATH_ID, description = PATH_ID_DESC),
                    @Output(value = Outputs.PRODUCT_ID, description = PRODUCT_ID_DESC),
                    @Output(value = Outputs.PROVISIONED_PRODUCT_ID, description = PROVISIONED_PRODUCT_ID_DESC),
                    @Output(value = Outputs.PROVISIONED_PRODUCT_NAME, description = PROVISIONED_PRODUCT_NAME_DESC),
                    @Output(value = Outputs.PROVISIONED_PRODUCT_TYPE, description = PROVISIONED_PRODUCT_TYPE_DESC),
                    @Output(value = Outputs.PROVISIONING_ARTIFACT_ID, description = PROVISIONING_ARTIFACT_ID_OUT_DESC),
                    @Output(value = Outputs.STATUS, description = STATUS_DESC),
                    @Output(value = Outputs.STACK_ID, description = STACK_ID_DESC),
                    @Output(value = Outputs.STACK_NAME, description = STACK_NAME_DESC),
                    @Output(value = Outputs.STACK_OUTPUTS, description = STACK_OUTPUTS_DESC),
                    @Output(value = Outputs.STACK_RESOURCES, description = STACK_RESOURCES_DESC)

            },
            responses = {
                    @Response(text = Outputs.SUCCESS, field = Outputs.RETURN_CODE, value = Outputs.SUCCESS_RETURN_CODE,
                            matchType = MatchType.COMPARE_EQUAL, responseType = ResponseType.RESOLVED, description = SUCCESS_DESC),
                    @Response(text = Outputs.FAILURE, field = Outputs.RETURN_CODE, value = Outputs.FAILURE_RETURN_CODE,
                            matchType = MatchType.COMPARE_EQUAL, responseType = ResponseType.ERROR, description = FAILURE_DESC)
            }
    )
    public Map<String, String> execute(@Param(value = IDENTITY, required = true, description = IDENTITY_DESC) final String identity,
                                       @Param(value = CREDENTIAL, required = true, encrypted = true, description = CREDENTIAL_DESC) final String credential,
                                       @Param(value = PROXY_HOST, description = PROXY_HOST_DESC) final String proxyHost,
                                       @Param(value = PROXY_PORT, description = PROXY_PORT_DESC) final String proxyPort,
                                       @Param(value = PROXY_USERNAME, description = PROXY_USERNAME_DESC) final String proxyUsername,
                                       @Param(value = PROXY_PASSWORD, encrypted = true, description = PROXY_PASSWORD_DESC) final String proxyPassword,
                                       @Param(value = CONNECT_TIMEOUT, description = CONNECT_TIMEOUT_DESC) String connectTimeout,
                                       @Param(value = EXECUTION_TIMEOUT, description = EXECUTION_TIMEOUT_DESC) String execTimeout,
                                       @Param(value = POOLING_INTERVAL, description = POOLING_INTERVAL_DESC) String poolingInterval,
                                       @Param(value = ASYNC, description = ASYNC_DESC) String async,
                                       @Param(value = PRODUCT_ID, required = true, description = PRODUCT_ID_DESC) String productId,
                                       @Param(value = PROVISIONED_PRODUCT_NAME, required = true, description = PROVISIONED_PRODUCT_NAME_DESC) String provisionedProductName,
                                       @Param(value = PROVISIONING_ARTIFACT_ID, required = true, description = PROVISIONING_ARTIFACT_ID_DESC) String provisioningArtifactId,
                                       @Param(value = PARAM_KEY_NAME, required = true, description = PROVISIONING_PARAM_KEY_NAME) String paramKeyName,
                                       @Param(value = PARAM_SSH_LOCATION, description = PROVISIONING_PARAM_SSH_LOCATION) String paramSshLocation,
                                       @Param(value = PARAM_INSTANCE_TYPE, description = PROVISIONING_PARAM_INSTANCE_TYPE) String paramInstanceType,
                                       @Param(value = TAGS, description = TAGS_DESC) String tags,
                                       @Param(value = PROVISION_TOKEN, description = PROVISION_TOKEN_DESC) String provisionTokens,
                                       @Param(value = ACCEPT_LANGUAGE, description = ACCEPT_LANGUAGE_DESC) String acceptLanguage,
                                       @Param(value = NOTIFICATION_ARNS, description = NOTIFICATION_ARNS_DESC) String notificationArns,
                                       @Param(value = PATH_ID, description = PATH_ID_DESC) String pathId,
                                       @Param(value = REGION, description = REGION_DESC) String region) {
        //Assign default values to inputs
        final String proxyPortVal = defaultIfEmpty(proxyPort, DefaultValues.PROXY_PORT);
        final String connectTimeoutVal = defaultIfEmpty(connectTimeout, DefaultValues.CONNECT_TIMEOUT);
        final String execTimeoutVal = defaultIfEmpty(execTimeout, DefaultValues.EXEC_TIMEOUT);
        final String asyncVal = defaultIfEmpty(async, DefaultValues.ASYNC);
        final String poolingIntervalVal = defaultIfEmpty(poolingInterval, DefaultValues.POOLING_INTERVAL_DEFAULT);


        //Validate inputs
        Validator validator = new Validator()
                .validatePort(proxyPortVal, PROXY_PORT)
                .validateInt(connectTimeoutVal, CONNECT_TIMEOUT)
                .validateInt(execTimeoutVal, EXECUTION_TIMEOUT)
                .validateBoolean(asyncVal, ASYNC);

        if (validator.hasErrors()) {
            return getFailureResultsMap(validator.getErrors());
        }

        //Variable conversions
        final Integer proxyPortImp = Integer.valueOf(proxyPortVal);
        final Integer connectTimeoutImp = Integer.valueOf(connectTimeoutVal);
        final Integer execTimeoutImp = Integer.valueOf(execTimeoutVal);
        final Long poolingIntervalImp = Long.valueOf(poolingIntervalVal);
        final Boolean asyncImp = Boolean.valueOf(asyncVal);

        try {

            final AWSServiceCatalog awsServiceCatalog = ServiceCatalogClientBuilder.getServiceCatalogClientBuilder(identity, credential,
                    proxyHost, proxyPortImp, proxyUsername, proxyPassword, connectTimeoutImp, execTimeoutImp, region, asyncImp);
            final ProvisionProductResult result = AmazonServiceCatalogService.provisionProduct(provisionedProductName,
                    setProvisionParameters(paramKeyName, paramSshLocation, paramInstanceType), productId, provisionTokens, provisioningArtifactId,
                    toArrayOfTags(tags), acceptLanguage, notificationArns, pathId, awsServiceCatalog);

            Map<String, String> results = getSuccessResultMapProvisionProduct(result);
            final String cloudFormationStackName = getCloudFormationStackName(result.getRecordDetail().getRecordId(), awsServiceCatalog, poolingIntervalImp);

            final AmazonCloudFormation awsCloudFormation = CloudFormationClientBuilder.getCloudFormationClient(identity, credential,
                    proxyHost, proxyPort, proxyUsername, proxyPassword, connectTimeoutVal, execTimeoutVal, region);
            List<Stack> stacks = describeCloudFormationStack(cloudFormationStackName, awsCloudFormation);
            while (stacks.get(0).getStackStatus().equals(CREATE_IN_PROGRESS)) {
                Thread.sleep(poolingIntervalImp);
                stacks = describeCloudFormationStack(cloudFormationStackName, awsCloudFormation);
            }

            results.put(STACK_NAME, stacks.get(0).getStackName());
            results.put(STACK_ID, stacks.get(0).getStackId());
            results.put(STACK_OUTPUTS, stacks.get(0).getOutputs().toString());
            results.put(STACK_RESOURCES, describeStackResources(cloudFormationStackName, awsCloudFormation));


            return results;
        } catch (Exception e) {

            return getFailureResultsMap(e);

        }
    }
}
