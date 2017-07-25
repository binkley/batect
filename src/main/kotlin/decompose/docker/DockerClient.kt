package decompose.docker

import decompose.config.Container
import java.util.UUID

class DockerClient(
        private val imageLabellingStrategy: DockerImageLabellingStrategy,
        private val processRunner: ProcessRunner,
        private val creationCommandGenerator: DockerContainerCreationCommandGenerator) {

    fun build(projectName: String, container: Container): DockerImage {
        val label = imageLabellingStrategy.labelImage(projectName, container)
        val command = listOf("docker", "build", "--tag", label, container.buildDirectory)

        if (failed(processRunner.run(command))) {
            throw ImageBuildFailedException()
        }

        return DockerImage(label)
    }

    fun create(container: Container, command: String?, image: DockerImage, network: DockerNetwork): DockerContainer {
        val args = creationCommandGenerator.createCommandLine(container, command, image, network)
        val result = processRunner.runAndCaptureOutput(args)

        if (failed(result.exitCode)) {
            throw ContainerCreationFailedException("Creation of container '${container.name}' failed. Output from Docker was: ${result.output.trim()}")
        } else {
            return DockerContainer(result.output.trim(), container.name)
        }
    }

    fun run(container: DockerContainer): DockerContainerRunResult {
        val command = listOf("docker", "start", "--attach", "--interactive", container.id)
        val exitCode = processRunner.run(command)

        return DockerContainerRunResult(exitCode)
    }

    fun start(container: DockerContainer) {
        val command = listOf("docker", "start", container.id)
        val result = processRunner.runAndCaptureOutput(command)

        if (failed(result.exitCode)) {
            throw ContainerStartFailedException(container.name, result.output)
        }
    }

    fun waitForHealthStatus(container: DockerContainer): HealthStatus {
        if (!hasHealthCheck(container)) {
            return HealthStatus.NoHealthCheck
        }

        val command = listOf("docker", "events", "--since=0",
                "--format", "{{.Status}}",
                "--filter", "container=${container.id}",
                "--filter", "event=die",
                "--filter", "event=health_status")

        val result = processRunner.runAndProcessOutput(command) { line ->
            when {
                line == "health_status: healthy" -> KillProcess(HealthStatus.BecameHealthy)
                line == "health_status: unhealthy" -> KillProcess(HealthStatus.BecameUnhealthy)
                line.startsWith("health_status") -> throw IllegalArgumentException("Unexpected health_status event: $line")
                line == "die" -> KillProcess(HealthStatus.Exited)
                else -> throw IllegalArgumentException("Unexpected event received: $line")
            }
        }

        return when (result) {
            is KilledDuringProcessing -> result.result
            is Exited -> throw ContainerHealthCheckException("Event stream for container '${container.name}' exited early with exit code ${result.exitCode}.")
        }
    }

    private fun hasHealthCheck(container: DockerContainer): Boolean {
        val command = listOf("docker", "inspect", "--format", "{{json .Config.Healthcheck}}", container.id)
        val result = processRunner.runAndCaptureOutput(command)

        if (failed(result.exitCode)) {
            throw ContainerHealthCheckException("Checking if container '${container.name}' has a healthcheck failed. Output from Docker was: ${result.output}")
        }

        return result.output.trim() != "null"
    }

    fun stop(container: DockerContainer) {
        val command = listOf("docker", "stop", container.id)
        val result = processRunner.runAndCaptureOutput(command)

        if (failed(result.exitCode)) {
            if (result.output.startsWith("Error response from daemon: No such container: ")) {
                throw ContainerDoesNotExistException("Stopping container '${container.name}' failed because it does not exist. If it was started with '--rm', it may have already stopped and removed itself.")
            } else {
                throw ContainerStopFailedException(container.name, result.output.trim())
            }
        }
    }

    fun createNewBridgeNetwork(): DockerNetwork {
        val command = listOf("docker", "network", "create", "--driver", "bridge", UUID.randomUUID().toString())
        val result = processRunner.runAndCaptureOutput(command)

        if (failed(result.exitCode)) {
            throw NetworkCreationFailedException(result.output.trim())
        }

        return DockerNetwork(result.output.trim())
    }

    fun deleteNetwork(network: DockerNetwork) {
        val command = listOf("docker", "network", "rm", network.id)
        val result = processRunner.runAndCaptureOutput(command)

        if (failed(result.exitCode)) {
            throw NetworkDeletionFailedException(network.id, result.output.trim())
        }
    }

    private fun failed(exitCode: Int): Boolean = exitCode != 0
}

data class DockerImage(val id: String)
data class DockerContainer(val id: String, val name: String)
data class DockerContainerRunResult(val exitCode: Int)
data class DockerNetwork(val id: String)

class ImageBuildFailedException : RuntimeException("Image build failed.")
class ContainerCreationFailedException(message: String) : RuntimeException(message)
class ContainerStartFailedException(val containerName: String, val outputFromDocker: String) : RuntimeException("Starting container '$containerName' failed. Output from Docker was: $outputFromDocker")
class ContainerStopFailedException(val containerName: String, val outputFromDocker: String) : RuntimeException("Stopping container '$containerName' failed. Output from Docker was: $outputFromDocker")
class ContainerDoesNotExistException(message: String) : RuntimeException(message)
class ContainerHealthCheckException(message: String) : RuntimeException(message)
class NetworkCreationFailedException(val outputFromDocker: String) : RuntimeException("Creation of network failed. Output from Docker was: $outputFromDocker")
class NetworkDeletionFailedException(val networkId: String, val outputFromDocker: String) : RuntimeException("Deletion of network '$networkId' failed. Output from Docker was: $outputFromDocker")

enum class HealthStatus {
    NoHealthCheck,
    BecameHealthy,
    BecameUnhealthy,
    Exited
}
