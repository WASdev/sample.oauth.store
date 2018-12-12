/*
 * Copyright 2018 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package security.custom.store;

import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

/**
 * 
 * This Activator registers the MongoDBHelper class so it can optionally load configuration information
 * from the server.xml
 * 
 * It also stops the database connection when the bundle stops.
 *
 */
public class Activator implements BundleActivator {

	static final Logger LOGGER = Logger.getLogger(Activator.class.getName());

	ServiceRegistration configRef = null;

	@Override
	public void start(BundleContext context) throws Exception {
		// Do not connect to the MongoDB database at activation. Leave as lazy init.

		configRef = context.registerService(ManagedService.class.getCanonicalName(), MongoDBHelper.getInstance(),
				MongoDBHelper.getInstance().getDefaults());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (configRef != null) {
			configRef.unregister();
		}

		MongoDBHelper.getInstance().stopDB();
	}

}
