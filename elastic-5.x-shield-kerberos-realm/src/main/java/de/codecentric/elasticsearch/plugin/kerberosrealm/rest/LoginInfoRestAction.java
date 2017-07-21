/*
   Copyright 2015 codecentric AG

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Author: Hendrik Saly <hendrik.saly@codecentric.de>
 */
package de.codecentric.elasticsearch.plugin.kerberosrealm.rest;

import org.elasticsearch.action.main.MainAction;
import org.elasticsearch.action.main.MainRequest;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.RestRequest.Method;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.security.user.User;

import java.io.IOException;

public class LoginInfoRestAction extends BaseRestHandler {

    @Inject
    public LoginInfoRestAction(final Settings settings, final RestController controller, final Client client) {
        super(settings);
        controller.registerHandler(Method.GET, "/_logininfo", this);
    }

    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        return channel -> client.execute(MainAction.INSTANCE, new MainRequest(), new RestBuilderListener<MainResponse>(channel) {
            @Override
            public RestResponse buildResponse(MainResponse response, XContentBuilder builder) throws Exception {

                RestStatus status = response.isAvailable() ? RestStatus.OK : RestStatus.SERVICE_UNAVAILABLE;

                // Default to pretty printing, but allow ?pretty=false to disable
                if (!request.hasParam("pretty")) {
                    builder.prettyPrint().lfAtEnd();
                }
                response.toXContent(builder, request);

                try {
                    builder.startObject();
                    final User user = ((User) client.threadPool().getThreadContext().getTransient("_shield_user"));
                    if (user != null) {
                        builder.field("principal", user.principal());
                        builder.field("roles", user.roles());
                    } else {
                        builder.nullField("principal");
                    }

                    builder.field("remote_address", (String) client.threadPool().getThreadContext().getTransient("_rest_remote_address"));
                    builder.endObject();
//                    response = new BytesRestResponse(RestStatus.OK, builder);
                } catch (final Exception e1) {
                    builder.startObject();
                    builder.field("error", e1.toString());
                    builder.endObject();
//                    response = new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, builder);
                }


                return new BytesRestResponse(status, builder);

            }
        });
    }


}
