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
package de.codecentric.elasticsearch.plugin.kerberosrealm;

import de.codecentric.elasticsearch.plugin.kerberosrealm.realm.KerberosAuthenticationFailureHandler;
import de.codecentric.elasticsearch.plugin.kerberosrealm.realm.KerberosRealm;
import de.codecentric.elasticsearch.plugin.kerberosrealm.realm.KerberosRealmFactory;
import de.codecentric.elasticsearch.plugin.kerberosrealm.rest.LoginInfoRestAction;
import de.codecentric.elasticsearch.plugin.kerberosrealm.support.KrbConstants;
import de.codecentric.elasticsearch.plugin.kerberosrealm.support.PropertyUtil;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;

import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.extensions.XPackExtension;
import org.elasticsearch.xpack.security.authc.AuthenticationFailureHandler;
import org.elasticsearch.xpack.security.authc.Realm.Factory;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class KerberosRealmPlugin extends XPackExtension {

    private static final String CLIENT_TYPE = "client.type";
    private final boolean client;
    private final Settings settings;

    public KerberosRealmPlugin(final Settings settings) {
        this.settings = settings;
        client = !"node".equals(settings.get(CLIENT_TYPE, "node"));
//        logger.info("Start Kerberos Realm Plugin (mode: {})", settings.get(CLIENT_TYPE));
    }

    @Override
    public String name() {
        return KerberosRealm.TYPE + "-realm";
    }

    @Override
    public String description() {
        return "codecentric AG Kerberos V5 Realm for Elastic 5.x";
    }

    /**
     * Returns a collection of header names that will be used by this extension. This is necessary to ensure the headers are copied from
     * the incoming request and made available to your realm(s).
     */
    @Override
    public Collection<String> getRestHeaders() {
        return Arrays.asList(KerberosRealm.AUTHENTICATION_HEADER, KrbConstants.WWW_AUTHENTICATE );
    }

    /**
     * Returns a map of the custom realms provided by this extension. The first parameter is the string representation of the realm type;
     * this is the value that is specified when declaring a realm in the settings. Note, the realm type cannot be one of the types
     * defined by X-Pack. In order to avoid a conflict, you may wish to use some prefix to your realm types.
     *
     * The second parameter is an instance of the {@link Factory} implementation. This factory class will be used to create realms of
     * this type that are defined in the elasticsearch settings.
     */
    @Override
    public Map<String, Factory> getRealms(ResourceWatcherService resourceWatcherService) {
        return new MapBuilder<String, Factory>()
                .put(KerberosRealm.TYPE, new KerberosRealmFactory())
                .immutableMap();
    }

    /**
     * Returns the custom authentication failure handler. Note only one implementation and instance of a failure handler can
     * exist. There is a default implementation, {@link org.elasticsearch.xpack.security.authc.DefaultAuthenticationFailureHandler} that
     * can be extended where appropriate. If no changes are needed to the default implementation, then there is no need to override this
     * method.
     */
    @Override
    public AuthenticationFailureHandler getAuthenticationFailureHandler() {
        return new KerberosAuthenticationFailureHandler();
    }


//
//
//    public void onModule(final RestModule module) {
//        if (!client) {
//            module.addRestAction(LoginInfoRestAction.class);
//        }
//    }
//
//    @SuppressForbidden(reason = "proper use of Paths.get()")
//    public void onModule(final AuthenticationModule authenticationModule) {
//        if (!client) {
//            PropertyUtil.initKerberosProps(settings, Paths.get("/"));
//            authenticationModule.addCustomRealm(KerberosRealm.TYPE, KerberosRealmFactory.class);
//            authenticationModule.setAuthenticationFailureHandler(KerberosAuthenticationFailureHandler.class);
//        } else {
//            logger.warn("This plugin is not necessary for client nodes");
//        }
//    }
}
