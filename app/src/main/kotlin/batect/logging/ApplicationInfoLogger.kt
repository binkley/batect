/*
   Copyright 2017-2019 Charles Korn.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package batect.logging

import batect.VersionInfo
import batect.docker.DockerClient
import batect.os.SystemInfo

class ApplicationInfoLogger(
    private val logger: Logger,
    private val versionInfo: VersionInfo,
    private val systemInfo: SystemInfo,
    private val dockerClient: DockerClient,
    private val environmentVariables: Map<String, String>
) {
    constructor(logger: Logger, versionInfo: VersionInfo, systemInfo: SystemInfo, dockerClient: DockerClient)
        : this(logger, versionInfo, systemInfo, dockerClient, System.getenv())

    fun logApplicationInfo(commandLineArgs: Iterable<String>) {
        logger.info {
            message("Application started.")
            data("commandLine", commandLineArgs)
            data("versionInfo", versionInfo)
            data("systemInfo", systemInfo)
            data("dockerVersionInfo", dockerClient.getDockerVersionInfo().toString())
            data("environment", environmentVariables)
        }
    }
}
