
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
def buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('Could not find repository with id = unknown')
// Maven 4 can produce an additional build POM; every comparison must include at least the POM and JAR.
def matchedFiles = (buildLog =~ /rebuild comparison result: (\d+) files match/).collect { it[1].toInteger() }
assert matchedFiles.size() == 4
assert matchedFiles.every { it >= 2 }
