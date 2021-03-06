/*
 * Copyright (C) 2020 ActiveJ LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.activej.launchers.fs;

import io.activej.async.service.EventloopTaskScheduler;
import io.activej.config.Config;
import io.activej.eventloop.Eventloop;
import io.activej.fs.ActiveFs;
import io.activej.fs.cluster.ClusterRepartitionController;
import io.activej.fs.cluster.FsPartitions;
import io.activej.fs.cluster.ServerSelector;
import io.activej.fs.tcp.ActiveFsServer;
import io.activej.inject.annotation.Inject;
import io.activej.inject.annotation.Named;
import io.activej.inject.annotation.Optional;
import io.activej.inject.annotation.Provides;
import io.activej.launcher.Launcher;

import java.util.HashMap;
import java.util.Map;

import static io.activej.common.Utils.nullToDefault;
import static io.activej.fs.cluster.ServerSelector.RENDEZVOUS_HASH_SHARDER;
import static io.activej.launchers.fs.Initializers.ofClusterRepartitionController;
import static io.activej.launchers.fs.Initializers.ofFsPartitions;
import static io.activej.launchers.initializers.Initializers.ofEventloopTaskScheduler;

/**
 * @deprecated use {@link ClusterTcpServerLauncher} instead
 */
public abstract class ClusterRepartitionControllerLauncher extends ActiveFsServerLauncher {

	@Inject
	ClusterRepartitionController controller;

	@Inject
	@Named("repartition")
	EventloopTaskScheduler repartitionScheduler;

	@Inject
	@Named("clusterDeadCheck")
	EventloopTaskScheduler clusterDeadCheckScheduler;

	@Provides
	@Named("repartition")
	EventloopTaskScheduler eventloopTaskScheduler(Config config, ClusterRepartitionController controller) {
		return EventloopTaskScheduler.create(controller.getEventloop(), controller::repartition)
				.withInitializer(ofEventloopTaskScheduler(config.getChild("activefs.repartition")));
	}

	@Provides
	@Named("clusterDeadCheck")
	EventloopTaskScheduler deadCheckScheduler(Config config, FsPartitions partitions) {
		return EventloopTaskScheduler.create(partitions.getEventloop(), partitions::checkDeadPartitions)
				.withInitializer(ofEventloopTaskScheduler(config.getChild("activefs.repartition.deadCheck")));
	}

	@Provides
	ClusterRepartitionController repartitionController(Config config,
			ActiveFsServer localServer, FsPartitions partitions) {
		String localPartitionId = config.get("activefs.repartition.localPartitionId");
		return ClusterRepartitionController.create(localPartitionId, partitions)
				.withInitializer(ofClusterRepartitionController(config.getChild("activefs.repartition")));
	}

	@Provides
	FsPartitions fsPartitions(Config config, Eventloop eventloop, @Optional ServerSelector serverSelector, ActiveFs fs) {
		Map<Object, ActiveFs> partitions = new HashMap<>();
		partitions.put(config.get("activefs.repartition.localPartitionId"), fs);
		return FsPartitions.create(eventloop, partitions)
				.withServerSelector(nullToDefault(serverSelector, RENDEZVOUS_HASH_SHARDER))
				.withInitializer(ofFsPartitions(config.getChild("activefs.cluster")));
	}

	public static void main(String[] args) throws Exception {
		Launcher launcher = new ClusterRepartitionControllerLauncher() {};
		launcher.launch(args);
	}
}
