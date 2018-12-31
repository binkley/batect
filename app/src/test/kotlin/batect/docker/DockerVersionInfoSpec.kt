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

package batect.docker

import batect.testutils.equalTo
import batect.utils.Version
import com.natpryce.hamkrest.assertion.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object DockerVersionInfoSpec : Spek({
    given("a set of Docker version information") {
        val info = DockerVersionInfo(Version(17, 9, 1, "ce"), "serverApi", "serverMinApi", "serverCommit")

        on("converting it to a string") {
            val result = info.toString()

            it("returns a human-readable representation of itself") {
                assertThat(result, equalTo("17.9.1-ce (API version: serverApi, minimum supported API version: serverMinApi, commit: serverCommit)"))
            }
        }
    }
})
