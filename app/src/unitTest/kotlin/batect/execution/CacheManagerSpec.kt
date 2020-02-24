/*
   Copyright 2017-2020 Charles Korn.

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

package batect.execution

import batect.config.ProjectPaths
import batect.testutils.createForEachTest
import batect.testutils.equalTo
import batect.testutils.given
import batect.testutils.runForEachTest
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.matches
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.serialization.toUtf8Bytes
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Files

object CacheManagerSpec : Spek({
    describe("a cache manager") {
        val fileSystem by createForEachTest { Jimfs.newFileSystem(Configuration.unix()) }
        val cachesDirectoryPath by createForEachTest { fileSystem.getPath("/.batect/caches") }
        val cacheKeyPath by createForEachTest { cachesDirectoryPath.resolve("key") }
        val projectPaths by createForEachTest {
            mock<ProjectPaths> {
                on { cacheDirectory } doReturn cachesDirectoryPath
            }
        }

        val cacheManager by createForEachTest { CacheManager(projectPaths) }

        describe("getting the project cache key") {
            given("the caches directory does not exist") {
                val cacheKey by runForEachTest { cacheManager.projectCacheKey }

                it("generates a cache key in the expected format") {
                    assertThat(cacheKey, matches("""^[a-z0-9]{6}$""".toRegex()))
                }

                it("writes the cache key to the cache key file") {
                    assertThat(Files.readAllBytes(cacheKeyPath).toString(Charsets.UTF_8), equalTo("$cacheKey\n"))
                }
            }

            given("the caches directory exists") {
                beforeEachTest {
                    Files.createDirectories(cachesDirectoryPath)
                }

                given("the cache key file already exists") {
                    beforeEachTest {
                        Files.write(cacheKeyPath, "the-cache-key-on-disk\n".toUtf8Bytes())
                    }

                    it("returns the cache key from the cache key file") {
                        assertThat(cacheManager.projectCacheKey, equalTo("the-cache-key-on-disk"))
                    }
                }

                given("the cache key file does not already exist") {
                    val cacheKey by runForEachTest { cacheManager.projectCacheKey }

                    it("generates a cache key in the expected format") {
                        assertThat(cacheKey, matches("""^[a-z0-9]{6}$""".toRegex()))
                    }

                    it("writes the cache key to the cache key file") {
                        assertThat(Files.readAllBytes(cacheKeyPath).toString(Charsets.UTF_8), equalTo("$cacheKey\n"))
                    }
                }
            }
        }
    }
})