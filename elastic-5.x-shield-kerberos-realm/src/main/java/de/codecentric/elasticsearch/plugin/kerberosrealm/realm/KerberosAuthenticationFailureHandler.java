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
package de.codecentric.elasticsearch.plugin.kerberosrealm.realm;

import de.codecentric.elasticsearch.plugin.kerberosrealm.support.KrbConstants;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.transport.TransportMessage;
import org.elasticsearch.xpack.security.authc.AuthenticationToken;
import org.elasticsearch.xpack.security.authc.DefaultAuthenticationFailureHandler;

/**
 */
public class KerberosAuthenticationFailureHandler extends DefaultAuthenticationFailureHandler {


    @Override
    public ElasticsearchSecurityException failedAuthentication(final RestRequest request, final AuthenticationToken token, final ThreadContext context) {
        final ElasticsearchSecurityException e = super.failedAuthentication(request, token, context);
        e.addHeader(KrbConstants.WWW_AUTHENTICATE, KrbConstants.NEGOTIATE);
        return e;
    }
    @Override

    public ElasticsearchSecurityException failedAuthentication(TransportMessage message, AuthenticationToken token, String action, ThreadContext context) {
        final ElasticsearchSecurityException e = super.failedAuthentication(message, token, action, context);
        e.addHeader(KrbConstants.WWW_AUTHENTICATE, KrbConstants.NEGOTIATE);
        return e;

    }

    @Override
    public ElasticsearchSecurityException exceptionProcessingRequest(final RestRequest request, final Exception e, final ThreadContext context) {
        final ElasticsearchSecurityException se = super.exceptionProcessingRequest(request, e, context);
        String outToken = "";
        if (e instanceof ElasticsearchException) {
            final ElasticsearchException kae = (ElasticsearchException) e;
            if (kae.getHeader("kerberos_out_token") != null) {
                outToken = " " + kae.getHeader("kerberos_out_token").get(0);
            }
        }

        se.addHeader(KrbConstants.WWW_AUTHENTICATE, KrbConstants.NEGOTIATE + outToken);


        return se;
    }



    @Override
    public ElasticsearchSecurityException exceptionProcessingRequest(final TransportMessage message,
                                                                     final String action,
                                                                     final Exception e,
                                                                     final ThreadContext context) {
        final ElasticsearchSecurityException se = super.exceptionProcessingRequest(message,action, e, context);
        String outToken = "";
        if (e instanceof ElasticsearchException) {
            final ElasticsearchException kae = (ElasticsearchException) e;
            if (kae.getHeader("kerberos_out_token") != null) {
                outToken = " " + kae.getHeader("kerberos_out_token").get(0);
            }
        }
        se.addHeader(KrbConstants.WWW_AUTHENTICATE, KrbConstants.NEGOTIATE + outToken);


        return se;
    }


    @Override
    public ElasticsearchSecurityException missingToken(final RestRequest request, final ThreadContext context) {
        final ElasticsearchSecurityException e = super.missingToken(request, context);
        e.addHeader(KrbConstants.WWW_AUTHENTICATE, KrbConstants.NEGOTIATE);
        return e;
    }

    public ElasticsearchSecurityException missingToken(final TransportMessage message, final String action,final ThreadContext context) {
        final ElasticsearchSecurityException se = super.missingToken(message, action, context);
        se.addHeader(KrbConstants.WWW_AUTHENTICATE, KrbConstants.NEGOTIATE);
        return se;
    }


    @Override
    public ElasticsearchSecurityException authenticationRequired(final String action, final ThreadContext context) {
        final ElasticsearchSecurityException se = super.authenticationRequired(action, context);
        se.addHeader(KrbConstants.WWW_AUTHENTICATE, KrbConstants.NEGOTIATE);

        return se;
    }



}
