/*
 * Copyright 2022 Yan Kun <yan_kun_1992@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import mill._
import mill.scalalib._
import mill.scalalib.publish._

object ProjectInfo {

    def description: String     = "A super fast IO & Actor programming model!"
    def organization: String    = "cc.otavia"
    def organizationUrl: String = "https://github.com/otavia-projects"
    def projectUrl: String      = "https://github.com/otavia-projects/otavia-examples"
    def github                  = VersionControl.github("otavia-projects", "otavia-examples")
    def repository              = github.browsableRepository.get
    def licenses                = Seq(License.`Apache-2.0`)
    def author                  = Seq("Yan Kun <yan_kun_1992@foxmail.com>")
    def version                 = "0.4.0"
    def scalaVersion            = "3.3.3"
    def buildTool               = "mill"
    def buildToolVersion        = main.BuildInfo.millVersion

    def all       = ivy"cc.otavia::otavia-all:${version}"

}

trait ExampleModule extends ScalaModule {

    override def scalaVersion = ProjectInfo.scalaVersion
    override def ivyDeps      = Agg(ProjectInfo.all)

}

object basic extends ExampleModule

object `http-server` extends ExampleModule {

    override def ivyDeps = super.ivyDeps() ++ Agg(ProjectInfo.all)

}

object database extends ExampleModule {

    override def ivyDeps = super.ivyDeps() ++ Agg(ProjectInfo.all)

}

object echo extends ExampleModule {

    override def ivyDeps = super.ivyDeps() ++ Agg(ProjectInfo.all)

}

object timer extends ExampleModule

object misc extends ExampleModule {

    override def ivyDeps = super.ivyDeps() ++ Agg(ProjectInfo.all)

}

object techempower extends ScalaModule {

    override def scalaVersion = "3.3.3"

    override def ivyDeps = Agg(ProjectInfo.all, ivy"com.lihaoyi::scalatags:0.12.0")
}